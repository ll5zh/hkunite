package com.example.comp3330_hkunite;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_USERID = "userID";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String userID;
    private String welcomeText;
    private String upcomingText;
    private String invitationsText;
    private Button buttonUpcoming;
    private Button buttonHosting;
    private Button buttonInvitations;

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
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String userID) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERID, userID);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userID = getArguments().getString(ARG_USERID);
//            loadUserDataFromDatabase(userId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }


    private void showEvents(String uID, String filter) {
        //load data from database
        String serverUrl = "http://10.68.123.231:5000/my-events?uid=" + uID; //make sure for right user
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //return jsonify({"success": True, "data": list(my_events.values())}), 200
                            JSONArray eventsArray = response.getJSONArray("data");
                            ArrayList<Event> eventsList = parseEvents(eventsArray);

                            if (filter.equals("upcoming")){
                                eventsList = filterUpcoming(eventsList);
                            } else if(filter.equals("hosting")) {
                                eventsList = filterHosting(eventsList, uID);
                            } else if(filter.equals("invitations")) {
                                eventsList = filterInvitations(eventsList);
                            }

                            showEventsinUI(eventsList); //display the events

                        } catch (JSONException e) {
                            Log.e("Volley", "JSON parsing error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley", "Error: " + error.toString());
                    }
                }
        );

        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }



    private ArrayList<Event> parseEvents(JSONArray eventsArray) throws JSONException {
        ArrayList<Event> events = new ArrayList<>();

        for (int i = 0; i < eventsArray.length(); i++) {
            JSONObject eventJson = eventsArray.getJSONObject(i);

            Event event = new Event(
                    eventJson.getInt("EID"),
                    eventJson.getString("TITLE"),
                    eventJson.getString("DESCRIPTION"),
                    eventJson.getString("IMAGE"),
                    eventJson.getString("DATE"),
                    eventJson.getInt("CID"),
                    eventJson.getString("CATEGORY_NAME"),
                    eventJson.getString("OWNER_USERNAME")
            );

            Log.d("ImageLoading", "Loading image from: " + eventJson.getString("IMAGE"));

            events.add(event);
        }

        return events;
    }

    //TODO::maybe change this to a viewerpage for better UI experience
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
}