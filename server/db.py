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
            UID INTEGER PRIMARY KEY AUTOINCREMENT,
            EMAIL TEXT NOT NULL UNIQUE,
            PASSWORD TEXT NOT NULL,
            NAME TEXT,
            IMAGE TEXT,
            CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    """)

    # Event table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT (
            EID INTEGER PRIMARY KEY AUTOINCREMENT,
            TITLE TEXT NOT NULL,
            DESCRIPTION TEXT,
            CID INTEGER,
            IMAGE TEXT,
            DATE TIMESTAMP NOT NULL,
            OID INTEGER NOT NULL,
            PUBLIC BOOLEAN NOT NULL,
            FOREIGN KEY (OID) REFERENCES USER(UID),
            FOREIGN KEY (CID) REFERENCES CATEGORY(CID)
        );
    """)

    # Event Category table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS CATEGORY (
            CID INTEGER PRIMARY KEY AUTOINCREMENT,
            NAME TEXT NOT NULL
        )
    """)

    # Event Participant table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT_PARTICIPANT (
            EID INTEGER NOT NULL,
            UID INTEGER NOT NULL,
            JOINED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (EID, UID),
            FOREIGN KEY (EID) REFERENCES EVENT(EID),
            FOREIGN KEY (UID) REFERENCES USER(UID)
        );
    """)

    # Badge table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS BADGE (
            BID INTEGER PRIMARY KEY AUTOINCREMENT,
            NAME TEXT NOT NULL UNIQUE,
            IMAGE TEXT
        );
    """)

    # Badge owner table
    cur.execute("""
        CREATE TABLE IF NOT EXISTS EVENT_PARTICIPANT (
            BID INTEGER NOT NULL,
            UID INTEGER NOT NULL,
            JOINED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (BID, UID),
            FOREIGN KEY (BID) REFERENCES BADGE(BID),
            FOREIGN KEY (UID) REFERENCES USER(UID)
        );
    """)

    # Add some initial values
    categories = ["Academic", "Social", "International", "Sports", "Arts"]
    for cat in categories:
        cur.execute("INSERT OR IGNORE INTO CATEGORY (NAME) VALUES (?)", (cat,))
    
    default_events = [
        ("Kotlin Meetup", "Learn Kotlin basics", 1, None, "2025-11-10 18:00:00", 1, True),
        ("Python Workshop", "Hands-on Python", 2, None, "2025-11-15 14:00:00", 2, True),
        ("Music Concert", "Live local bands", 3, None, "2025-12-01 20:00:00", 3, False),
    ]
    for title, desc, cid, image, date, oid, public in default_events:
        cur.execute("""
            INSERT OR IGNORE INTO EVENT (TITLE, DESCRIPTION, CID, IMAGE, DATE, OID, PUBLIC)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (title, desc, cid, image, date, oid, int(public)))
    
    users = {
        "u3649750@connect.hku.hk": "12345"
    }
    for email, password in users.items(): # password currently not hashed
        cur.execute(
            "INSERT OR IGNORE INTO USER (EMAIL, PASSWORD) VALUES (?, ?)",
            (email, password)
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
            INSERT INTO USER (EMAIL, PASSWORD, NAME, IMAGE)
            VALUES (?, ?, ?, ?)
        """, (email, password, name, image))
        con.commit()
        return cur.lastrowid

# Verifies user on login
def verify_user(email, password):
    with get_connection() as con:
        cur = con.cursor()
        user = cur.execute(
            "SELECT * FROM USER WHERE EMAIL = ? AND PASSWORD = ?",
            (email, password)
        ).fetchone()
        return dict(user) if user else None

# Gets user information for profile page
def get_user_info(uid):
    with get_connection() as con:
        cur = con.cursor()
        user = cur.execute(
            "SELECT UID, EMAIL, NAME, IMAGE FROM USER WHERE UID = ?",
            (user_id,)
        ).fetchone()
        return dict(user) if user else None

# ------------------------------
# Event functions
# ------------------------------

# Gets all events
def get_all_events():
    with get_connection() as con:
        cur = con.cursor()
        rows = cur.execute("SELECT * FROM EVENT WHERE PUBLIC = 1").fetchall()
        return [dict(r) for r in rows]

# Gets events by category
def get_events_by_category(cid):
    with get_connection() as con:
        cur = con.cursor()
        rows = cur.execute(
            "SELECT * FROM EVENT WHERE CID = ? AND PUBLIC = ?",
            (cid, 1)
        ).fetchall()
        return [dict(r) for r in rows]

# Gets events associated with user (organizer or participant)
def get_events_for_user(uid):
    with get_connection() as con:
        cur = con.cursor()
        rows = cur.execute("""
            SELECT E.*
            FROM EVENT E LEFT JOIN EVENT_PARTICIPANT EP ON E.EID = EP.EID
            WHERE EP.UID = ? OR E.OID = ?
        """, (uid, uid)).fetchall()
        return [dict(r) for r in rows]

# Gets events organized by user
def get_events_organized_by_user(uid):
    with get_connection() as con:
        cur = con.cursor()
        rows = cur.execute(
            "SELECT * FROM EVENT WHERE OID = ?",
            (uid,)
        ).fetchall()
        return [dict(r) for r in rows]

# Gets events where user is participant
def get_events_participated_by_user(uid):
    with get_connection() as con:
            cur = con.cursor()
            rows = cur.execute("""
                SELECT E.*
                FROM EVENT E LEFT JOIN EVENT_PARTICIPANT EP ON E.EID = EP.EID
                WHERE EP.UID = ?
            """, (uid,)).fetchall()
            return [dict(r) for r in rows]

# Gets event
def get_event(eid):
    with get_connection() as con:
        cur = con.cursor()
        row = cur.execute("SELECT * FROM EVENT WHERE EID = ?", (eid,)).fetchone()
        return dict(row) if row else None

# Adds event
def add_event(title, oid, cid, public=True, participants=[]):
    with get_connection() as con:
        cur = con.cursor()
        cur.execute("""
            INSERT INTO EVENT (NAME, OID, CID, PUBLIC)
            VALUES (?, ?, ?, ?)
        """, (title, oid, cid, int(public)))
        eid = cur.lastrowid

        # If event is private, need to add participants to event
        if not public:
            # Get participant uids from emails
            formatted_participants = ",".join("?" for _ in participants)
            query = f"SELECT UID FROM USER WHERE EMAIL IN ({formatted_participants})"
            cur.execute(query, participants)
            uids = [row[0] for row in cur.fetchall()]

            # Add participants by uid
            cur.executemany(
                "INSERT OR IGNORE INTO EVENT_PARTICIPANT (EID, UID) VALUES (?, ?)",
                [(eid, uid) for uid in uids]
            )

        con.commit()
        return eid
    
# Adds event participant(s)
def add_event_participants(eid, uid):   
    with get_connection() as con:
        cur.execute(
            "INSERT OR IGNORE INTO EVENT_PARTICIPANT (EID, UID) VALUES (?, ?)",
            (eid, uid)
        )
        return (eid, uid)
