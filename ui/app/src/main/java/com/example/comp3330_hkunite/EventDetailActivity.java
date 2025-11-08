package com.example.comp3330_hkunite;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private Button joinButton;
    private int uid = 1; // Replace with actual user ID logic
    private int ownerId = -1;
    private boolean hasJoined = false;
    private boolean eventLoaded = false;
    private boolean joinStatusChecked = false;
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

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        int eid = getIntent().getIntExtra("EID", -1);
        Log.d(TAG, "Received EID: " + eid);

        if (eid == -1) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventFromServer(eid);
        checkIfJoined(uid, eid);

        joinButton.setOnClickListener(v -> {
            sendJoinEventToServer(uid, eid);
            joinButton.setEnabled(false);
            joinButton.setText("Joined");
        });
    }

    private void loadEventFromServer(int eid) {
        String url = "http://10.70.8.141:5001/event/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String title = response.getString("TITLE");
                        String description = response.getString("DESCRIPTION");
                        String image = response.optString("IMAGE", null);
                        String date = response.getString("DATE");
                        String ownerUsername = response.optString("OWNER_USERNAME", "Unknown");
                        ownerId = response.optInt("OID", -1);
                        Log.d(TAG, "Owner ID: " + ownerId + ", Current UID: " + uid);

                        eventOwner.setText("Organized by: " + ownerUsername);
                        eventTitle.setText(title);
                        eventDate.setText(date);
                        eventDescription.setText(description);
                        if (image != null && !image.isEmpty()) {
                            Glide.with(this).load(image).into(eventImage);
                        }

                        eventLoaded = true;
                        updateJoinButton();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error in loadEventFromServer", e);
                        Toast.makeText(this, "Error parsing event data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in loadEventFromServer: " + error.toString(), error);
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e(TAG, "Status code: " + statusCode);
                        Log.e(TAG, "Response body: " + responseBody);
                        Toast.makeText(this, "Server error: " + statusCode, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Network error: " + error.toString(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void checkIfJoined(int uid, int eid) {
        String url = "http://10.70.8.141:5001/has-joined?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        hasJoined = response.getBoolean("joined");
                        joinStatusChecked = true;
                        updateJoinButton();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error in checkIfJoined", e);
                        Toast.makeText(this, "Error parsing join status", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in checkIfJoined: " + error.toString(), error);
                    Toast.makeText(this, "Error checking join status: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void updateJoinButton() {
        if (!eventLoaded || !joinStatusChecked) return;

        if (hasJoined) {
            joinButton.setEnabled(false);
            joinButton.setText("Joined");
        } else {
            joinButton.setEnabled(true);
            joinButton.setText("Join");
        }
    }

    private void sendJoinEventToServer(int uid, int eid) {
        String url = "http://10.70.8.141:5001/join-event";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
            jsonBody.put("eid", eid);
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error in sendJoinEventToServer", e);
            Toast.makeText(this, "Error creating join request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                null,
                error -> {
                    Log.e(TAG, "Volley error in sendJoinEventToServer: " + error.toString(), error);
                    Toast.makeText(this, "Server error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}
