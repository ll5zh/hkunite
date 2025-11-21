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

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.Calendar;


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

    private RequestQueue requestQueue;
    private static final String TAG = "AddFragment";
    private static final String PREF_NAME = "HKUnitePrefs";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        uploadImageButtonField.setOnClickListener(v -> {
            uploadImage(CoverPicture);
        });
        LocButtonField.setOnClickListener(v -> {
            checkLocationPermissionGetLoc();});
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

    private void uploadEventToDatabase(){

        if (requestQueue == null) {
            Toast.makeText(getContext(), "Network error: Request queue not initialized", Toast.LENGTH_SHORT).show();
            return;
        }


        String title = titleField.getText().toString();
        String description = descriptionField.getText().toString();
        String location = locationField.getText().toString();
        String date = dateField.getText().toString();
        String time = timeField.getText().toString();
        boolean isPublic = !switchPrivateField.isChecked();
        int publicValue = isPublic ? 0 : 1;
        Log.d(TAG, "is it pucblic " + publicValue);
        String participants = "";


        //hardcoded for now
        int categoryId = 1; // Example category ID. Update this if you have a category selection spinner.

        //to get the organizers ID from the Loding activity
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int organizerId = prefs.getInt("USER_ID", -1);

        Log.d(TAG, "Retrieved Organizer ID (oid): " + organizerId);
        if (organizerId == -1) {
            Toast.makeText(getContext(), "User not logged in. Cannot create event.", Toast.LENGTH_LONG).show();
            return;
        }


        String dateTimeCombined = date + " " + time + " " + location; // Combining date, time, and location in one string for simplicity as per Flask 'date' field.


        String url = BASE_URL + "/add-event";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("description", description);
            jsonBody.put("oid", organizerId);
            jsonBody.put("cid", categoryId);
            jsonBody.put("public", 1); // Flask endpoint uses public=1/0
            jsonBody.put("date", dateTimeCombined);
            //jsonBody.put("participants",participants );
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error for add-event", e);
            Toast.makeText(getContext(), "Error preparing event data.", Toast.LENGTH_LONG).show();
            return;
        }
                        //so ispublic 0 = it is private
        //no mattter what: public = 1, private = 0
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    // Success listener
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            int eid = response.getInt("eid");
                            Toast.makeText(getContext(), "Event added successfully! ID: " + eid, Toast.LENGTH_LONG).show();
                            Log.i(TAG, "Event added successfully with EID: " + eid);
                            // Clear fields or navigate away after success
                            resetFormFields();
                        } else {
                            // Success is false from server (e.g., DB error)
                            String errorMsg = response.optString("error", "Unknown server error.");
                            Toast.makeText(getContext(), "Failed to add event: " + errorMsg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Server reported failure: " + errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error on success response", e);
                        Toast.makeText(getContext(), "Event added, but response could not be parsed.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    // Network or Volley error
                    String errorMsg = error.toString();
                    Log.e(TAG, "Volley error during add-event: " + errorMsg);
                    Toast.makeText(getContext(), "Error adding event: " + errorMsg, Toast.LENGTH_LONG).show();
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

}

