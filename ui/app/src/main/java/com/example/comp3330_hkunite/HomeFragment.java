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
import com.example.comp3330_hkunite.Configuration;

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
    private ArrayList<Event> currentEvents;
    private LinearLayout eventsCarosel;

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

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            //where do I get the userID from??? How do I know who is who
////            userID = getArguments().getString(ARG_USERID);
////            loadUserEvents(uID);
//        }
//    }

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
        eventsCarosel = view.findViewById(R.id.eventsCarosel);

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

        return view;
    }
/*
private void showEvents(int uID, String filter) {
    // Load data from database. Loads all events associated with user by default
    String serverUrl = "http://10.70.208.59:5000/my-events?uid=" + uID;
    if (filter.equals("hosting")) {
        serverUrl = "http://10.70.208.59:5000/my-organized-events?uid=" + uID;
    } else if (filter.equals("invitations")) {
        serverUrl = "http://10.70.208.59:5000/my-invites/" + uID; //TODO:: Something still wrong with invitations
    }
*/
// config version:
private void showEvents(int uID, String filter) {
    // Use Configuration.BASE_URL so you don't have to type the IP again
    String serverUrl = Configuration.BASE_URL + "/my-events?uid=" + uID;

    if (filter.equals("hosting")) {
        serverUrl = Configuration.BASE_URL + "/my-organized-events?uid=" + uID;
    } else if (filter.equals("invitations")) {
        // Note: Check if your teammates changed this route to use ?uid= like the others!
        serverUrl = Configuration.BASE_URL + "/my-invites/" + uID;
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
                        ArrayList<Event> eventsList = parseEvents(eventsArray);
                        Log.d("API Debug", "Parsed events: " + eventsList.size());

                        // Display events in UI
                        showEventsinUI(eventsList);

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


    private ArrayList<Event> parseEvents(JSONArray eventsArray) throws JSONException {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < eventsArray.length(); i++) {
            JSONObject eventJson = eventsArray.getJSONObject(i);

            // Log each event to see actual structure
            Log.d("EventDebug", "Event " + i + ": " + eventJson.toString());

            //get_events_for_user

            try {
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

                Log.d("ImageLoading", "Event: " + event.getTitle() + ", Image: " + event.getImageUrl());
                events.add(event);

            } catch (JSONException e) {
                Log.e("ParseError", "Failed to parse event at index " + i + ": " + e.getMessage());
                // Continue with next event instead of failing entirely
            }
        }

        return events;
    }

    //TODO::maybe change this to a viewerpage or recycler view for better UI experience
    private void showEventsinUI(ArrayList<Event> events) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //update UI elements
                    eventsCarosel.removeAllViews();

                    for (Event event : events) {
                        View eventView = createEventView(event);
                        eventsCarosel.addView(eventView);
                    }

                    // Optional: Show a message if no events found
                    if (events.isEmpty()) {
                        // You might want to show a TextView with "No events found"
                        // For example: textViewNoEvents.setVisibility(View.VISIBLE);
                        Log.d("HomeFragment", "No events to display");
                    } else {
                        // Hide the no events message if it exists
                        // textViewNoEvents.setVisibility(View.GONE);
                        Log.d("HomeFragment", "Displaying " + events.size() + " events");
                    }
                }
            });
        }
    }

    private View createEventView(Event event){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View eventView = inflater.inflate(R.layout.events_layout, eventsCarosel, false);

        // Set proper layout params for the event view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 0, 16, 0);
        eventView.setLayoutParams(params);

        ImageView eventImageField = eventView.findViewById(R.id.EventImage);
        TextView eventTitleField = eventView.findViewById(R.id.EventTitle);

        // Set fixed dimensions for ImageView
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(300, 200);
        eventImageField.setLayoutParams(imageParams);

        // Load image from database using Glide with proper error handling
        String imageUrl = event.getImageUrl();
        Log.d("ImageLoad", "Loading image for: " + event.getTitle());
        Log.d("ImageLoad", "Image URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            // Use the actual image URL from the database
            Glide.with(getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_dialog_info) // Show while loading
                    .error(android.R.drawable.ic_dialog_alert) // Show if error
                    .into(eventImageField);
            Log.d("ImageLoad", "Glide load called for: " + imageUrl);
        } else {
            // Use fallback image if no URL or empty URL
            Log.w("ImageLoad", "No valid image URL for: " + event.getTitle() + ", using fallback");
            eventImageField.setImageResource(android.R.drawable.ic_dialog_map);
        }

        eventTitleField.setText(event.getTitle());

        return eventView;
    }

}
