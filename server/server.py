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
    uid = request.args.get("uid", type=int)

    con = db.get_connection()
    cur = con.cursor()
    rows = cur.execute("""
        SELECT E.EID, E.TITLE, E.DESCRIPTION, E.IMAGE, E.DATE, E.CID,
               C.NAME AS CATEGORY_NAME,
               U.NAME AS OWNER_USERNAME
        FROM EVENT E
        LEFT JOIN CATEGORY C ON E.CID = C.CID
        JOIN USER U ON E.OID = U.UID
        WHERE E.PUBLIC = 1 AND E.OID != ?
        ORDER BY C.CID, E.DATE
    """, (uid,)).fetchall()
    con.close()
    return jsonify([dict(row) for row in rows])



# Gets events by category

# Gets events associated with user

# Gets events organized by user

# Gets event
@app.route("/event/<int:eid>", methods=["GET"])
def get_event(eid):
    con = db.get_connection()
    cur = con.cursor()
    row = cur.execute("""
        SELECT E.EID, E.TITLE, E.DESCRIPTION, E.IMAGE, E.DATE, E.OID, U.NAME AS OWNER_USERNAME
        FROM EVENT E
        JOIN USER U ON E.OID = U.UID
        WHERE E.EID = ?
    """, (eid,)).fetchone()
    con.close()

    if row:
        return jsonify(dict(row))
    else:
        return jsonify({"error": "Event not found"}), 404





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
        db.add_event_participants(eid, uid)
        return jsonify({"success": True}), 201
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500
    
# Has joined
@app.route("/has-joined", methods=["GET"])
def has_joined():
    try:
        uid = int(request.args.get("uid"))
        eid = int(request.args.get("eid"))
    except (TypeError, ValueError):
        return jsonify({"success": False, "error": "Invalid uid or eid"}), 400

    con = db.get_connection()
    cur = con.cursor()
    cur.execute("SELECT 1 FROM EVENT_PARTICIPANT WHERE UID = ? AND EID = ?", (uid, eid))
    joined = cur.fetchone() is not None
    con.close()

    return jsonify({"success": True, "joined": joined})






if __name__ == "__main__":
    # Initialize db on startup
    print("DB path:", os.path.abspath(db.DB_NAME))

    if not os.path.exists(db.DB_NAME):
        
        db.init_db()

    # adds host="0.0.0.0" to make the server publicly available
    app.run(host="0.0.0.0", port=5001)
