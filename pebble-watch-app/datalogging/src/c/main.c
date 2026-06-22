#include <pebble.h>

#define SAMPLES_PER_BATCH   25
#define NUM_SESSIONS        2
#define RETRY_BUF_SIZE      32
#define ACCEL_TAG_BASE      0x0091u
#define INBOX_SIZE          32
#define OUTBOX_SIZE         256
#define AM_DRAIN_INTERVAL   90000

static Window    *s_window;
static TextLayer *s_time_layer;
static TextLayer *s_stat_layer;

static uint32_t s_num_batches     = 0;
static uint32_t s_dropped_batches = 0;
static char     s_stat_buf[32];

typedef struct __attribute__((packed)) {
    int16_t x, y, z;
} RawAccelSample;

typedef struct __attribute__((packed)) {
	uint8_t		   battery;
    uint64_t       b_tstamp;
    RawAccelSample samples[SAMPLES_PER_BATCH];
} AccelDataPacket;

static DataLoggingSessionRef s_sessions[NUM_SESSIONS] = { NULL, NULL };

static void ensure_session(uint8_t idx) {
    if (s_sessions[idx] != NULL) return;
    s_sessions[idx] = data_logging_create(
        ACCEL_TAG_BASE + idx, DATA_LOGGING_BYTE_ARRAY, sizeof(AccelDataPacket), true);
    if (s_sessions[idx] == NULL) {
        APP_LOG(APP_LOG_LEVEL_ERROR, "session[%d] create failed", (int)idx);
    }
}

static DataLoggingResult log_on(uint8_t idx, const AccelDataPacket *pkt) {
    ensure_session(idx);
    if (s_sessions[idx] == NULL) return DATA_LOGGING_NOT_FOUND;

    DataLoggingResult res = data_logging_log(s_sessions[idx], pkt, 1);
    if (res == DATA_LOGGING_CLOSED) {
        data_logging_finish(s_sessions[idx]);
        s_sessions[idx] = NULL;
        ensure_session(idx);
        if (s_sessions[idx] == NULL) return DATA_LOGGING_NOT_FOUND;
        res = data_logging_log(s_sessions[idx], pkt, 1);
    }
    return res;
}

static DataLoggingResult log_packet(const AccelDataPacket *pkt) {
    DataLoggingResult res = log_on(0, pkt);
    if (res == DATA_LOGGING_BUSY) {
        res = log_on(1, pkt);
    }
    return res;
}


static AccelDataPacket s_retry_buf[RETRY_BUF_SIZE];
static uint8_t         s_retry_head  = 0;
static uint8_t         s_retry_count = 0;
static uint8_t         s_retry_hwm   = 0;

static void retry_push(const AccelDataPacket *pkt) {
    if (s_retry_count < RETRY_BUF_SIZE) {
        uint8_t tail = (s_retry_head + s_retry_count) % RETRY_BUF_SIZE;
        s_retry_buf[tail] = *pkt;
        s_retry_count++;
    } else {
        s_retry_buf[s_retry_head] = *pkt;
        s_retry_head = (s_retry_head + 1) % RETRY_BUF_SIZE;
        s_dropped_batches++;
		    APP_LOG(APP_LOG_LEVEL_WARNING, "Dropped batch: %d", (int)s_dropped_batches);

    }
}

static bool      s_am_sending  = false;
static AppTimer *s_drain_timer = NULL;

static void am_drain_next(void);

static void retry_drain(void) {
    if (s_am_sending) return;
    while (s_retry_count > 0) {
        DataLoggingResult res = log_packet(&s_retry_buf[s_retry_head]);
        if (res != DATA_LOGGING_SUCCESS) break;
        s_retry_head = (s_retry_head + 1) % RETRY_BUF_SIZE;
        s_retry_count--;
    }
    if (s_retry_count == 0) s_retry_hwm = 0;
}

static void update_display(void) {
    snprintf(s_stat_buf, sizeof(s_stat_buf), "B:%lu D:%lu R:%d",
             (unsigned long)s_num_batches,
             (unsigned long)s_dropped_batches,
             (int)s_retry_count);
    text_layer_set_text(s_stat_layer, s_stat_buf);

}

static void raw_data_handler(AccelRawData *data, uint32_t num_samples, uint64_t timestamp) {
    AccelDataPacket packet;
    packet.b_tstamp = timestamp;
    packet.battery = (uint8_t)battery_state_service_peek().charge_percent;
    s_num_batches++;

    uint32_t n = num_samples < SAMPLES_PER_BATCH ? num_samples : SAMPLES_PER_BATCH;
    for (uint32_t i = 0; i < n; i++) {
        packet.samples[i].x = data[i].x;
        packet.samples[i].y = data[i].y;
        packet.samples[i].z = data[i].z;
    }

	retry_drain();

    DataLoggingResult res = log_packet(&packet);
    if (res != DATA_LOGGING_SUCCESS) {

        APP_LOG(APP_LOG_LEVEL_WARNING, "Logging did not succeed, status: %d, batch: %lu", res, s_num_batches);
		retry_push(&packet);
        if (s_retry_count > s_retry_hwm) {
            s_retry_hwm = s_retry_count;
        }
    }

    if (s_num_batches % 25 == 0) {
        update_display();
    }
}

static void outbox_sent_callback(DictionaryIterator *iter, void *context) {
    s_retry_head = (s_retry_head + 1) % RETRY_BUF_SIZE;
    s_retry_count--;
    if (s_retry_count == 0) s_retry_hwm = 0;
    s_am_sending = false;
    update_display();
    am_drain_next();
}

static void outbox_failed_callback(DictionaryIterator *iter, AppMessageResult reason, void *context) {
    s_am_sending = false;
}

static void inbox_received_callback(DictionaryIterator *iter, void *context) {
    (void)iter; (void)context;
}

static void am_drain_next(void) {
    if (s_am_sending || s_retry_count == 0) return;

    AccelDataPacket *pkt   = &s_retry_buf[s_retry_head];

    DictionaryIterator *iter;
    if (app_message_outbox_begin(&iter) != APP_MSG_OK) return;

    dict_write_uint8(iter, MESSAGE_KEY_batry_lvl, pkt->battery);
    dict_write_data(iter, MESSAGE_KEY_b_tstamp, (uint8_t *)&pkt->b_tstamp, sizeof(pkt->b_tstamp));
    dict_write_data(iter, MESSAGE_KEY_samples, (uint8_t *)pkt->samples, sizeof(pkt->samples));

    if (app_message_outbox_send() == APP_MSG_OK) {
        s_am_sending = true;
    }
}

static void drain_timer_callback(void *context) {
    s_drain_timer = app_timer_register(AM_DRAIN_INTERVAL, drain_timer_callback, NULL);

    if (s_sessions[0] != NULL) {
        data_logging_finish(s_sessions[0]);
        s_sessions[0] = NULL;
    }

    am_drain_next();
}

static void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
    static char time_buf[6];
    strftime(time_buf, sizeof(time_buf), "%H:%M", tick_time);
    text_layer_set_text(s_time_layer, time_buf);
}

static void prv_window_load(Window *window) {
    Layer *root  = window_get_root_layer(window);
    GRect bounds = layer_get_bounds(root);

    s_time_layer = text_layer_create(GRect(0, 35, bounds.size.w, 50));
    text_layer_set_text_alignment(s_time_layer, GTextAlignmentCenter);
    text_layer_set_font(s_time_layer, fonts_get_system_font(FONT_KEY_BITHAM_42_BOLD));
    layer_add_child(root, text_layer_get_layer(s_time_layer));

    s_stat_layer = text_layer_create(GRect(0, 100, bounds.size.w, 28));
    text_layer_set_text_alignment(s_stat_layer, GTextAlignmentCenter);
    text_layer_set_font(s_stat_layer, fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
    text_layer_set_text(s_stat_layer, "B:0 D:0 R:0");
    layer_add_child(root, text_layer_get_layer(s_stat_layer));

    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    static char time_buf[6];
    strftime(time_buf, sizeof(time_buf), "%H:%M", t);
    text_layer_set_text(s_time_layer, time_buf);
}

static void prv_window_unload(Window *window) {
    text_layer_destroy(s_time_layer);
    text_layer_destroy(s_stat_layer);
}

static void prv_init(void) {
    tick_timer_service_subscribe(MINUTE_UNIT, tick_handler);

    s_window = window_create();
    window_set_window_handlers(s_window, (WindowHandlers){
        .load   = prv_window_load,
        .unload = prv_window_unload,
    });
    window_stack_push(s_window, true);

    ensure_session(0);

    accel_raw_data_service_subscribe(SAMPLES_PER_BATCH, raw_data_handler);
    accel_service_set_sampling_rate(ACCEL_SAMPLING_100HZ);

    app_message_register_inbox_received(inbox_received_callback);
    app_message_register_outbox_sent(outbox_sent_callback);
    app_message_register_outbox_failed(outbox_failed_callback);
    app_message_open(INBOX_SIZE, OUTBOX_SIZE);

    s_drain_timer = app_timer_register(AM_DRAIN_INTERVAL, drain_timer_callback, NULL);
}

static void prv_deinit(void) {
    accel_data_service_unsubscribe();
    tick_timer_service_unsubscribe();

    if (s_drain_timer) {
        app_timer_cancel(s_drain_timer);
        s_drain_timer = NULL;
    }

    update_display();

    for (int attempt = 0; attempt < 5 && s_retry_count > 0; attempt++) {
        retry_drain();
        if (s_retry_count > 0) {
            psleep(200);
        }
    }

    if (s_retry_count > 0) {
        APP_LOG(APP_LOG_LEVEL_WARNING, "deinit: %d packets lost", (int)s_retry_count);
    }

    for (int i = 0; i < NUM_SESSIONS; i++) {
        if (s_sessions[i] != NULL) {
            data_logging_finish(s_sessions[i]);
            s_sessions[i] = NULL;
        }
    }

    window_destroy(s_window);
}

int main(void) {
    prv_init();
    app_event_loop();
    prv_deinit();
}
