package com.example.comp3330_hkunite;

import android.content.Intent;
import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_USERID = "userID";
    private static final String ARG_PARAM2 = "param2";

    private Button buttonUpcomingField;
    private Button buttonHostingField;
    private Button buttonInvitationsField;
    private RecyclerView eventsRecyclerViewField;
    private TextView welcomeTextField;
    private TextView upcomingTextField;
    private HomeEventAdapter homeEventAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userID userID.
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String userID) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Context context = getContext();

        // Load UID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, getContext().MODE_PRIVATE);
        int uid = prefs.getInt("USER_ID", -1);
        Log.d(TAG, "Loaded UID in ExploreFragment: " + uid);

        //connect the buttons
        buttonUpcomingField = view.findViewById(R.id.buttonUpcoming);
        buttonHostingField = view.findViewById(R.id.buttonHosting);
        buttonInvitationsField = view.findViewById(R.id.buttonInvitations);
        eventsRecyclerViewField = view.findViewById(R.id.eventsRecyclerView);
        welcomeTextField = view.findViewById(R.id.welcomeText);
        upcomingTextField = view.findViewById(R.id.upcomingText);

        //by default show all upcoming events
        showEvents(uid, "upcoming");

        buttonUpcomingField.setOnClickListener(v -> {
            showEvents(uid, "upcoming");
        });

        buttonHostingField.setOnClickListener(v -> {
            showEvents(uid, "hosting");
        });

        buttonInvitationsField.setOnClickListener(v -> {
            showEvents(uid, "invitations");
        });

        getUsername(uid);

        //TODO:: Implement username and numbers (Number of invitations, upcoming events)

        return view;
    }

private void showEvents(int uID, String filter) {
    // Load data from database. Loads all events associated with user by default
    String serverUrl = "http://10.0.2.2:5000/my-events?uid=" + uID;
    if (filter.equals("hosting")) {
        serverUrl = "http://10.0.2.2:5000/my-organized-events?uid=" + uID;
    } else if (filter.equals("invitations")) {
        //TODO:: Something still wrong with invitations
        serverUrl = "http://10.0.2.2:5000/my-invites?uid=" + uID;
    }


    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            serverUrl,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.d("API Debug", "Full response: " + response.toString());

                        // Check if response has "success" field
                        if (response.has("success") && !response.getBoolean("success")) {
                            Log.e("API", "Server returned success: false");
                            Toast.makeText(getContext(), "Server error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get events array - try different possible field names
                        JSONArray eventsArray = null;
                        if (response.has("data")) {
                            eventsArray = response.getJSONArray("data");
                        } else if (response.has("events")) {
                            eventsArray = response.getJSONArray("events");
                        } else {
                            Log.e("API", "No events array found in response");
                            Toast.makeText(getContext(), "No events data found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.d("API Debug", "Events array length: " + eventsArray.length());

                        // Parse events with proper field names
                        ArrayList<Event> eventsList = new ArrayList<>(); //parseEvents(eventsArray);
                        for (int i = 0; i < eventsArray.length(); i++) {
                            JSONObject eventJson = eventsArray.getJSONObject(i);

                            // Log each event to see actual structure
                            Log.d("EventDebug", "Event " + i + ": " + eventJson.toString());

                            // Use proper field names - adjust based on what your backend actually returns
                            Event event = new Event(
                                    eventJson.getInt("eid"),  // or "EID" if uppercase
                                    eventJson.getString("title"),  // or "TITLE"
                                    eventJson.optString("description", ""),  // Use optString with default
                                    eventJson.optString("image"),  // Use actual image field with fallback
                                    eventJson.getString("date"),  // or "DATE"
                                    eventJson.optInt("cid", 0),  // Use optInt with default
                                    eventJson.optString("category_name", "Unknown"),  // or "CATEGORY_NAME"
                                    eventJson.optString("owner_username", "Unknown")  // or "OWNER_USERNAME" or "oid"
                            );
                            eventsList.add(event);
                        }

                        Log.d("API Debug", "Parsed events: " + eventsList.size());

                        // Display events in UI
//                        showEventsinUI(eventsList);
//                        homeEventAdapter = new CategoryAdapter(getContext(), eventsList);
//                        eventsRecyclerViewField.setAdapter(homeEventAdapter);

                        HomeEventAdapter adapter = new HomeEventAdapter(getContext(), eventsList);
                        eventsRecyclerViewField.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                        eventsRecyclerViewField.setAdapter(adapter);


                        Toast.makeText(getContext(), "Loaded " + eventsList.size() + " events", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Log.e("Volley", "JSON parsing error: " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Volley", "Error: " + error.toString());
                    Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    Log.d("HomeFragment", "Request added to queue");
}
//    private void showEventsinUI(ArrayList<Event> events) {
//        if (getActivity() != null) {
//            getActivity().runOnUiThread(() -> {
//                EventAdapter.updateEvents(events);
//
//                if (events.isEmpty()) {
//                    Log.d("HomeFragment", "No events to display");
//                    // You can show a "No events" message here
//                } else {
//                    Log.d("HomeFragment", "Displaying " + events.size() + " events in RecyclerView");
//                }
//            });
//        }
//    }

//    private View createEventView(Event event) {
//        LayoutInflater inflater = LayoutInflater.from(getContext());
//        View eventView = inflater.inflate(R.layout.events_layout, eventsCarosel, false);
//
//        // Set proper layout params for horizontal scrolling
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        params.setMargins(8, 0, 8, 0); // Reduced margins for better scrolling
//        eventView.setLayoutParams(params);
//
//        ImageView eventImageField = eventView.findViewById(R.id.EventImage);
//        TextView eventTitleField = eventView.findViewById(R.id.EventTitle);
//
//        // Set fixed dimensions for ImageView
//        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(280, 180); // Slightly smaller for better fit
//        eventImageField.setLayoutParams(imageParams);
//
//        // Load image and set text (your existing code)
//        String imageUrl = event.getImageUrl();
//        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
//            Glide.with(getContext())
//                    .load(imageUrl)
//                    .placeholder(android.R.drawable.ic_dialog_info)
//                    .error(android.R.drawable.ic_dialog_alert)
//                    .into(eventImageField);
//        } else {
//            eventImageField.setImageResource(android.R.drawable.ic_dialog_map);
//        }
//
//        eventTitleField.setText(event.getTitle());
//
//        eventView.setOnClickListener(v -> {
//            openEventDetails(event.getEid());
//        });
//
//        return eventView;
//    }

    // ✅ NEW METHOD: Handle opening event details
    private void openEventDetails(int eventId) {
        // Create the fragment you want to open (EventDetailFragment or EditEventFragment)
        EditEventFragment fragment = new EditEventFragment();

        // Pass the event ID via arguments
        Bundle args = new Bundle();
        args.putInt("EID", eventId);
        fragment.setArguments(args);


        // Use FragmentManager from the fragment (not Activity)
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment) // Make sure you have this container in your activity
                .addToBackStack("home_to_event_detail") // Add to back stack so user can go back
                .commit();

        Log.d(TAG, "Opening event details for EID: " + eventId);
    }


    private void getUsername(int uID){
        if (welcomeTextField == null || upcomingTextField == null) {
            Log.e(TAG, "TextViews not initialized");
            return;
        }

        String serverUrl = "http://10.0.2.2:5000/users/" + uID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d("API Debug", "Full response: " + response.toString());

                            if (response.has("success") && !response.getBoolean("success")) {
                                Log.e("API", "Server returned success: false");
                                return;
                            }

                            // Update UI on main threadill;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        welcomeTextField.setText("Welcomehh " + response.getString("name") + "!");
                                        upcomingTextField.setText("You have " + response.getInt("joined_count") + " upcoming events!");
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error updating UI", e);
                                    }
                                });
                            }

                        } catch (JSONException e) {
                            Log.e("Volley", "JSON parsing error: " + e.getMessage());
                        }
                    }
                },
                error -> {
                    Log.e("Volley", "Error: " + error.toString());
                }
        );

        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }

}
