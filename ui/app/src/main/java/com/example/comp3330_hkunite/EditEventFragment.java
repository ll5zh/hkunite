package com.example.comp3330_hkunite;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.content.SharedPreferences; // For getting the organizer ID (oid)
import static android.content.Context.MODE_PRIVATE;


//for the pictures
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Confirmed to be available from ProfileFragment

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class EditEventFragment extends Fragment {

    //initializing the variables:
    private Button saveEditsButtonField;
    private Button uploadImageButtonField;
    private TextInputEditText titleField, locationField, dateField, timeField, descriptionField;
    private TextInputLayout textInputTitleField, textInputLocationField, textInputDateField;
    private Switch switchPrivateField;
    private ImageView CoverPicture;
    private Button LocButtonField;
    private int eid;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private static final String BASE_URL = Configuration.BASE_URL;

    //for the google maps implementation
    private PlacesClient placesClient;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001; // Request code for the Activity Result



    private RequestQueue requestQueue;
    private static final String TAG = "EditEventFragment";
    private static final String PREF_NAME = "HKUnitePrefs";


    //to select the image and all that
    private String selectedImageUrl = ""; // Holds the URL that will be sent
    private final List<ImageOption> imageOptions = new ArrayList<>();

    //TODO::put in logic for getting eID and maybe uID??
    //TODO::put in logic for editing database

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //for google maps:
        if (!com.google.android.libraries.places.api.Places.isInitialized() && getContext() != null) {
            // Android automatically reads the key from the manifest/strings.xml!
            // We just need to initialize the Places SDK.
            com.google.android.libraries.places.api.Places.initialize(getContext(), getContext().getString(R.string.google_maps_key));
        }

        //database stuff:
        if(getContext() != null){
            requestQueue = Volley.newRequestQueue(getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_event, container, false);
        Context context = getContext();

        //connecting all the buttons to their UI button, use: findViewById(R.id.btn_add)
//        saveEditsButtonField = view.findViewById(R.id.saveEditsButton);
        uploadImageButtonField = view.findViewById(R.id.add_image);
        titleField = view.findViewById(R.id.editTextTitle);
        locationField = view.findViewById(R.id.editTextLocation);
        dateField = view.findViewById(R.id.editTextDate);
        timeField = view.findViewById(R.id.editTextTime);
        descriptionField = view.findViewById(R.id.editTextDescription);
        switchPrivateField = view.findViewById(R.id.switchPrivate);
        CoverPicture = view.findViewById(R.id.picture);
        LocButtonField = view.findViewById(R.id.LocationButton);
//
//        textInputTitleField = view.findViewById(R.id.textInputTitle);
//        textInputLocationField = view.findViewById(R.id.textInputLocation);
//        textInputDateField = view.findViewById(R.id.textInputDate);
        registerPictureUpload();

        // Get UID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        int uid = prefs.getInt("USER_ID", -1);

        // Get EID from arguments
        //TODO::make sure this part works
        Bundle args = getArguments();
        if (args != null) {
            eid = args.getInt("EID", -1);
        }

        if (eid == -1 || uid == -1) {
            Toast.makeText(getContext(), "Invalid event or user ID", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed(); // Go back instead of finish()
            }
            return view;
        }

        //making the datepicker and timepicker pop up:
        dateField.setOnClickListener(v -> {
            openDatePicker(dateField);
        });
        timeField.setOnClickListener(v -> {
            openTimePicker(timeField);
        });
        uploadImageButtonField.setOnClickListener(v -> {
            uploadImage(CoverPicture);
        });

        uploadImageButtonField.setOnClickListener(v -> {
            showImageSelectionDialog(); // Call the new method
        });

        locationField.setOnClickListener(v -> { launchPlaceAutocomplete(); });
        LocButtonField.setOnClickListener(v -> { launchPlaceAutocomplete(); });

        switchPrivateField.setOnClickListener(v -> {
            Boolean isPrivate = switchPrivateField.isChecked();});


        //to finally upload the whole event
        saveEditsButtonField.setOnClickListener( v -> {
            //TODO::change this method
            saveEventEdits(eid);
        });


        //for the location:
        if (context != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            locationPermissionRequest = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                        if (fineLocationGranted != null && (fineLocationGranted|| coarseLocationGranted)){
                            handleLocationRetrieval();
                        } else{
                            Toast.makeText(getContext(), "location permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }

        return view;
    }

    private void populateEventDetails (int eid) {
        String url = "http://10.0.2.2:5000/events/" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            titleField.setText(data.getString("title"));
                            descriptionField.setText(data.optString("description", ""));
                            timeField.setText(data.getString("date"));
                            dateField.setText(data.getString("date"));

                            String image = data.optString("image", null);
                            if (image != null && !image.isEmpty()) {
                                Glide.with(this).load(image).into(CoverPicture);
                            }

                        } else {
                            Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        Toast.makeText(getContext(), "Error parsing event data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error.toString(), error);
                    Toast.makeText(getContext(), "Network/server error: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(getContext()).add(request);
    }


    //helper methods:
    public void openDatePicker(EditText date) {

        //we start a calendar (set on current day month and year to prefill)
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                getContext(), //so it opens the picker on the current fragment
                (view, y, m, d) -> date.setText(d + "/" + (m + 1) + "/" + y),
                year, month, day
        );
        datePicker.show();
    }

    public void openTimePicker(EditText time) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                getContext(),
                (view, h, m) -> time.setText(h + ":" + m),
                hour, minute, true
        );
        timePicker.show();
    }



    // NEW METHOD: Shows the visual selection dialog
    private void showImageSelectionDialog() {
        // 1. Inflate a simple layout for the dialog to hold the RecyclerView
        // You MUST create dialog_recyclerview_container.xml (see guide below)
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_recyclerview_container, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_dialog);

        // 2. Create the AlertDialog without showing it yet
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Select Cover Image")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, i) -> d.dismiss())
                .create();

        // 3. Set up the Adapter, passing the dialog reference
        ImageOptionAdapter adapter = new ImageOptionAdapter(imageOptions, dialog);

        // 4. Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // 5. Show the dialog
        dialog.show();
    }


    private void uploadImage(ImageView CoverPicture) {
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        pickImageLauncher.launch(intent);
    }

    private void registerPictureUpload() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            Uri imageUri = result.getData().getData();
                            CoverPicture.setImageURI(imageUri);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
        );
    }

    //for the location:
    private void checkLocationPermissionGetLoc() {

        Context context = getContext();
        if (context == null) return;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions already granted, go directly to location retrieval
            handleLocationRetrieval();
        } else {
            // Request permissions using the launcher
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }


    @SuppressWarnings("MissingPermission") // Permissions are checked in checkPermissionsAndRetrieveLocation()
    private void handleLocationRetrieval() {
        if (fusedLocationClient == null) return;

        // Use getCurrentLocation for a single, immediate update
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Location found
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Toast.makeText(getContext(), "Location: Lat " + latitude + ", Lon " + longitude, Toast.LENGTH_LONG).show();
                        String locString = "Latitude:"+latitude+" Longitude:"+longitude;
                        locationField.setText(locString);
                    } else {
                        // Location is null, might need to ensure location services are enabled on the device
                        Toast.makeText(getContext(), "Location not available, check device settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEventEdits(int eID) {
        if (requestQueue == null) {
            Toast.makeText(getContext(), "Network error: Request queue not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get updated values from form fields
        String title = titleField.getText().toString().trim();
        String description = descriptionField.getText().toString().trim();
        String date = dateField.getText().toString().trim();
        String time = timeField.getText().toString().trim();
        boolean isPublic = !switchPrivateField.isChecked(); // Assuming switchPrivate means private when checked

        // Basic validation
        if (title.isEmpty()) {
            titleField.setError("Title is required");
            return;
        }

        if (date.isEmpty()) {
            dateField.setError("Date is required");
            return;
        }

        // Combine date and time for the server
        String combinedDateTime = date;
        if (!time.isEmpty()) {
            combinedDateTime += " " + time + ":00"; // Format: "YYYY-MM-DD HH:MM:00"
        }

        // Hardcoded category ID (update if you have category selection)
        int categoryId = 1;

        String url = BASE_URL + "/edit-event";

        JSONObject jsonBody = new JSONObject();
        try {
            // REQUIRED: Event ID to identify which event to update
            jsonBody.put("eid", eID);

            // ONLY include fields that are in the allowed update list
            jsonBody.put("title", title);
            jsonBody.put("description", description);
            jsonBody.put("cid", categoryId);
            jsonBody.put("public", isPublic ? 1 : 0); // Convert boolean to int (1=true, 0=false)
            jsonBody.put("date", combinedDateTime);

            // Note: "image" is NOT in the can_update list, so it won't be processed
            // If you need to update images, you'll need a separate endpoint or add "image" to can_update

            Log.d(TAG, "Sending edit request for EID: " + eID);
            Log.d(TAG, "Update data - Title: " + title + ", Public: " + (isPublic ? 1 : 0) + ", Date: " + combinedDateTime);

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error for edit-event", e);
            Toast.makeText(getContext(), "Error preparing event data.", Toast.LENGTH_LONG).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject updatedEvent = response.getJSONObject("data");
                            Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_LONG).show();
                            Log.i(TAG, "Event updated successfully: " + updatedEvent.toString());

                            // Optionally navigate back or show success
                            if (getActivity() != null) {
                                getActivity().onBackPressed(); // Go back to previous screen
                            }

                        } else {
                            String errorMsg = response.optString("error", "Unknown server error.");
                            Toast.makeText(getContext(), "Failed to update event: " + errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Server reported failure: " + errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error on success response", e);
                        Toast.makeText(getContext(), "Error parsing server response", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    String errorMsg = error.toString();
                    Log.e(TAG, "Volley error during edit-event: " + errorMsg);

                    // More detailed error information
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "UTF-8");
                            Log.e(TAG, "Error response: " + responseBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not parse error response");
                        }
                    }

                    Toast.makeText(getContext(), "Network error while updating event", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public HashMap<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }


    private void resetFormFields() {
        titleField.setText("");
        locationField.setText("");
        dateField.setText("");
        timeField.setText("");
        descriptionField.setText("");
        switchPrivateField.setChecked(false);
        // Reset the ImageView to a default placeholder if possible
        CoverPicture.setImageResource(android.R.drawable.ic_menu_gallery);
    }





    //testing new idea for imageupload
    private static class ImageOption {
        String name;
        String url;

        ImageOption(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    // Adapter to display the image and name in the dialog
    private class ImageOptionAdapter extends RecyclerView.Adapter<ImageOptionAdapter.ViewHolder> {
        private final List<ImageOption> options;
        private final AlertDialog dialog; // Reference to the dialog to dismiss it

        public ImageOptionAdapter(List<ImageOption> options, AlertDialog dialog) {
            this.options = options;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // You MUST create dialog_image_item.xml (see guide below)
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_image_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageOption option = options.get(position);

            // Use Glide (confirmed available!) to load the URL
            Glide.with(holder.imageView.getContext())
                    .load(option.url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background) // Use any placeholder you have
                    .into(holder.imageView);

            holder.textView.setText(option.name);

            // Set click listener to pass the selected URL back to the fragment
            holder.itemView.setOnClickListener(v -> {
                // 1. Update the fragment's state
                selectedImageUrl = option.url;

                // Updating the  CoverPicture
                Glide.with(requireContext())
                        .load(option.url)
                        .centerCrop()
                        .into(CoverPicture);

                Toast.makeText(getContext(), "Cover image set to " + option.name, Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                // These IDs must match the ones you create in dialog_image_item.xml
                imageView = itemView.findViewById(R.id.image_preview);
                textView = itemView.findViewById(R.id.image_name);
            }
        }
    }


    //new location logic with google m aps.
    private void launchPlaceAutocomplete() {
        if (getContext() == null) return;

        // Define which pieces of data we want back from the selected place
        List<com.google.android.libraries.places.api.model.Place.Field> fields =
                java.util.Arrays.asList(
                        com.google.android.libraries.places.api.model.Place.Field.ID,
                        com.google.android.libraries.places.api.model.Place.Field.NAME,
                        com.google.android.libraries.places.api.model.Place.Field.ADDRESS // This is the full address string
                );

        // Build and launch the full-screen search activity
        Intent intent = new com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
                com.google.android.libraries.places.widget.model.AutocompleteActivityMode.FULLSCREEN, fields)
                .build(getContext());

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    // Inside AddFragment.java:
    @Override
    public void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                // Success: Retrieve the Place object
                com.google.android.libraries.places.api.model.Place place =
                        com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(data);

                // The location we save is the full address string
                String fullAddress = place.getAddress();

                // 1. Update the UI's EditText field
                locationField.setText(fullAddress);
                Toast.makeText(getContext(), "Location Set: " + place.getName(), Toast.LENGTH_LONG).show();

            } else if (resultCode == com.google.android.libraries.places.widget.AutocompleteActivity.RESULT_ERROR && data != null) {
                // Error handling
                com.google.android.gms.common.api.Status status = com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(data);
                Log.e(TAG, "Place Autocomplete Error: " + status.getStatusMessage());
                Toast.makeText(getContext(), "Error selecting location.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}

