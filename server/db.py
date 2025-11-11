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
        CREATE TABLE IF NOT EXISTS BADGE_OWNER (
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
        cur.execute("INSERT OR IGNORE INTO CATEGORY (name) VALUES (?)", (cat,))
    
    default_events = [
        ("Kotlin Meetup", "Learn Kotlin basics", 1, "https://hips.hearstapps.com/hmg-prod/images/dog-puppy-on-garden-royalty-free-image-1586966191.jpg", "2025-11-10 18:00:00", 1, True),
        ("Python Workshop", "Hands-on Python", 2, "https://img.freepik.com/free-photo/pug-dog-isolated-white-background_2829-11416.jpg", "2025-11-15 14:00:00", 2, True),
        ("Music Concert", "Live local bands", 3, "https://www.cdc.gov/healthy-pets/media/images/2024/04/GettyImages-598175960-cute-dog-headshot.jpg", "2025-12-01 20:00:00", 3, False),
        ("Art Jam", "Paint with friends", 4, "https://images.unsplash.com/photo-1513364776144-60967b0f800f", "2025-11-20 16:00:00", 4, True),
        ("Startup Pitch Night", "Watch student startups pitch ideas", 5, "https://images.unsplash.com/photo-1551836022-d5d88e9218df", "2025-11-22 19:00:00", 4, True),
        ("Board Game Social", "Play Codenames and Dixit", 1, "https://images.unsplash.com/photo-1607746882042-944635dfe10e", "2025-11-25 18:30:00", 3, True),
        ("Photography Walk", "Explore campus with cameras", 2, "https://www.computerhope.com/jargon/p/program.png", "2025-11-28 15:00:00", 3, True),
        ("Career Talk: UX Design", "Learn from industry designers", 3, "https://static01.nyt.com/images/2024/11/06/multimedia/03BEATA-gftv/03BEATA-gftv-articleLarge.jpg", "2025-12-03 17:00:00", 1, True),
        ("Coding Challenge", "Solve problems in teams", 3, "https://studio.code.org/shared/images/courses/logo_tall_dance-2022.png", "2025-12-05 13:00:00", 1, True),
        ("Movie Night", "Watch a surprise film", 4, "https://cdn.britannica.com/70/234870-050-D4D024BB/Orange-colored-cat-yawns-displaying-teeth.jpg", "2025-12-07 20:00:00", 2, True),
        ("Christmas Fair", "Food, crafts, and music", 2, "https://www.eatingwell.com/thmb/m5xUzIOmhWSoXZnY-oZcO9SdArQ=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/article_291139_the-top-10-healthiest-foods-for-kids_-02-4b745e57928c4786a61b47d8ba920058.jpg", "2025-12-15 12:00:00", 4, True)
    ]
    for title, desc, cid, image, date, oid, public in default_events:
        cur.execute("""
            INSERT OR IGNORE INTO EVENT (title, description, cid, image, date, oid, public)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (title, desc, cid, image, date, oid, int(public)))
    
    users = [
    ("u3649750@connect.hku.hk", "12345", "Alice"),
    ("user2@hku.hk", "abcde", "Bob"),
    ("user3@hku.hk", "xyz123", "Charlie"),
    ("user233@hku.hk", "xyz123", "Delta")
    ]

    for email, password, name in users:  # password currently not hashed
        cur.execute(
            "INSERT OR IGNORE INTO USER (EMAIL, PASSWORD, NAME) VALUES (?, ?, ?)",
            (email, password, name)
        )


    cur.execute ('''
        INSERT INTO EVENT_PARTICIPANT (EID, UID) VALUES
        (4, 1),
        (2, 1),
        (5, 2);
                 ''')
    
    cur.execute ('''
        INSERT INTO BADGE (NAME, IMAGE) VALUES
        ('Early Bird', NULL),
        ('Python Pro', NULL);
                 ''')
    
    cur.execute ('''
        INSERT INTO BADGE_OWNER (BID, UID) VALUES
        (1, 1),
        (2, 1),
        (2, 2);
                 ''')

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
        cur = con.cursor()
        cur.execute("""
            SELECT 1 FROM EVENT_PARTICIPANT WHERE EID = ? AND UID = ?
        """, (eid, uid))
        if cur.fetchone():
            return False  # already joined
        cur.execute(
            "INSERT INTO EVENT_PARTICIPANT (EID, UID) VALUES (?, ?)",
            (eid, uid)
        )
        con.commit()
        return True
