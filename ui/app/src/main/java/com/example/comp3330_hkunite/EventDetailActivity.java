package com.example.comp3330_hkunite;

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
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

public class EventDetailActivity extends AppCompatActivity {
    private ImageView eventImage;
    private TextView eventOwner, eventTitle, eventDate, eventDescription;
    private Button joinButton, joinButtonSide, declineButton;
    private LinearLayout invitationCard;
    private TextView invitationNotice;
    private ImageView invitationIcon;
    private LinearLayout inviteButtonRow;

    private int uid; // loaded from SharedPreferences
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
        invitationNotice = findViewById(R.id.textInvitationNotice);
        invitationIcon = findViewById(R.id.invitationIcon);
        inviteButtonRow = findViewById(R.id.inviteButtonRow);

        invitationCard.setVisibility(View.GONE);
        inviteButtonRow.setVisibility(View.GONE);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        uid = prefs.getInt("USER_ID", -1);
        Log.d(TAG, "Loaded UID: " + uid);

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
        String url = "http://10.0.2.2:5000/events/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            String title = data.getString("title");
                            String description = data.optString("description", "");
                            String image = data.optString("image", null);
                            String date = data.getString("date");
                            ownerId = data.optInt("oid", -1);
                            String ownerName = data.optString("owner_name", "Unknown");

                            eventOwner.setText("Organized by: " + ownerName);
                            eventTitle.setText(title);
                            eventDate.setText(date);
                            eventDescription.setText(description);
                            if (image != null && !image.isEmpty()) {
                                Glide.with(this).load(image).into(eventImage);
                            }

                            eventLoaded = true;
                            updateButtons();
                        } else {
                            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        Toast.makeText(this, "Error parsing event data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error.toString(), error);
                    Toast.makeText(this, "Network/server error: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void checkIfJoined(int uid, int eid) {
        String url = "http://10.68.166.59:5001/has-joined?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        hasJoined = response.getBoolean("joined");
                        joinStatusChecked = true;
                        updateButtons();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error in checkIfJoined", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in checkIfJoined: " + error.toString(), error);
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void checkIfInvited(int uid, int eid) {
        String url = "http://10.68.166.59:5001/has-invite?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        invited = response.getBoolean("invited");
                        inviteStatusChecked = true;
                        updateButtons();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error in checkIfInvited", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in checkIfInvited: " + error.toString(), error);
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void updateButtons() {
        if (!eventLoaded || !joinStatusChecked || !inviteStatusChecked) return;

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
            }
        }
    }


    private void sendJoinEventToServer(int uid, int eid) {
        String url = "http://10.68.166.59:5001/join-event?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Toast.makeText(this, "Joined event successfully", Toast.LENGTH_SHORT).show();
                    hasJoined = true;
                    joinStatusChecked = true;
                    updateButtons();
                },
                error -> {
                    Log.e(TAG, "Volley error in sendJoinEventToServer: " + error.toString(), error);
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void declineInvite(int uid, int eid) {
        String url = " http://10.68.166.59:5001/decline-invite?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Toast.makeText(this, "Invite declined", Toast.LENGTH_SHORT).show();

                    // Hide the join button in the row
                    joinButtonSide.setVisibility(View.GONE);

                    // Stretch Decline to fill width
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    declineButton.setLayoutParams(params);

                    // Update text and disable
                    declineButton.setText("Declined");
                    declineButton.setEnabled(false);
                },
                error -> {
                    Log.e(TAG, "Volley error in declineInvite: " + error.toString(), error);
                    Toast.makeText(this, "Error declining invite", Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }


}
