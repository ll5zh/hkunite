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

# ------------------------------
# Event endpoints
# ------------------------------

# Gets all events (explore page - public)
@app.route("/events", methods=["GET"])
def get_events():
    # Filter by category if provided
    cid = request.args.get("cid")
    if cid:
        events = db.get_events_by_category(cid)
    else:
        events = db.get_all_events()
    return jsonify({"success": True, "data": events}), 200

# Gets events associated with user (participant or organizer)
@app.route("/my-events", methods=["GET"])
def get_my_events():
    # TESTING on server side
    uid = request.args.get("uid")

    organized = db.get_events_organized_by_user(uid)
    participated = db.get_events_participated_by_user(uid)
    
    # Merge results
    my_events = {event["eid"]: event for event in organized}
    for event in participated:
        my_events[event["eid"]] = event  # adds new or overwrites same event
    
    return jsonify({"success": True, "data": list(my_events.values())}), 200

    # UNCOMMENT when hooking up with mobile app (which should pass uid in json body)
    # event = request.json # Pass uid
    # return jsonify({"success": True, "data": db.get_events_for_user(event["uid"])}), 200

# Gets events organized by user

# Gets event
@app.route("/events/<int:eid>", methods=["GET"])
def get_event(eid):
    return jsonify({"success": True, "data": db.get_event_by_id(eid)}), 200

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


if __name__ == "__main__":
    # Initialize db on startup
    if not os.path.exists(db.DB_NAME):
        db.init_db()

    # adds host="0.0.0.0" to make the server publicly available
    app.run(host="0.0.0.0", port=5001)
