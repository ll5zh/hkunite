package com.example.comp3330_hkunite;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.content.Context;
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

    //to upload it to the database and for connection with server
    private RequestQueue queue;
    private final String BASE_URL = Configuration.BASE_URL;
    private static final String TAG = "AddFragmentUpload";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getContext() != null) {
            queue = Volley.newRequestQueue(getContext());
        }

        // here i should now create the database i think
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

        addButtonField.setOnClickListener(v ->{
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

    //to upload our data to the database
    private void uploadEventToDatabase(){
        if (queue == null) {
            Toast.makeText(getContext(), "Network queue not ready.", Toast.LENGTH_SHORT).show();
            return;
        }


        // This is where we package the data into the server's required format to then send it off to the server
        JSONObject eventData = new JSONObject();
        try{
            String title = titleField.getText().toString();
            String description = descriptionField.getText().toString();
            String date = dateField.getText().toString();
            String time = timeField.getText().toString();

            //CHANGE THIS AFTERWARDS:
            String organizerId = "1"; // THIS IS JUST FOR NOW; THIS IS JUST BECAUSE I DONT HAVE THAT IMPLEMENTED YET

            String fullDateTime = date + " " + time; //summarizing date and time into one string

            boolean isPublic = !switchPrivateField.isChecked();

            //now we put the the variables/strings into the Json object using .put
            eventData.put("title", title);
            eventData.put("description", description);
            eventData.put("oid", organizerId);
            eventData.put("date", fullDateTime);
            eventData.put("public", isPublic);

        }catch (JSONException e) {
            Log.e(TAG, "Error preparing JSON data: " + e.getMessage());
            Toast.makeText(getContext(), "Error preparing data for server.", Toast.LENGTH_SHORT).show();
            return;}


        //now we set up the POST request so we request that it gets posted to the server
        String url = BASE_URL + "/add-event";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,  // Use POST to upload/create data
                url,
                eventData,
                response -> {
                    try{
                        if (response.getBoolean("success")) {
                            String eventId = response.getString("eid");
                            Toast.makeText(getContext(), "Event Created! ID: " + eventId, Toast.LENGTH_LONG).show();
                            // You might want to navigate to another screen here
                        } else {
                            String errorMsg = response.optString("error", "Unknown server error.");
                            Toast.makeText(getContext(), "Server Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing success response: " + e.getMessage());
                    }

                },
                error -> {
                    Log.e(TAG, "Volley Network Error: " + error.toString());
                    Toast.makeText(getContext(), "Network Error: Cannot connect to server.", Toast.LENGTH_LONG).show();
                });

                queue.add(request);


    }

}