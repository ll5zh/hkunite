package com.example.comp3330_hkunite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class ProfileFragment extends Fragment {

    //server and user information:
    private static final String TAG = "ProfileFragment";
    private static final String BASE_URL = "http://10.70.8.141:5001";
    private int currentUserID; //getting this from SharedPreferences

    //ui stuff:
    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private TextView eventsOrganizedCount, eventsJoinedCount;
    private Button logoutButton;

    //adapter and list:
    private RecyclerView badgesRecyclerView;
    private BadgeAdapter badgeAdapter;
    private List<Badge> badgeList = new ArrayList<>();

    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();

    //volley logic
    private RequestQueue queue;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //get logged in user's id:
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        currentUserID = prefs.getInt("USER_ID", -1);

        if (currentUserID == -1) {
            //no one logged in go back to login page
            Log.e(TAG, "User ID not found in SharedPreferences. Logging out.");
            goToLogin();
            return null;
        }

        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        //initializing ui items:
        profileImage = root.findViewById(R.id.profile_image);
        profileName = root.findViewById(R.id.profile_name);
        profileEmail = root.findViewById(R.id.profile_email);
        eventsOrganizedCount = root.findViewById(R.id.profile_events_organized_count);
        eventsJoinedCount = root.findViewById(R.id.profile_events_joined_count);
        badgesRecyclerView = root.findViewById(R.id.profile_badges_recyclerview);
        eventsRecyclerView = root.findViewById(R.id.profile_events_recyclerview);
        logoutButton = root.findViewById(R.id.profile_logout_button);

        //initializing volley:
        queue = Volley.newRequestQueue(requireContext());

        //logout button:
        logoutButton.setOnClickListener(v -> {
            //clear user
            SharedPreferences.Editor editor = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
            goToLogin(); //go back
        });

        setupRecyclerViews();

        loadProfileData();

        return root;
    }

    private void setupRecyclerViews() {
        //badge list:
        badgeAdapter = new BadgeAdapter(badgeList);
        badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        badgesRecyclerView.setAdapter(badgeAdapter);

        //events list:
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(eventAdapter);
        eventsRecyclerView.setNestedScrollingEnabled(false);
    }

    //This is our main data-loading function.
    //It calls the MOCK functions for missing endpoints
    //and the REAL function for the my events endpoint --> because we have this in the database until now
    private void loadProfileData() {
        //MOCK DATA
        //Endpoints are MISSING for user info and badges.
        loadMockBasicInfo(currentUserID);
        loadMockBadges(currentUserID);

        //REAL DATA
        //Endpoint EXISTS for events.
        fetchUserEvents(currentUserID);
    }

    //MOCK FUNC 1
    private void loadMockBasicInfo(int uid) {
        // TODO: Replace this when server endpoint GET /user/<uid> is ready

        String name = "Test User";
        String email = "test@user.com";
        String orgCount = "0";
        String joinCount = "0";
        String imageUrl = null;

        //for now user 1 is Alice, i can also add more users...
        if (uid == 1) {
            name = "Alice";
            email = "u3649750@connect.hku.hk";
            orgCount = "3";
            joinCount = "2";
        } else if (uid == 2) {
            name = "Bob";
            email = "user2@hku.hk";
        }

        profileName.setText(name);
        profileEmail.setText(email);
        eventsOrganizedCount.setText(orgCount);
        eventsJoinedCount.setText(joinCount);

        Glide.with(this)
                .load(imageUrl) //load the null URL
                .placeholder(R.drawable.default_profile) //it will show this placeholder
                .circleCrop()
                .into(profileImage);
    }

    //MOCK FUNC 2
    private void loadMockBadges(int uid) {
        // TODO: Replace this when server endpoint GET /user/<uid>/badges is ready
        badgeList.clear();

        //only Alice has badges for now
        if (uid == 1) {
            badgeList.add(new Badge("Early Bird", null));
            badgeList.add(new Badge("Python Pro", null));
        }

        badgeAdapter.notifyDataSetChanged();
    }

    //real func for events:
    private void fetchUserEvents(int uid) {
        String url = BASE_URL + "/my-events?uid=" + uid; //this is the real url

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray eventsArray = response.getJSONArray("data");

                            eventList.clear();
                            for (int i = 0; i < eventsArray.length(); i++) {
                                JSONObject eventObject = eventsArray.getJSONObject(i);

                                Event event = new Event(
                                        eventObject.getInt("eid"),
                                        eventObject.getString("title"),
                                        eventObject.getString("description"),
                                        eventObject.optString("image", null),
                                        eventObject.getString("date"),
                                        eventObject.getInt("cid"),
                                        "", // categoryName (missing from server response, but OK)
                                        ""  // ownerUsername (missing from server response, but OK)
                                );
                                eventList.add(event);
                            }
                            eventList.sort(Comparator.comparing(Event::getDate)); //sorted events by date

                            eventAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for events", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching events: " + error.toString());
                    Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(request);
    }
    //go back to login helper func
    private void goToLogin() {
        if (getActivity() == null) return; // Safety check

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}