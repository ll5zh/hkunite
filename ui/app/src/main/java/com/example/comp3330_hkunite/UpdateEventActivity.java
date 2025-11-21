package com.example.comp3330_hkunite;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateEventActivity extends AppCompatActivity {

    private EditText inputTitle, inputDate, inputDescription;
    private Button saveButton;
    private int eventId = -1;
    private static final String TAG = "UpdateEventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_event);

        inputTitle = findViewById(R.id.inputTitle);
        inputDate = findViewById(R.id.inputDate);
        inputDescription = findViewById(R.id.inputDescription);
        saveButton = findViewById(R.id.buttonSaveChanges);

        eventId = getIntent().getIntExtra("EID", -1);

        if (eventId == -1) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCurrentData(eventId);

        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void loadCurrentData(int eid) {
        String url = Configuration.BASE_URL + "/events/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.has("data") ? response.getJSONObject("data") : response;

                        inputTitle.setText(data.optString("title"));
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
        } catch (JSONException e) { return; }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Update Successful!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }
}