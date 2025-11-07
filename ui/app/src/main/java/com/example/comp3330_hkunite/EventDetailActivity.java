package com.example.comp3330_hkunite;

import android.os.Bundle;
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
    private TextView eventTitle, eventDate, eventDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventImage = findViewById(R.id.eventImage);
        eventTitle = findViewById(R.id.eventTitle);
        eventDate = findViewById(R.id.eventDate);
        eventDescription = findViewById(R.id.eventDescription);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        int eid = getIntent().getIntExtra("EID", -1);
        if (eid != -1) {
            Event event = DatabaseHelper.getInstance(this).getEvent(eid);
            if (event != null) {
                eventTitle.setText(event.getTitle());
                eventDate.setText(event.getDate());
                eventDescription.setText(event.getDescription());
                if (event.getImageUrl() != null) {
                    Glide.with(this).load(event.getImageUrl()).into(eventImage);
                }
            }
        }

        Button joinButton = findViewById(R.id.buttonJoinEvent);
        int uid = 2; // Replace with actual user ID logic

        boolean alreadyJoined = DatabaseHelper.getInstance(this).hasJoinedEvent(uid, eid);
        if (alreadyJoined) {
            joinButton.setEnabled(false);
            joinButton.setText("Joined");
        }

        joinButton.setOnClickListener(v -> {
            boolean success = DatabaseHelper.getInstance(this).joinEvent(uid, eid);
            if (success) {
                sendJoinEventToServer(uid, eid); // 🔹 Send to server
                Toast.makeText(this, "You joined this event!", Toast.LENGTH_SHORT).show();
                joinButton.setEnabled(false);
                joinButton.setText("Joined");
            } else {
                Toast.makeText(this, "You have already joined this event.", Toast.LENGTH_SHORT).show();
            }
        });


    }
    private void sendJoinEventToServer(int uid, int eid) {
        String url = "http://10.0.2.2:5001/join-event"; // Replace with your actual endpoint

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
            jsonBody.put("eid", eid);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Toast.makeText(this, "Joined on server!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Toast.makeText(this, "Server error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

}

