import sqlite3 as db
import os

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "test.db")

conn = None
try:
    conn = db.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON;")

    conn.execute("""CREATE TABLE IF NOT EXISTS ACCEL_BATCH
                 (ID                 INTEGER   PRIMARY KEY AUTOINCREMENT,
                  TIMESTAMP          INTEGER   NOT NULL,
                  BATTERY_LEVEL      INTEGER   NOT NULL
                 );""")

    conn.execute("""CREATE TABLE IF NOT EXISTS ACCEL_SAMPLE
                 (ID        INTEGER   PRIMARY KEY AUTOINCREMENT,
                  BATCH_ID  INTEGER   NOT NULL,
                  X         INTEGER   NOT NULL,
                  Y         INTEGER   NOT NULL,
                  Z         INTEGER   NOT NULL,
                  FOREIGN KEY(BATCH_ID) REFERENCES ACCEL_BATCH(ID) ON DELETE CASCADE
                 );""")

    conn.execute("""CREATE TABLE IF NOT EXISTS TOKENS
                 (ID             INTEGER   PRIMARY KEY AUTOINCREMENT,
                  TOKEN          TEXT      NOT NULL UNIQUE,
                  PARTICIPANT_ID TEXT      NOT NULL,
                  PROJECT_ID     TEXT      NOT NULL
                 );""")
    

    conn.execute("CREATE INDEX IF NOT EXISTS idx_token ON TOKENS(TOKEN);")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_batch_id ON ACCEL_SAMPLE(BATCH_ID);")
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_timestamp ON ACCEL_BATCH(TIMESTAMP);"
    )
    conn.commit()

except Exception as e:
    print(f"Error creating tables: {e}")
    if conn:
        conn.rollback()
finally:
    if conn:
        conn.close()
