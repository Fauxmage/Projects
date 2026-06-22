from flask import (
    Flask,
    jsonify,
    request,
    render_template,
    url_for,
    redirect,
    send_from_directory,
)
from flask_cors import CORS
import os
import os.path
from db.accel_db import (
    log_accel_data,
    get_recent_batches_with_samples,
    get_hourly_battery_data,
    get_total_batch_count,
    get_runtime_bounds,
    analyze_sampling_jitter,
)
import sqlite3
import secrets
from datetime import datetime

RESULT_FILES = ["cm_100hz.svg", "cm_50hz.svg", "cm_32hz.svg"]

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(BASE_DIR, "Model")

app = Flask(__name__, template_folder="templates")
app.secret_key = "notsecret"
CORS(app, supports_credentials=True)

DB_PATH = os.path.join(os.path.dirname(__file__), "test.db")


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/", methods=["GET"])
def ret_index():
    recent_batches = get_recent_batches_with_samples(limit=10)
    try:
        return render_template("index.html", batches=recent_batches)

    except Exception as e:
        print(f"Error rendering index page: {e}")
        return f"<h1>Error loading data from database: {e}</h1>", 500


@app.route("/accel", methods=["GET"])
def reroute_idx():
    return redirect(url_for("ret_index"))


@app.route("/get-pdf/<filename>")
def get_pdf(filename):
    return send_from_directory(MODEL_DIR, filename)


@app.route("/results", methods=["GET"])
def results():
    pdf_files = ["cm_100hz.svg", "cm_50hz.svg", "cm_32hz.svg"]

    try:
        return render_template("results.html", pdfs=pdf_files)
    except Exception as e:
        return f"<h1>Error loading results: {e}</h1>", 500


@app.route("/tokengen", methods=["POST"])
def token_gen():
    data = request.get_json()

    if not data or "participant_id" not in data or "project_id" not in data:
        return jsonify(
            {"error": "Missing participant_id or project_id in JSON payload"}
        ), 400

    participant_id = str(data["participant_id"])
    project_id = str(data["project_id"])
    token = secrets.token_hex(32)

    conn = None
    try:
        conn = get_db_connection()
        conn.execute(
            "INSERT INTO TOKENS (TOKEN, PARTICIPANT_ID, PROJECT_ID) VALUES (?, ?, ?)",
            (
                token,
                participant_id,
                project_id,
            ),
        )
        conn.commit()
    except Exception as e:
        return jsonify({"error": f"Error: {e}", "token": f"{token}"}), 500
    finally:
        if conn:
            conn.close()

    return jsonify({"message": "Token generated successfully", "token": token}), 201


@app.route("/user_auth", methods=["POST"])
def user_auth():
    data = request.get_json()

    if not data or "token" not in data:
        return jsonify({"error": "Missing token in JSON payload"}), 400

    token = data["token"]

    conn = None
    try:
        conn = get_db_connection()
        token_record = conn.execute(
            "SELECT PARTICIPANT_ID, PROJECT_ID FROM TOKENS WHERE TOKEN = ?", (token,)
        ).fetchone()
    except sqlite3.Error as e:
        return jsonify({"error": f"Database error: {e}"}), 500
    finally:
        if conn:
            conn.close()

    if token_record:
        return jsonify(True), 200
    else:
        return jsonify(False), 401


@app.route("/acceldata", methods=["GET"])
def get_accel_data():
    try:
        recent_batches = get_recent_batches_with_samples(limit=5)
        total_count = get_total_batch_count()
        return jsonify({"batches": recent_batches, "total_count": total_count}), 200
    except Exception as e:
        return jsonify({"error": f"Failed to retrieve accelerometer data: {e}"}), 500


@app.route("/accel", methods=["POST"])
def accel_message():
    try:
        data = request.get_json(silent=True)
        if not data:
            return jsonify({"error": "No data received!"}), 400

        if isinstance(data.get("batches"), list):
            batches_processed = 0
            samples_processed = 0
            batches_failed = 0
            batches_duplicate = 0

            for batch_data in data["batches"]:
                try:
                    result = log_accel_data(batch_data)

                    if result["status"] == "inserted":
                        batches_processed += 1
                        samples_processed += len(batch_data.get("samples", []))
                    elif result["status"] == "duplicate":
                        batches_duplicate += 1
                    else:
                        batches_failed += 1
                except Exception as e:
                    print(f"Error processing batch: {e}")
                    batches_failed += 1

            return jsonify(
                {
                    "msg": "Bulk accelerometer data processed",
                    "batches_processed": batches_processed,
                    "samples_processed": samples_processed,
                    "batches_duplicate": batches_duplicate,
                    "batches_failed": batches_failed,
                }
            ), 200

        result = log_accel_data(data)
        if result["status"] == "inserted":
            return jsonify(
                {
                    "msg": "Accelerometer data received and stored",
                }
            ), 200
        if result["status"] == "duplicate":
            return jsonify({"msg": "Duplicate batch skipped"}), 200

        return jsonify({"error": "Failed to store accelerometer data"}), 500

    except Exception as e:
        print(f"Error processing accelerometer data: {e}")
        return jsonify({"error": f"Failed to process accelerometer data: {e}"}), 500


@app.route("/datatags", methods=["GET"])
def data_tags():
    try:
        return render_template("datatags.html")

    except Exception as e:
        print(f"Error rendering datatags page: {e}")
        return f"<h1>Error loading data from database: {e}</h1>", 500


@app.route("/bstats", methods=["GET"])
def sampling_stats():
    try:
        stat_list = analyze_sampling_jitter()
        return render_template("sampstats.html", stats=stat_list)

    except Exception as e:
        print(f"Error rendering datatags page: {e}")
        return f"<h1>Error loading data from database: {e}</h1>", 500


@app.route("/battery-graph", methods=["GET"])
def battery_graph():
    hourly_data = get_hourly_battery_data()

    labels = [row["hour"] for row in hourly_data]
    data_points = [row["avg_battery"] for row in hourly_data]

    return render_template("battery_graph.html", labels=labels, data=data_points)


@app.route("/api/batches", methods=["GET"])
def api_batches():
    try:
        limit = request.args.get("limit", default=10, type=int)
        batches = get_recent_batches_with_samples(limit=limit)
        total_count = get_total_batch_count()
        return jsonify({"batches": batches, "total_count": total_count}), 200
    except Exception as e:
        return jsonify({"error": f"Failed to retrieve batches: {e}"}), 500


@app.route("/api/battery", methods=["GET"])
def api_battery():
    try:
        hourly_data = get_hourly_battery_data()
        labels = [row["hour"] for row in hourly_data]
        data_points = [row["avg_battery"] for row in hourly_data]
        return jsonify({"labels": labels, "data": data_points}), 200
    except Exception as e:
        return jsonify({"error": f"Failed to retrieve battery data: {e}"}), 500


@app.route("/api/stats", methods=["GET"])
def api_stats():
    try:
        return jsonify({"stats": analyze_sampling_jitter()}), 200
    except Exception as e:
        return jsonify({"error": f"Failed to compute stats: {e}"}), 500


@app.route("/api/results", methods=["GET"])
def api_results():
    return jsonify({"files": RESULT_FILES}), 200


@app.route("/api/runtime", methods=["GET"])
def api_runtime():
    try:
        return jsonify(get_runtime_bounds()), 200
    except Exception as e:
        return jsonify({"error": f"Failed to retrieve runtime: {e}"}), 500


@app.route("/api/tags", methods=["GET"])
def api_tags():
    return jsonify({"batches": []}), 200


if __name__ == "__main__":
    import db.create_db

    app.run()
