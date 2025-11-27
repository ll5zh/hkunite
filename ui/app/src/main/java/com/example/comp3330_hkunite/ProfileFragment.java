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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

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

    private static final String BASE_URL = Configuration.BASE_URL;
    private int currentUserID;

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
    private ExploreAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();

    //volley logic:
    private RequestQueue queue;
    private ScrollView headerContainer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //get logged in user's id:
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        currentUserID = prefs.getInt("USER_ID", -1);

        if (currentUserID == -1) {
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
        headerContainer = root.findViewById(R.id.headerContainer);
        //initializing volley:
        queue = Volley.newRequestQueue(requireContext());

        //logout button:
        logoutButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
            goToLogin();
        });

        setupRecyclerViews();

        loadProfileData();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        //fetch events again every time the fragment comes back into view
        if (currentUserID != -1) {
            fetchUserEvents(currentUserID);
        }
    }


    private void setupRecyclerViews() {
        //badge list:
        badgeAdapter = new BadgeAdapter(badgeList);
        badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        badgesRecyclerView.setAdapter(badgeAdapter);

        //events list:
        eventAdapter = new ExploreAdapter(getContext(), eventList);
        eventsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        eventsRecyclerView.setAdapter(eventAdapter);
        eventsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadProfileData() {
        fetchUserInfo(currentUserID);

        fetchUserBadges(currentUserID);

        fetchUserEvents(currentUserID);
    }

    private void fetchUserInfo(int uid) {
        String url = BASE_URL + "/users/" + uid;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    //if the user left the screen, stop here dont try to load images
                    if (!isAdded() || getActivity() == null) {
                        return;
                    }
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject user = response.getJSONObject("data");

                            String name = user.getString("name");
                            String email = user.getString("email");
                            String imageUrl = user.optString("image", null);

                            //counts calculated by server.py in db
                            int orgCount = user.optInt("organized_count", 0);
                            int joinCount = user.optInt("joined_count", 0);

                            //updating ui
                            profileName.setText(name);
                            profileEmail.setText(email);
                            eventsOrganizedCount.setText(String.valueOf(orgCount));
                            eventsJoinedCount.setText(String.valueOf(joinCount));
                            headerContainer.setVisibility(View.VISIBLE);
                            //load the image
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .circleCrop()
                                    .into(profileImage);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for user info", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching user info: " + error.toString());
                    //only show toast if still attached
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error loading profile info", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.add(request);
    }

    private void fetchUserBadges(int uid) {
        String url = BASE_URL + "/badges/" + uid;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray badgeArray = response.getJSONArray("data");

                            badgeList.clear();
                            for (int i = 0; i < badgeArray.length(); i++) {
                                JSONObject badgeObj = badgeArray.getJSONObject(i);
                                String name = badgeObj.getString("name");
                                String imageUrl = badgeObj.optString("image", null);

                                badgeList.add(new Badge(name, imageUrl));
                            }
                            badgeAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for badges", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching badges: " + error.toString());
                }
        );
        queue.add(request);
    }
    private void fetchUserEvents(int uid) {
        String url = BASE_URL + "/my-organized-events?uid=" + uid;

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
                                        "",
                                        "",
                                        ""
                                );
                                eventList.add(event);
                            }
                            eventList.sort(Comparator.comparing(Event::getDate));

                            eventAdapter.updateEvents(eventList);
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

    private void goToLogin() {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}