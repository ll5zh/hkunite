package com.example.comp3330_hkunite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

public class EditEventActivity extends AppCompatActivity {
    private ImageView eventImage;
    private TextView eventOwner, eventTitle, eventDate, eventDescription, eventLocation;
    private Button joinButton;
    private int uid = 1; // Replace with actual user ID logic
    private int ownerId = -1;
    private boolean hasJoined = false;
    private boolean eventLoaded = false;
    private boolean joinStatusChecked = false;
    private ActivityResultLauncher<Intent> updateEventLauncher;

    private static final String TAG = "EditEventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventImage = findViewById(R.id.eventImage);
        eventOwner = findViewById(R.id.eventOwner);
        eventTitle = findViewById(R.id.eventTitle);
        eventDate = findViewById(R.id.eventDate);
        eventLocation = findViewById(R.id.eventLocation);
        eventDescription = findViewById(R.id.eventDescription);
        joinButton = findViewById(R.id.buttonJoinEvent);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Register launcher to handle result from UpdateEventActivity
        updateEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Launcher callback fired. resultCode=" + result.getResultCode());
                    Intent data = result.getData();
                    if (data != null) {
                        Log.d(TAG, "UPDATED extra = " + data.getBooleanExtra("UPDATED", false));
                    } else {
                        Log.w(TAG, "No data Intent returned from UpdateEventActivity");
                    }
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            data != null && data.getBooleanExtra("UPDATED", false)) {

                        int eid = getIntent().getIntExtra("EID", -1);
                        Log.d(TAG, "Reloading event " + eid + " after update");
                        if (eid != -1) {
                            loadEventFromServer(eid);
                        }
                    }
                }
        );


        int eid = getIntent().getIntExtra("EID", -1);
        Log.d(TAG, "Received EID: " + eid);

        if (eid == -1) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEventFromServer(eid);
        checkIfJoined(uid, eid);

        // If owner, let them edit; otherwise join
        joinButton.setOnClickListener(v -> {
            if (uid == ownerId) {
                Intent intent = new Intent(this, UpdateEventActivity.class);
                intent.putExtra("EID", eid);
                Log.d(TAG, "Launching UpdateEventActivity via launcher for eid=" + eid);
                updateEventLauncher.launch(intent);
            } else {
                sendJoinEventToServer(uid, eid);
                joinButton.setEnabled(false);
                joinButton.setText("Joined");
            }
        });
    }

    private void loadEventFromServer(int eid) {
        String url = Configuration.BASE_URL + "/events/" + eid + "?ts=" + System.currentTimeMillis();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    
                    Log.d(TAG, "GET /events/" + eid + " response: " + response.toString());
                    JSONObject data = response.optJSONObject("data");
                    if (data == null) {
                        Log.e(TAG, "No 'data' object in response");
                        return;
                    }

                    String title = data.optString("title");
                    String description = data.optString("description", "");
                    String image = data.optString("image", null);
                    String date = data.optString("date");
                    String location = data.optString("location", "");
                    String ownerName = data.optString("owner_name", "Unknown");
                    ownerId = data.optInt("oid", -1);

                    eventOwner.setText("Organized by: " + ownerName);
                    eventTitle.setText(title);
                    eventDate.setText(date);
                    eventDescription.setText(description);
                    eventLocation.setText("Location: " + location);

                    eventLocation.setOnClickListener(v -> {
                        if (!location.isEmpty()) {
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("geo:0,0?q=" + Uri.encode(location)));
                            mapIntent.setPackage("com.google.android.apps.maps");
                            startActivity(mapIntent);
                        }
                    });

                    if (image != null && !image.isEmpty() && !"null".equals(image)) {
                        Glide.with(this).load(image).into(eventImage);
                    }

                    eventLoaded = true;
                    updateJoinButton();

                },
                error -> Log.e(TAG, "Volley error: " + error.toString(), error)
        );

        request.setShouldCache(false);
        Volley.newRequestQueue(getApplicationContext()).add(request);
    }



    private void checkIfJoined(int uid, int eid) {
        String url = Configuration.BASE_URL + "/has-joined?uid=" + uid + "&eid=" + eid;

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
                    }
                },
                error -> Log.e(TAG, "Volley error in checkIfJoined: " + error.toString(), error)
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
            joinButton.setText(uid == ownerId ? "Edit Event" : "Join");
        }
    }

    private void sendJoinEventToServer(int uid, int eid) {
        String url = Configuration.BASE_URL + "/join-event";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
            jsonBody.put("eid", eid);
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error in sendJoinEventToServer", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Toast.makeText(this, "Joined event successfully", Toast.LENGTH_SHORT).show();
                    hasJoined = true;
                    updateJoinButton();
                },
                error -> Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}
