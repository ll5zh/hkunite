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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class AddFragment extends Fragment {

    //initializing the variables:
    private Button addButtonField;
    private Button uploadImageButtonField;
    private EditText titleField;
    private EditText locationField;
    private EditText dateField;
    private EditText timeField;
    private EditText descriptionField;
    private Switch switchPrivateField;
    private ImageView CoverPicture;
    private Button LocButtonField;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private static final String BASE_URL = Configuration.BASE_URL;

    //for the google maps implementation
    private PlacesClient placesClient;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001; // Request code for the Activity Result



    private RequestQueue requestQueue;
    private static final String TAG = "AddFragment";
    private static final String PREF_NAME = "HKUnitePrefs";
    private void initializeImageOptions() {
        imageOptions.add(new ImageOption("Study Session", "https://images.pexels.com/photos/159775/library-la-trobe-study-students-159775.jpeg"));
        imageOptions.add(new ImageOption("Party", "https://images.pexels.com/photos/2034851/pexels-photo-2034851.jpeg"));
        imageOptions.add(new ImageOption("Sports", "https://images.pexels.com/photos/69773/uss-nimitz-basketball-silhouettes-sea-69773.jpeg"));
        imageOptions.add(new ImageOption("Food", "https://images.pexels.com/photos/3026808/pexels-photo-3026808.jpeg"));
        imageOptions.add(new ImageOption("Lecture", "https://images.pexels.com/photos/1708912/pexels-photo-1708912.jpeg"));
        imageOptions.add(new ImageOption("Cafe", "https://images.pexels.com/photos/1402407/pexels-photo-1402407.jpeg"));
        imageOptions.add(new ImageOption("Hangout", "https://images.pexels.com/photos/745045/pexels-photo-745045.jpeg"));

        imageOptions.add(new ImageOption("Placeholder", "")); // Allow clearing the image
    }

    //to select the image and all that
    private String selectedImageUrl = ""; // Holds the URL that will be sent
    private final List<ImageOption> imageOptions = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeImageOptions();

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
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        Context context = getContext();

        //connecting all the buttons to their UI button, use: findViewById(R.id.btn_add)
        addButtonField = view.findViewById(R.id.addButton);
        uploadImageButtonField = view.findViewById(R.id.add_image);
        titleField = view.findViewById(R.id.editTextTitle);
        locationField = view.findViewById(R.id.editTextLocation);
        dateField = view.findViewById(R.id.editTextDate);
        timeField = view.findViewById(R.id.editTextTime);
        descriptionField = view.findViewById(R.id.editTextDescription);
        switchPrivateField = view.findViewById(R.id.switchPrivate);
        CoverPicture = view.findViewById(R.id.picture);
        LocButtonField = view.findViewById(R.id.LocationButton);
        registerPictureUpload();

        //making the datepicker and timepicker pop up:
        dateField.setOnClickListener(v -> {
            openDatePicker(dateField);
        });
        timeField.setOnClickListener(v -> {
            openTimePicker(timeField);
        });
        //uploadImageButtonField.setOnClickListener(v -> {
        //    uploadImage(CoverPicture);
        //});

        uploadImageButtonField.setOnClickListener(v -> {
            showImageSelectionDialog(); // Call the new method
        });

        locationField.setOnClickListener(v -> { launchPlaceAutocomplete(); });
        LocButtonField.setOnClickListener(v -> { launchPlaceAutocomplete(); });

        switchPrivateField.setOnClickListener(v -> {
            Boolean isPrivate = switchPrivateField.isChecked();});

        //to finally upload the whole event
        addButtonField.setOnClickListener( v -> {
            uploadEventToDatabase();
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

    private void uploadEventToDatabase() {
        if (requestQueue == null) {
            Toast.makeText(getContext(), "Network error: Request queue not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleField.getText().toString();
        String description = descriptionField.getText().toString();
        String location = locationField.getText().toString().trim();
        String date = dateField.getText().toString();
        String time = timeField.getText().toString();
        boolean isPublic = !switchPrivateField.isChecked();
        int publicValue = isPublic ? 1 : 0; // 1 = public, 0 = private
        String imageURLToSend = selectedImageUrl.isEmpty() ? "" : selectedImageUrl;

        int categoryId = 1; // Example category ID
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int organizerId = prefs.getInt("USER_ID", -1);

        if (organizerId == -1) {
            Toast.makeText(getContext(), "User not logged in. Cannot create event.", Toast.LENGTH_LONG).show();
            return;
        }

        String url = BASE_URL + "/add-event";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("description", description);
            jsonBody.put("oid", organizerId);
            jsonBody.put("cid", categoryId);
            jsonBody.put("public", publicValue);
            jsonBody.put("date", date + " " + time);
            jsonBody.put("location", location);
            jsonBody.put("image", imageURLToSend);
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error for add-event", e);
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
                            int eid = response.getInt("eid");
                            Toast.makeText(getContext(), "Event added successfully! ID: " + eid, Toast.LENGTH_LONG).show();
                            resetFormFields();
                        } else {
                            String errorMsg = response.optString("error", "Unknown server error.");
                            Toast.makeText(getContext(), "Failed to add event: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Event added, but response could not be parsed.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error adding event: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        );
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

