import sqlite3

DB_NAME = "hkunite.db"

# ------------------------------
# Connects to database
# The .db file is created automatically if it does not exist
# ------------------------------
def get_connection():
    con = sqlite3.connect(DB_NAME)
    con.row_factory = sqlite3.Row  # dictionary-like access to rows
    return con

# ------------------------------
# Initializes database
# ------------------------------
def init_db():
    con = get_connection()
    cur = con.cursor()

    # User table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS USER (
            uid INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            name TEXT,
            image TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    """)

    # Event table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT (
            eid INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT,
            cid INTEGER,
            image TEXT,
            date TIMESTAMP NOT NULL,
            oid INTEGER NOT NULL,
            public BOOLEAN NOT NULL,
            FOREIGN KEY (oid) REFERENCES USER(uid),
            FOREIGN KEY (cid) REFERENCES CATEGORY(cid)
        );
    """)

    # Event Category table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS CATEGORY (
            cid INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL
        )
    """)

    # Event Participant table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT_PARTICIPANT (
            eid INTEGER NOT NULL,
            uid INTEGER NOT NULL,
            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (eid, uid),
            FOREIGN KEY (eid) REFERENCES EVENT(eid),
            FOREIGN KEY (uid) REFERENCES USER(uid)
        );
    """)

    # Badge table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS BADGE (
            bid INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            image TEXT
        );
    """)

    # Badge owner table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT_PARTICIPANT (
            bid INTEGER NOT NULL,
            uid INTEGER NOT NULL,
            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (bid, uid),
            FOREIGN KEY (bid) REFERENCES BADGE(bid),
            FOREIGN KEY (uid) REFERENCES USER(uid)
        );
    """)

    # Add some initial values
    categories = ["Academic", "Social", "International", "Sports", "Arts"]
    for cat in categories:
        cur.execute("INSERT OR IGNORE INTO CATEGORY (name) VALUES (?)", (cat,))
    
    default_events = [
        ("Kotlin Meetup", "Learn Kotlin basics", 1, None, "2025-11-10 18:00:00", 1, True),
        ("Python Workshop", "Hands-on Python", 2, None, "2025-11-15 14:00:00", 2, True),
        ("Music Concert", "Live local bands", 3, None, "2025-12-01 20:00:00", 3, False),
    ]
    for title, desc, cid, image, date, oid, public in default_events:
        cur.execute("""
            INSERT OR IGNORE INTO EVENT (title, description, cid, image, date, oid, public)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (title, desc, cid, image, date, oid, int(public)))
    
    users = {
        "u3649750@connect.hku.hk": "12345",
        "test@connect.hku.hk": "00000",
        "123@connect.hku.hk": "password",
    }
    for email, password in users.items(): # password currently not hashed
        cur.execute(
            "INSERT OR IGNORE INTO USER (email, password) VALUES (?, ?)",
            (email, password)
        )
    
    participation = [
        (1, 2), (1, 3)
    ]
    for uid, eid in participation:
        cur.execute(
            "INSERT OR IGNORE INTO EVENT_PARTICIPANT (uid, eid) VALUES (?, ?)",
            (uid, eid)
        )

    con.commit()
    con.close()

# ------------------------------
# User functions
# ------------------------------

# Creates user on sign up
def add_user(email, password, name = None, image = None):
    with get_connection() as con:
        cur = con.cursor()
        cur.execute("""
            INSERT INTO USER (email, password, name, image)
            VALUES (?, ?, ?, ?)
        """, (email, password, name, image))
        con.commit()
        return cur.lastrowid

# Verifies user on login
def verify_user(email, password):
    with get_connection() as con:
        cur = con.cursor()
        user = cur.execute(
            "SELECT * FROM USER WHERE email = ? AND password = ?",
            (email, password)
        ).fetchone()
        return dict(user) if user else None

# Gets user information for profile page
def get_user_info(uid):
    with get_connection() as con:
        cur = con.cursor()
        user = cur.execute(
            "SELECT uid, email, name, image FROM USER WHERE uid = ?",
            (user_id,)
        ).fetchone()
        return dict(user) if user else None

# ------------------------------
# Event functions
# ------------------------------

# Gets all events
def get_all_events():
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        rows = cur.execute("SELECT * FROM EVENT WHERE public = 1").fetchall()
        return [dict(r) for r in rows]

# Gets events by category
def get_events_by_category(cid):
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        rows = cur.execute(
            "SELECT * FROM EVENT WHERE cid = ? AND public = 1",
            (cid,)
        ).fetchall()
        return [dict(r) for r in rows]

# Gets events associated with user (organizer or participant)
def get_events_for_user(uid):
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        rows = cur.execute("""
            SELECT *
            FROM EVENT E LEFT JOIN EVENT_PARTICIPANT EP ON E.eid = EP.eid
            WHERE EP.uid = ? OR E.oid = ?
        """, (uid, uid)).fetchall()
        return [dict(r) for r in rows]

# Gets events organized by user
def get_events_organized_by_user(uid):
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        rows = cur.execute(
            "SELECT * FROM EVENT WHERE oid = ?",
            (uid,)
        ).fetchall()
        return [dict(r) for r in rows]

# Gets events where user is participant
def get_events_participated_by_user(uid):
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        rows = cur.execute("""
            SELECT E.*
            FROM EVENT E JOIN EVENT_PARTICIPANT EP ON E.eid = EP.eid
            WHERE EP.uid = ?
        """, (uid,)).fetchall()
        return [dict(r) for r in rows]

# Gets event
def get_event_by_id(eid):
    with get_connection() as con:
        con.row_factory = sqlite3.Row
        cur = con.cursor()
        row = cur.execute("SELECT * FROM EVENT WHERE eid = ?", (eid,)).fetchone()
        return dict(row) if row else None

# Adds event
def add_event(title, description, oid, cid, date, public=True, participants=[]):
    with get_connection() as con:
        cur = con.cursor()
        cur.execute("""
            INSERT INTO EVENT (title, description, oid, cid, public, date)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (title, description, oid, cid, int(public), date))
        eid = cur.lastrowid

        # If event is private, need to add participants to event (or if participants are specified)
        if not public or participants:
            # Get participant uids from emails
            formatted_participants = ",".join("?" for _ in participants)
            query = f"SELECT uid FROM USER WHERE email IN ({formatted_participants})"
            cur.execute(query, participants)
            uids = [row[0] for row in cur.fetchall()]

            # Add participants by uid
            cur.executemany(
                "INSERT OR IGNORE INTO EVENT_PARTICIPANT (eid, uid) VALUES (?, ?)",
                [(eid, uid) for uid in uids]
            )

        con.commit()
        return eid
    
# Adds event participant(s)
def add_event_participants(eid, uid):   
    with get_connection() as con:
        cur.execute(
            "INSERT OR IGNORE INTO EVENT_PARTICIPANT (eid, uid) VALUES (?, ?)",
            (eid, uid)
        )
        return (eid, uid)
