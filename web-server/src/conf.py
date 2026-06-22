import sqlite3 as db


def reg_user(username, password):
    conn = db.connect("test.db")
    cur = conn.cursor()

    cur.execute(
        "INSERT INTO USERS (USERNAME,PASSWORD) VALUES (?,?)", (username, password)
    )
    conn.commit()
    conn.close()


def return_user(username):
    user_lookup = username

    conn = db.connect("test.db")
    cur = conn.cursor()

    pwd_fetch = cur.execute(
        "SELECT PASSWORD FROM USERS WHERE USERNAME=?", (user_lookup,)
    )
    pwd = pwd_fetch.fetchone()

    if pwd:
        pwd_str = pwd[0].decode("utf-8")
        print("string:", pwd_str)
        pwd_byte = pwd_str.encode("utf-8")
        print("byte:", pwd_byte)
        return pwd_byte
    else:
        return None


def authenticate_user(username):
    user_lookup = username

    conn = db.connect("test.db")
    cur = conn.cursor()

    user_fetch = cur.execute(
        "SELECT USERNAME FROM USERS WHERE USERNAME=?", (user_lookup,)
    )
    user = user_fetch.fetchone()

    if user:
        return True
    else:
        return False


