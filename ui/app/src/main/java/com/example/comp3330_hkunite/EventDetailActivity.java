package com.example.comp3330_hkunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {
    private ImageView eventImage;
    private TextView eventOwner, eventTitle, eventDate, eventDescription;
    private Button joinButton, joinButtonSide, declineButton;
    private LinearLayout invitationCard;
    private LinearLayout inviteButtonRow;
    private TextView eventLocation;

    private int uid;
    private int ownerId = -1;
    private boolean hasJoined = false;
    private boolean eventLoaded = false;
    private boolean joinStatusChecked = false;
    private boolean inviteStatusChecked = false;
    private boolean invited = false;
    private ActivityResultLauncher<Intent> updateEventLauncher;

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
        eventLocation = findViewById(R.id.eventLocation);
        joinButton = findViewById(R.id.buttonJoinEvent);
        joinButtonSide = findViewById(R.id.buttonJoinEventSide);
        declineButton = findViewById(R.id.buttonDeclineInvite);
        invitationCard = findViewById(R.id.invitationCard);
        inviteButtonRow = findViewById(R.id.inviteButtonRow);

        invitationCard.setVisibility(View.GONE);
        inviteButtonRow.setVisibility(View.GONE);

        updateEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("UPDATED", false)) {
                            int eid = getIntent().getIntExtra("EID", -1);
                            loadEventFromServer(eid); // reload immediately
                        }
                    }
                }
        );

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
                        String location = data.optString("location", ""); // NEW
                        ownerId = data.optInt("oid", -1);

                        String ownerName = data.optString("owner_name");
                        if(ownerName.isEmpty()) ownerName = data.optString("owner_username", "Unknown");

                        eventOwner.setText("Organized by: " + ownerName);
                        eventTitle.setText(title);
                        eventDate.setText(date);
                        eventDescription.setText(description);
                        eventLocation.setText("Location: " + location); // NEW

                        // Click → open Google Maps
                        eventLocation.setOnClickListener(v -> {
                            if (!location.isEmpty()) {
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                                        android.net.Uri.parse("geo:0,0?q=" + Uri.encode(location)));
                                mapIntent.setPackage("com.google.android.apps.maps");
                                startActivity(mapIntent);
                            }
                        });

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

        long now = System.currentTimeMillis();
        boolean eventNotPassed = false;
        try {
            // parse eventDate string to millis
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date eventDateObj = sdf.parse(eventDate.getText().toString());
            if (eventDateObj != null) {
                eventNotPassed = eventDateObj.getTime() > now;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Date parse error", e);
        }

        if (uid == ownerId) {
            joinButton.setVisibility(View.GONE);
            joinButtonSide.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);
            inviteButtonRow.setVisibility(View.GONE);
            slideOut(invitationCard);

            if (eventNotPassed) {
                // show Edit + Invite row
                findViewById(R.id.editInviteRow).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonEditEventSolo).setVisibility(View.GONE);

                Button editBtn = findViewById(R.id.buttonEditEventSide);
                Button inviteBtn = findViewById(R.id.buttonInviteEvent);

                editBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(this, UpdateEventActivity.class);
                    intent.putExtra("EID", getIntent().getIntExtra("EID", -1));
                    updateEventLauncher.launch(intent);
                });

                inviteBtn.setOnClickListener(v -> {
                    int eid = getIntent().getIntExtra("EID", -1);
                    if (eid != -1) {
                        Intent intent = new Intent(EventDetailActivity.this, InviteActivity.class);
                        intent.putExtra("EVENT_ID", eid);   // pass event ID to InviteActivity
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                // show solo Edit button
                findViewById(R.id.editInviteRow).setVisibility(View.GONE);
                findViewById(R.id.buttonEditEventSolo).setVisibility(View.VISIBLE);

                Button editSolo = findViewById(R.id.buttonEditEventSolo);
                editSolo.setOnClickListener(v -> {
                    Intent intent = new Intent(this, UpdateEventActivity.class);
                    intent.putExtra("EID", getIntent().getIntExtra("EID", -1));
                    updateEventLauncher.launch(intent);
                });
            }
            return;
        }

        if (hasJoined) {
            joinButton.setVisibility(View.VISIBLE);
            joinButton.setEnabled(false);
            joinButton.setText("Joined");
            inviteButtonRow.setVisibility(View.GONE);
            slideOut(invitationCard);
        } else {
            if (invited) {
                inviteButtonRow.setVisibility(View.VISIBLE);
                slideIn(invitationCard);
                joinButton.setVisibility(View.GONE);
            } else {
                inviteButtonRow.setVisibility(View.GONE);
                slideOut(invitationCard);
                joinButton.setVisibility(View.VISIBLE);
                joinButton.setEnabled(true);
                joinButton.setText("Join");

                int eid = getIntent().getIntExtra("EID", -1);
                joinButton.setOnClickListener(v -> sendJoinEventToServer(uid, eid));
            }
        }
    }

    private void sendJoinEventToServer(int uid, int eid) {
        String url = Configuration.BASE_URL + "/join-event?uid=" + uid + "&eid=" + eid;

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
                error -> Toast.makeText(this, "Failed to join", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void declineInvite(int uid, int eid) {
        String url = Configuration.BASE_URL + "/decline-invite?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Toast.makeText(this, "Invite declined", Toast.LENGTH_SHORT).show();

                    joinButtonSide.setVisibility(View.GONE);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            (int) (getResources().getDisplayMetrics().density * 250),
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                    declineButton.setLayoutParams(params);

                    declineButton.setText("Declined");
                    declineButton.setEnabled(false);

                    slideOut(invitationCard);
                },
                error -> Toast.makeText(this, "Error declining invite", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    // --- Animation helpers ---
    private void fadeOut(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1f); // reset for next time
                    });
        }
    }


    private void slideIn(View view) {
        view.setVisibility(View.VISIBLE);
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
    }

    private void slideOut(View view) {
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(anim);
    }

}