package com.example.comp3330_hkunite;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateEventActivity extends AppCompatActivity {

    private EditText inputTitle, inputDate, inputLocation, inputDescription;
    private Button buttonSearchLocation, saveButton;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2001;
    private ImageButton backButton;
    private ImageView imageCover;
    private Button buttonChangeImage;
    private SwitchMaterial switchPrivate;
    private MaterialButton btnAcademic, btnSocial, btnInternational, btnSports, btnArts;
    private Map<MaterialButton, Integer> categoryMap;
    private int eventId = -1;
    private String selectedImageUrl = "";
    private int selectedCategoryId = -1;
    private final List<ImageOption> imageOptions = new ArrayList<>();;
    private static final String TAG = "UpdateEventActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_event);

        initializeImageOptions();


        inputTitle = findViewById(R.id.inputTitle);
        inputDate = findViewById(R.id.inputDate);
        inputDescription = findViewById(R.id.inputDescription);
        inputLocation = findViewById(R.id.inputLocation);   // NEW
        buttonSearchLocation = findViewById(R.id.buttonSearchLocation); // NEW
        saveButton = findViewById(R.id.buttonSaveChanges);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        imageCover = findViewById(R.id.imageCover);
        buttonChangeImage = findViewById(R.id.buttonChangeImage);
        switchPrivate = findViewById(R.id.switchPrivate);

        btnAcademic = findViewById(R.id.btnAcademic);
        btnSocial = findViewById(R.id.btnSocial);
        btnInternational = findViewById(R.id.btnInternational);
        btnSports = findViewById(R.id.btnSports);
        btnArts = findViewById(R.id.btnArts);

        backButton.setOnClickListener(v -> finish());

        categoryMap = new HashMap<>();
        categoryMap.put(btnAcademic, 1);
        categoryMap.put(btnSocial, 2);
        categoryMap.put(btnInternational, 3);
        categoryMap.put(btnSports, 4);
        categoryMap.put(btnArts, 5);

        // Single-selection logic
        for (Map.Entry<MaterialButton, Integer> entry : categoryMap.entrySet()) {
            MaterialButton button = entry.getKey();
            button.setOnClickListener(v -> {
                for (Map.Entry<MaterialButton, Integer> e : categoryMap.entrySet()) {
                    MaterialButton b = e.getKey();
                    if (b == v) {
                        b.setChecked(true);
                        b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_secondary));
                        b.setTextColor(Color.WHITE);
                        selectedCategoryId = e.getValue();
                    } else {
                        b.setChecked(false);
                        b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                        b.setTextColor(Color.parseColor("#7E8C85"));
                    }
                }
            });
        }

        buttonChangeImage.setOnClickListener(v -> showImageSelectionDialog());



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
//                Toast.makeText(this, "Location Set: " + place.getName(), Toast.LENGTH_LONG).show();

            } else if (resultCode == com.google.android.libraries.places.widget.AutocompleteActivity.RESULT_ERROR && data != null) {
                com.google.android.gms.common.api.Status status =
                        com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(data);
                Log.e(TAG, "Place Autocomplete Error: " + status.getStatusMessage());
                Toast.makeText(this, "Error selecting location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeImageOptions() {
        imageOptions.add(new ImageOption("Study Session", "https://images.pexels.com/photos/159775/library-la-trobe-study-students-159775.jpeg"));
        imageOptions.add(new ImageOption("Party", "https://images.pexels.com/photos/2034851/pexels-photo-2034851.jpeg"));
        imageOptions.add(new ImageOption("Sports", "https://images.pexels.com/photos/69773/uss-nimitz-basketball-silhouettes-sea-69773.jpeg"));
        imageOptions.add(new ImageOption("Food", "https://images.pexels.com/photos/3026808/pexels-photo-3026808.jpeg"));
        imageOptions.add(new ImageOption("Lecture", "https://images.pexels.com/photos/1708912/pexels-photo-1708912.jpeg"));
        imageOptions.add(new ImageOption("Cafe", "https://images.pexels.com/photos/1402407/pexels-photo-1402407.jpeg"));
        imageOptions.add(new ImageOption("Hangout", "https://images.pexels.com/photos/745045/pexels-photo-745045.jpeg"));
        imageOptions.add(new ImageOption("Placeholder", ""));
    }

    private void showImageSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recyclerview_container, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_dialog);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Cover Image")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, i) -> d.dismiss())
                .create();

        ImageOptionAdapter adapter = new ImageOptionAdapter(imageOptions, dialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        dialog.show();
    }

    private static class ImageOption {
        String name;
        String url;
        ImageOption(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private class ImageOptionAdapter extends RecyclerView.Adapter<ImageOptionAdapter.ViewHolder> {
        private final List<ImageOption> options;
        private final AlertDialog dialog;

        ImageOptionAdapter(List<ImageOption> options, AlertDialog dialog) {
            this.options = options;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_image_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageOption option = options.get(position);

            Glide.with(holder.imageView.getContext())
                    .load(option.url)
                    .centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .into(holder.imageView);

            holder.textView.setText(option.name);

            holder.itemView.setOnClickListener(v -> {
                selectedImageUrl = option.url;
                if (!selectedImageUrl.isEmpty()) {
                    Glide.with(UpdateEventActivity.this)
                            .load(selectedImageUrl)
                            .centerCrop()
                            .into(imageCover);
                } else {
                    imageCover.setImageResource(R.drawable.image_placeholder);
                }
                dialog.dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_preview);
                textView = itemView.findViewById(R.id.image_name);
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

                        selectedImageUrl = data.optString("image", "");
                        if (!selectedImageUrl.isEmpty()) {
                            Glide.with(this).load(selectedImageUrl).centerCrop().into(imageCover);
                        } else {
                            imageCover.setImageResource(R.drawable.image_placeholder);
                        }

                        boolean isPublic = data.optInt("public", 1) == 1;
                        switchPrivate.setChecked(!isPublic);

                        int cid = data.optInt("cid", -1);
                        for (Map.Entry<MaterialButton, Integer> entry : categoryMap.entrySet()) {
                            if (entry.getValue() == cid) {
                                entry.getKey().setChecked(true);
                                entry.getKey().setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.brand_secondary));
                                entry.getKey().setTextColor(Color.WHITE);
                                selectedCategoryId = cid;
                            } else {
                                entry.getKey().setChecked(false);
                                entry.getKey().setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
                                entry.getKey().setTextColor(Color.parseColor("#7E8C85"));
                            }
                        }

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
        boolean isPublic = !switchPrivate.isChecked();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Category required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (location.isEmpty()) {
            Toast.makeText(this, "Location required", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = Configuration.BASE_URL + "/update-event/" + eventId;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("description", desc);
            jsonBody.put("date", date);
            jsonBody.put("location", location);
            jsonBody.put("cid", selectedCategoryId);
            jsonBody.put("public", isPublic ? 1 : 0);
            jsonBody.put("image", selectedImageUrl);
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