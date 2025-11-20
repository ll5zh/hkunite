from flask import Flask, request, jsonify
import db
import os

app = Flask(__name__)
# app.config["DEBUG"] = True # Enable debug mode to enable hot-reloader.

# ------------------------------
# User endpoints
# ------------------------------

# Handles user login
@app.route("/login", methods=["POST"])
def user_login():
    data = request.json
    user = db.verify_user(data.get("username"), data.get("password"))
    if user:
        import secrets
        token = secrets.token_hex(16)
        # Normally store token in DB or memory
        return jsonify({"success": True, "user": user, "token": token})
    return jsonify({"success": False, "message": "Invalid credentials"}), 401

# Adds user account
@app.route("/signup", methods=["POST"])
def user_signup():
    data = request.json
    try:
        uid = db.add_user(
            username=data["email"],
            password=data["password"],
            name=data.get("name"),
            image=data.get("image")
        )
        return jsonify({"success": True, "uid": uid})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

# Adds profile picture
@app.route("/add-image", methods=["POST"])
def user_add_image():
    data = request.json
    try:
        db.add_image(image=data.get("image"))
        return jsonify({"success": True, "uid": uid, "image": data.get("image")})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

# Gets user information (profile page)
@app.route("/users/<int:uid>")
def get_user_info(uid):
    return jsonify({"success": True, "data": db.get_user_info(uid)}), 200

# Gets user badges
@app.route("/badges/<int:uid>")
def get_badges(uid):
    badges = db.get_my_badges(uid) #calls smart badge function
    return jsonify({"success": True, "data": badges}), 200

# Gets all users
@app.route("/users", methods=["GET"])
def get_all_users():
    try:
        eid = request.args.get("eid", type=int)
        if eid:
            users = db.get_users_not_invited_or_participating(eid)
        else:
            users = db.get_all_users()
        return jsonify({"success": True, "users": users})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400



# ------------------------------
# Event endpoints
# ------------------------------

# Gets all events (explore page - public)
@app.route("/events", methods=["GET"])
def get_events():
    con = db.get_connection()
    cur = con.cursor()
    # join EVENT with CATEGORY and USER to get category name and organizer name
    rows = cur.execute("""
        SELECT E.*, 
               C.name AS category_name,
               U.name AS owner_name
        FROM EVENT E
        LEFT JOIN CATEGORY C ON E.cid = C.cid
        LEFT JOIN USER U ON E.oid = U.uid
        WHERE E.public = 1
    """).fetchall()
    con.close()
    return jsonify([dict(r) for r in rows])


# Check if user has already joined the event
@app.route("/has-joined", methods=["GET"])
def has_joined():
    uid = request.args.get("uid", type=int)
    eid = request.args.get("eid", type=int)
    row = db.get_connection().execute(
        "SELECT 1 FROM EVENT_PARTICIPANT WHERE uid = ? AND eid = ?",
        (uid, eid)
    ).fetchone()
    return jsonify({"success": True, "joined": row is not None})



# Gets events associated with user (participant or organizer)
@app.route("/my-events", methods=["GET"])
def get_my_events():
    # TESTING on server side - GET /my-events?uid=<uid> (if we prefer to pass uid to url)
    uid = request.args.get("uid")

    organized = db.get_events_organized_by_user(uid)
    participated = db.get_events_participated_by_user(uid)
    
    # Merge results
    my_events = {event["eid"]: event for event in organized}
    for event in participated:
        my_events[event["eid"]] = event  # adds new or overwrites same event
    
    return jsonify({"success": True, "data": list(my_events.values())}), 200

    # UNCOMMENT when hooking up with mobile app (if we prefer to pass uid in json body)
    # event = request.json # Pass uid
    # return jsonify({"success": True, "data": db.get_events_for_user(event["uid"])}), 200

# Gets events by category

# Gets events organized by user
@app.route("/my-organized-events", methods=["GET"])  # GET /my-organized-events?uid=<uid>
def get_my_organized_events():
    uid = request.args.get("uid")
    organized = db.get_events_organized_by_user(uid)
    return jsonify({"success": True, "data": organized}), 200

# Gets events where user is participant
@app.route("/my-participated-events", methods=["GET"]) # GET /my-participated-events?uid=<uid>
def get_my_participated_events():
    uid = request.args.get("uid")
    participated = db.get_events_participated_by_user(uid)
    return jsonify({"success": True, "data": list(participated.values())}), 200

# Gets event
@app.route("/events/<int:eid>", methods=["GET"])
def get_event(eid):
    con = db.get_connection()
    cur = con.cursor()
    row = cur.execute("""
        SELECT E.*, U.name AS owner_name, U.email AS owner_email
        FROM EVENT E
        LEFT JOIN USER U ON E.oid = U.uid
        WHERE E.eid = ?
    """, (eid,)).fetchone()
    con.close()
    if row:
        return jsonify({"success": True, "data": dict(row)}), 200
    return jsonify({"success": False, "error": "Event not found"}), 404



# Gets event participants
@app.route("/event-participants/<int:eid>", methods=["GET"])
def get_event_participants(eid):
    participants = db.get_event_participants(eid)
    return jsonify({"success": True, "data": list(participants.values())}), 200

# Adds event
@app.route("/add-event", methods=["POST"])
def add_event():
    event = request.json
    try:
        eid = db.add_event(
            title=event["title"],
            description=event.get("description"),
            oid=event["oid"],
            cid=event.get("cid"),
            public=event.get("public", True),
            date=event["date"],
            participants=event.get("participants")
        )
        return jsonify({"success": True, "eid": eid})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

# Edits event
@app.route("/edit-event", methods=["POST"])
def edit_event():
    event = request.json
    eid = event["eid"]
    can_update = ["title", "description", "cid", "public", "date"]
    updates = {field: val for field, val in event.items() if field in allowed_fields}

    if not updates:
        return jsonify({"success": False, "error": "No valid fields to update."}), 400
    try:
        updated_event = db.edit_event(eid, updates)
        return jsonify({"success": True, "data": updated_event})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

# Joins event
@app.route("/join-event", methods=["POST"]) # POST /join-event?eid=<eid>?uid=<uid>
def join_event():
    uid = request.args.get("uid")
    eid = request.args.get("eid")

    try:
        db.join_event(eid, uid)
        return jsonify({"success": True, "eid": eid, "uid": uid})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400


# ------------------------------
# Invitation endpoints
# ------------------------------

# Adds invite for event to user
@app.route("/add-invite", methods=["POST"]) # POST /add-invite?eid=<eid>?uid=<uid>
def add_invite():
    uid = request.args.get("uid")
    eid = request.args.get("eid")

    try:
        db.add_invite(eid, uid)
        return jsonify({"success": True, "eid": eid, "uid": uid})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400


# Gets user invites
@app.route("/my-invites/<int:uid>", methods=["GET"])
def get_my_invites(uid):
    my_invites = db.get_my_invites(uid)
    return jsonify({"success": True, "data": list(my_invites.values())})

# Gets all invites for event
@app.route("/event-invites/<int:eid>", methods=["GET"])
def get_invites_for_event(eid):
    event_invites = db.get_event_invites(eid)
    return jsonify({"success": True, "data": list(event_invites.values())}), 200

# Declines invite for event from user
@app.route("/decline-invite", methods=["POST"]) # POST /decline-invite?eid=<eid>?uid=<uid>
def decline_invite():
    uid = request.args.get("uid")
    eid = request.args.get("eid")

    try:
        db.decline_invite(eid, uid)
        return jsonify({"success": True, "eid": eid, "uid": uid})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

# Check if user has invite
@app.route("/has-invite", methods=["GET"])
def has_invite():
    uid = request.args.get("uid", type=int)
    eid = request.args.get("eid", type=int)

    if uid is None or eid is None:
        return jsonify({"success": False, "error": "Missing uid or eid"}), 400

    con = db.get_connection()
    cur = con.cursor()
    row = cur.execute(
        "SELECT 1 FROM INVITATION WHERE uid = ? AND eid = ?",
        (uid, eid)
    ).fetchone()
    con.close()

    invited = row is not None
    return jsonify({"success": True, "invited": invited}), 200


if __name__ == "__main__":
    # Initialize db on startup
    print("DB path:", os.path.abspath(db.DB_NAME))

    if not os.path.exists(db.DB_NAME):
        
        db.init_db()

    # adds host="0.0.0.0" to make the server publicly available
    app.run(host="0.0.0.0", port=5001)
