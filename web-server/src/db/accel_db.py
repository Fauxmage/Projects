import sqlite3 as db
import json
import os
import numpy as np
from datetime import datetime

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "test.db")


def log_accel_data(data):
    conn = None
    try:
        tstamp = data.get("tstamp")
        battery_level = data.get("battery_level")
        samples = data.get("samples", [])

        conn = db.connect(DB_PATH)
        cur = conn.cursor()

        cur.execute(
            """
            INSERT INTO ACCEL_BATCH
            (TIMESTAMP, BATTERY_LEVEL)
            VALUES (?, ?)
            """,
            (
                tstamp,
                battery_level,
            ),
        )


        batch_id = cur.lastrowid

        samples_to_insert = []
        for sample_data in samples:
            samples_to_insert.append(
                (
                    batch_id,
                    sample_data.get("x", 0),
                    sample_data.get("y", 0),
                    sample_data.get("z", 0),
                )
            )

        if samples_to_insert:
            cur.executemany(
                "INSERT INTO ACCEL_SAMPLE (BATCH_ID, X, Y, Z) VALUES (?, ?, ?, ?)",
                samples_to_insert,
            )

        conn.commit()
        return {"status": "inserted", "batch_id": batch_id}

    except Exception as e:
        print(f"Error logging accel data: {e}")
        if conn:
            conn.rollback()
        return {"status": "failed", "batch_id": None}
    finally:
        if conn:
            conn.close()


def get_recent_batches_with_samples(limit=10):
    conn = None
    batches_list = []
    try:
        conn = db.connect(DB_PATH)
        conn.row_factory = db.Row 
        cur = conn.cursor()

        cur.execute("""
            SELECT ID, TIMESTAMP, BATTERY_LEVEL
            FROM ACCEL_BATCH
            ORDER BY TIMESTAMP DESC
            LIMIT ?
        """, (limit,))
        
        batches = cur.fetchall() 
        for batch_row in batches:
            samples_cur = conn.cursor()
            samples_cur.execute("""
                SELECT X, Y, Z
                FROM ACCEL_SAMPLE
                WHERE BATCH_ID = ?
                ORDER BY ID ASC
            """, (batch_row['ID'],))
            
            samples = samples_cur.fetchall()

            samples_list = [
                {'x': s['X'], 'y': s['Y'], 'z': s['Z']}
                for s in samples
            ]

            batches_list.append({
                "id": batch_row["ID"],
                "timestamp": batch_row["TIMESTAMP"],
                "battery_level": batch_row["BATTERY_LEVEL"],
                'samples': samples_list
            })
            
    except Exception as e:
        print(f"Error getting recent batches: {e}")
    finally:
        if conn:
            conn.close()
    
    return batches_list


def get_total_batch_count():
    conn = None
    try:
        conn = db.connect(DB_PATH)
        cur = conn.cursor()

        cur.execute("SELECT COUNT(*) as total FROM ACCEL_BATCH")
        result = cur.fetchone()

        return result[0] if result else 0

    except Exception as e:
        print(f"Error getting total batch count: {e}")
        return 0
    finally:
        if conn:
            conn.close()


def get_runtime_bounds():
    conn = None
    try:
        conn = db.connect(DB_PATH)
        conn.row_factory = db.Row
        cur = conn.cursor()

        cur.execute(
            """
            SELECT
                MIN(TIMESTAMP) AS first_timestamp,
                MAX(TIMESTAMP) AS latest_timestamp
            FROM ACCEL_BATCH
            """
        )
        row = cur.fetchone()

        if not row or row["first_timestamp"] is None or row["latest_timestamp"] is None:
            return {
                "first_timestamp": None,
                "latest_timestamp": None,
                "total_runtime_ms": 0,
            }

        first_ts = int(float(row["first_timestamp"]))
        latest_ts = int(float(row["latest_timestamp"]))

        return {
            "first_timestamp": first_ts,
            "latest_timestamp": latest_ts,
            "total_runtime_ms": max(0, latest_ts - first_ts),
        }

    except Exception as e:
        print(f"Error getting runtime bounds: {e}")
        return {
            "first_timestamp": None,
            "latest_timestamp": None,
            "total_runtime_ms": 0,
        }
    finally:
        if conn:
            conn.close()


def analyze_sampling_jitter():
    conn = None
    try:
        conn = db.connect(DB_PATH)
        cur = conn.cursor()
        cur.execute("SELECT TIMESTAMP FROM ACCEL_BATCH ORDER BY TIMESTAMP ASC")
        rows = cur.fetchall()

        if len(rows) < 2:
            print("Not enough data to calculate intervals.")
            return []

        timestamps_ms = np.array([float(r[0]) for r in rows])
        intervals_s = np.diff(timestamps_ms) / 1000.0

        expected_s = 0.250
        lower_bound_s = expected_s * 0.5
        upper_bound_s = expected_s * 2.0

        within_session = intervals_s[
            (intervals_s >= lower_bound_s) & (intervals_s <= upper_bound_s)
        ]
        n_outliers = len(intervals_s) - len(within_session)

        if len(within_session) == 0:
            print("No valid intervals after filtering.")
            return []

        return [
            {
                "mean":        round(float(np.mean(within_session)), 4),
                "std_dev":     round(float(np.std(within_session)), 4)
                if len(within_session) > 1
                else 0.0,
                "max":         round(float(np.max(within_session)), 4),
                "min":         round(float(np.min(within_session)), 4),
                "n_batches":   len(rows),
                "n_intervals": len(within_session),
                "n_outliers":   n_outliers,
                "expected_s":   expected_s,
            }
        ]

    except Exception as e:
        print(f"Error: {e}")
        return []

    finally:
        if conn:
            conn.close()


def get_hourly_battery_data():
    conn = None
    data_list = []
    try:
        conn = db.connect(DB_PATH)
        conn.row_factory = db.Row
        cur = conn.cursor()
        cur.execute("""
            SELECT
                strftime('%Y-%m-%d %H:00', TIMESTAMP / 1000, 'unixepoch') as hour,
                AVG(BATTERY_LEVEL) as avg_battery
            FROM ACCEL_BATCH
            GROUP BY hour
            ORDER BY hour ASC
        """)
        rows = cur.fetchall()

        for row in rows:
            data_list.append({"hour": row["hour"], "avg_battery": row["avg_battery"]})

    except Exception as e:
        print(f"Error getting hourly battery data: {e}")
    finally:
        if conn:
            conn.close()

    return data_list
