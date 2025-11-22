package com.example.comp3330_hkunite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.libraries.places.api.model.Place;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class UpdateEventActivity extends AppCompatActivity {

    private EditText inputTitle, inputDate, inputLocation, inputDescription;
    private Button buttonSearchLocation;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2001;
    private Button saveButton;
    private ImageButton backButton;
    private int eventId = -1;
    private static final String TAG = "UpdateEventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_event);

        inputTitle = findViewById(R.id.inputTitle);
        inputDate = findViewById(R.id.inputDate);
        inputDescription = findViewById(R.id.inputDescription);
        inputLocation = findViewById(R.id.inputLocation);   // NEW
        buttonSearchLocation = findViewById(R.id.buttonSearchLocation); // NEW
        saveButton = findViewById(R.id.buttonSaveChanges);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                    getApplicationContext(),
                    getString(R.string.google_maps_key)
            );
        }

        eventId = getIntent().getIntExtra("EID", -1);

        if (eventId == -1) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCurrentData(eventId);

        // Launch autocomplete when clicking either field or button
        inputLocation.setOnClickListener(v -> launchPlaceAutocomplete());
        buttonSearchLocation.setOnClickListener(v -> launchPlaceAutocomplete());

        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void launchPlaceAutocomplete() {
        List<com.google.android.libraries.places.api.model.Place.Field> fields =
                java.util.Arrays.asList(
                        com.google.android.libraries.places.api.model.Place.Field.ID,
                        com.google.android.libraries.places.api.model.Place.Field.NAME,
                        com.google.android.libraries.places.api.model.Place.Field.ADDRESS
                );

        Intent intent = new com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
                com.google.android.libraries.places.widget.model.AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                com.google.android.libraries.places.api.model.Place place =
                        com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(data);

                String fullAddress = place.getAddress();
                inputLocation.setText(fullAddress);
                Toast.makeText(this, "Location Set: " + place.getName(), Toast.LENGTH_LONG).show();

            } else if (resultCode == com.google.android.libraries.places.widget.AutocompleteActivity.RESULT_ERROR && data != null) {
                com.google.android.gms.common.api.Status status =
                        com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(data);
                Log.e(TAG, "Place Autocomplete Error: " + status.getStatusMessage());
                Toast.makeText(this, "Error selecting location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadCurrentData(int eid) {
        String url = Configuration.BASE_URL + "/events/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.has("data") ? response.getJSONObject("data") : response;

                        inputTitle.setText(data.optString("title"));
                        inputLocation.setText(data.optString("location"));
                        inputDescription.setText(data.optString("description"));
                        inputDate.setText(data.optString("date"));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error", e);
                    }
                },
                error -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void saveChanges() {
        String title = inputTitle.getText().toString();
        String desc = inputDescription.getText().toString();
        String date = inputDate.getText().toString();
        String location = inputLocation.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Configuration.BASE_URL + "/update-event/" + eventId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("description", desc);
            jsonBody.put("date", date);
            jsonBody.put("location", location);
        } catch (JSONException e) { return; }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Update Successful!", Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("UPDATED", true);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                },
                error -> {
                    Log.e("UpdateEventActivity", "Update failed: " + error.toString());
                    // Still finish, but with canceled to avoid silent failures
                    setResult(RESULT_CANCELED);
                    finish();
                });
        request.setShouldCache(false); // avoid stale cache
        Volley.newRequestQueue(this).add(request);
    }


}