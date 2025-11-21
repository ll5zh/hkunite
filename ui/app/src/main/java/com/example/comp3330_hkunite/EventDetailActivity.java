package com.example.comp3330_hkunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

public class EventDetailActivity extends AppCompatActivity {
    private ImageView eventImage;
    private TextView eventOwner, eventTitle, eventDate, eventDescription;
    private Button joinButton, joinButtonSide, declineButton;
    private LinearLayout invitationCard;
    private LinearLayout inviteButtonRow;

    private int uid;
    private int ownerId = -1;
    private boolean hasJoined = false;
    private boolean eventLoaded = false;
    private boolean joinStatusChecked = false;
    private boolean inviteStatusChecked = false;
    private boolean invited = false;

    private static final String TAG = "EventDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventImage = findViewById(R.id.eventImage);
        eventOwner = findViewById(R.id.eventOwner);
        eventTitle = findViewById(R.id.eventTitle);
        eventDate = findViewById(R.id.eventDate);
        eventDescription = findViewById(R.id.eventDescription);
        joinButton = findViewById(R.id.buttonJoinEvent);
        joinButtonSide = findViewById(R.id.buttonJoinEventSide);
        declineButton = findViewById(R.id.buttonDeclineInvite);
        invitationCard = findViewById(R.id.invitationCard);
        inviteButtonRow = findViewById(R.id.inviteButtonRow);

        invitationCard.setVisibility(View.GONE);
        inviteButtonRow.setVisibility(View.GONE);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        uid = prefs.getInt("USER_ID", -1);

        int eid = getIntent().getIntExtra("EID", -1);
        if (eid == -1 || uid == -1) {
            Toast.makeText(this, "Invalid event or user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventFromServer(eid);
        checkIfJoined(uid, eid);
        checkIfInvited(uid, eid);

        joinButton.setOnClickListener(v -> sendJoinEventToServer(uid, eid));
        joinButtonSide.setOnClickListener(v -> sendJoinEventToServer(uid, eid));
        declineButton.setOnClickListener(v -> declineInvite(uid, eid));
    }

    private void loadEventFromServer(int eid) {
        String url = Configuration.BASE_URL + "/events/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response;
                        if (response.has("data")) data = response.getJSONObject("data");

                        String title = data.optString("title");
                        String description = data.optString("description", "");
                        String image = data.optString("image", null);
                        String date = data.optString("date");
                        ownerId = data.optInt("oid", -1);

                        String ownerName = data.optString("owner_name");
                        if(ownerName.isEmpty()) ownerName = data.optString("owner_username", "Unknown");

                        eventOwner.setText("Organized by: " + ownerName);
                        eventTitle.setText(title);
                        eventDate.setText(date);
                        eventDescription.setText(description);

                        if (image != null && !image.isEmpty() && !image.equals("null")) {
                            Glide.with(this).load(image).into(eventImage);
                        }

                        eventLoaded = true;
                        updateButtons();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void checkIfJoined(int uid, int eid) {
        String url = Configuration.BASE_URL + "/has-joined?uid=" + uid + "&eid=" + eid;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        hasJoined = response.getBoolean("joined");
                        joinStatusChecked = true;
                        updateButtons();
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> Log.e(TAG, "Volley error: " + error.toString())
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void checkIfInvited(int uid, int eid) {
        String url = Configuration.BASE_URL + "/has-invite?uid=" + uid + "&eid=" + eid;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        invited = response.getBoolean("invited");
                        inviteStatusChecked = true;
                        updateButtons();
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> Log.e(TAG, "Volley error: " + error.toString())
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void updateButtons() {
        if (!eventLoaded || !joinStatusChecked || !inviteStatusChecked) return;

        // --- 1. THE HOST CHECK ---
        if (uid == ownerId) {
            joinButton.setVisibility(View.VISIBLE);
            joinButton.setText("Edit Event");
            joinButton.setEnabled(true);

            // POINT TO THE NEW UPDATE ACTIVITY
            joinButton.setOnClickListener(v -> {
                Intent intent = new Intent(EventDetailActivity.this, UpdateEventActivity.class);
                intent.putExtra("EID", getIntent().getIntExtra("EID", -1));
                startActivity(intent);
            });

            inviteButtonRow.setVisibility(View.GONE);
            invitationCard.setVisibility(View.GONE);
            return;
        }

        // --- 2. Normal User Logic ---
        if (hasJoined) {
            joinButton.setVisibility(View.VISIBLE);
            joinButton.setEnabled(false);
            joinButton.setText("Joined");
            inviteButtonRow.setVisibility(View.GONE);
            invitationCard.setVisibility(View.GONE);
        } else {
            if (invited) {
                inviteButtonRow.setVisibility(View.VISIBLE);
                invitationCard.setVisibility(View.VISIBLE);
                joinButton.setVisibility(View.GONE);
            } else {
                inviteButtonRow.setVisibility(View.GONE);
                invitationCard.setVisibility(View.GONE);
                joinButton.setVisibility(View.VISIBLE);
                joinButton.setEnabled(true);
                joinButton.setText("Join");

                int eid = getIntent().getIntExtra("EID", -1);
                joinButton.setOnClickListener(v -> sendJoinEventToServer(uid, eid));
            }
        }
    }

    private void sendJoinEventToServer(int uid, int eid) {
        String url = Configuration.BASE_URL + "/join-event";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
            jsonBody.put("eid", eid);
        } catch (JSONException e) { return; }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Joined event successfully", Toast.LENGTH_SHORT).show();
                    hasJoined = true;
                    joinStatusChecked = true;
                    updateButtons();
                },
                error -> Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void declineInvite(int uid, int eid) {
        String url = Configuration.BASE_URL + "/decline-invite";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
            jsonBody.put("eid", eid);
        } catch (JSONException e) { return; }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Invite declined", Toast.LENGTH_SHORT).show();
                    joinButtonSide.setVisibility(View.GONE);
                    declineButton.setText("Declined");
                    declineButton.setEnabled(false);
                },
                error -> Toast.makeText(this, "Error declining invite", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }
}