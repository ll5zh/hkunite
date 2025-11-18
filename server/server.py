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
    # 

# Gets all users
@app.route("/users", methods=["GET"])
def get_all_users():
    try:
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
    events = con.execute("SELECT *FROM EVENT WHERE PUBLIC = ?", (1,)).fetchall()
    con.close()
    return jsonify([dict(e) for e in events])

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
    return jsonify({"success": True, "data": list(organized.values())}), 200

# Gets events where user is participant
@app.route("/my-participated-events", methods=["GET"]) # GET /my-participated-events?uid=<uid>
def get_my_participated_events():
    uid = request.args.get("uid")
    participated = db.get_events_participated_by_user(uid)
    return jsonify({"success": True, "data": list(participated.values())}), 200

# Gets event
@app.route("/events/<int:eid>", methods=["GET"])
def get_event(eid):
    return jsonify({"success": True, "data": db.get_event_by_id(eid)}), 200

# Gets event participants
@app.route("/event-participants/<int:eid>", methods=["GET"])
def get_event_participants(eid):
    participants = db.get_event_participants(eid)
    return return jsonify({"success": True, "data": list(participants.values())}), 200

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

if __name__ == "__main__":
    # Initialize db on startup
    print("DB path:", os.path.abspath(db.DB_NAME))

    if not os.path.exists(db.DB_NAME):
        
        db.init_db()

    # adds host="0.0.0.0" to make the server publicly available
    app.run(host="0.0.0.0", port=5001)
