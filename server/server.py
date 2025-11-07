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
    con = db.get_connection()
    events = con.execute("SELECT *FROM EVENT WHERE PUBLIC = ?", (1,)).fetchall()
    con.close()
    return jsonify([dict(e) for e in events])

# Gets events by category

# Gets events associated with user

# Gets events organized by user

# Gets event

# Adds event

# Join event
@app.route("/join-event", methods=["POST"])
def join_event():
    data = request.json or {}
    uid = data.get("uid")
    eid = data.get("eid")
    if uid is None or eid is None:
        return jsonify({"success": False, "error": "Missing uid or eid"}), 400
    try:
        con = db.get_connection()
        # Prevent duplicate if your DB enforces PK; optionally check first
        con.execute(
            "INSERT INTO EVENT_PARTICIPANT (EID, UID) VALUES (?, ?)",
            (eid, uid)
        )
        con.commit()
        con.close()
        return jsonify({"success": True}), 201
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500



if __name__ == "__main__":
    # Initialize db on startup
    if not os.path.exists(db.DB_NAME):
        db.init_db()

    # adds host="0.0.0.0" to make the server publicly available
    app.run(host="0.0.0.0", port=5001)
