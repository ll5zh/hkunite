package com.example.comp3330_hkunite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.example.comp3330_hkunite.Configuration;

public class ExploreFragment extends Fragment {
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private Map<String, List<Event>> categorizedEvents = new LinkedHashMap<>();
    private static final String TAG = "ExploreFragment";

    private int uid = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewExplore);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load UID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, getContext().MODE_PRIVATE);
        uid = prefs.getInt("USER_ID", -1);
        Log.d(TAG, "Loaded UID in ExploreFragment: " + uid);

        loadEventsFromServer();

        EditText editTextSearch = view.findViewById(R.id.editTextSearch);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadEventsFromServer() {
        Log.d(TAG, "loadEventsFromServer() called");

        String url = Configuration.BASE_URL + "/events";

        JsonArrayRequest request = new JsonArrayRequest(
                url,
                response -> {
                    categorizedEvents.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);

                            int eid = obj.getInt("eid");
                            String title = obj.getString("title");
                            String description = obj.optString("description", "");
                            String image = obj.optString("image", null);
                            String date = obj.getString("date");
                            int cid = obj.optInt("cid", -1);
                            int ownerId = obj.optInt("oid", -1);

                            String categoryName = obj.optString("category_name", "Uncategorized");
                            String ownerUsername = obj.optString("owner_name", "Unknown");
                            String location = obj.optString("location", "");

                            Event event = new Event(eid, title, description, image, date, cid, categoryName, ownerUsername, location);

                            if (ownerId == uid) {
                                Log.d(TAG, "Skipping event " + eid + " owned by current user " + uid);
                                continue;
                            }

                            if (!categorizedEvents.containsKey(categoryName)) {
                                categorizedEvents.put(categoryName, new ArrayList<>());
                            }
                            categorizedEvents.get(categoryName).add(event);

                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error at index " + i, e);
                            Toast.makeText(getContext(), "Error parsing event data", Toast.LENGTH_SHORT).show();
                        }
                    }

                    categoryAdapter = new CategoryAdapter(getContext(), categorizedEvents);
                    recyclerView.setAdapter(categoryAdapter);

                },
                error -> {
                    Log.e(TAG, "Volley error in loadEventsFromServer: " + error.toString(), error);
                    Toast.makeText(getContext(), "Failed to load events: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(getContext()).add(request);
    }

    private void filterEvents(String keyword) {
        Map<String, List<Event>> filteredMap = new LinkedHashMap<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<String, List<Event>> entry : categorizedEvents.entrySet()) {
            List<Event> filteredList = new ArrayList<>();
            for (Event event : entry.getValue()) {
                if (event.getTitle().toLowerCase().contains(lowerKeyword) ||
                        event.getDescription().toLowerCase().contains(lowerKeyword) ||
                        event.getOwnerUsername().toLowerCase().contains(lowerKeyword) ||
                        event.getLocation().toLowerCase().contains(lowerKeyword)){
                    filteredList.add(event);
                }
            }
            if (!filteredList.isEmpty()) {
                filteredMap.put(entry.getKey(), filteredList);
            }
        }

        categoryAdapter = new CategoryAdapter(getContext(), filteredMap);
        recyclerView.setAdapter(categoryAdapter);
    }
}
