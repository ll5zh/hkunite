package com.example.comp3330_hkunite;

import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {
    private RecyclerView recyclerView;
    private ExploreAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private static final String TAG = "ExploreFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewExplore);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new ExploreAdapter(getContext(), events);
        recyclerView.setAdapter(adapter);

        loadEventsFromServer();

        EditText editTextSearch = view.findViewById(R.id.editTextSearch);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadEventsFromServer() {
        Log.d("ExploreFragment", "loadEventsFromServer() called");

        String url = "http://10.70.8.141:5001/events"; // ✅ Use correct IP for physical device

        JsonArrayRequest request = new JsonArrayRequest(
                url,
                response -> {
                    Log.d(TAG, "Received events JSON: " + response.toString());
                    events.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            int eid = obj.getInt("EID");
                            String title = obj.getString("TITLE");
                            String description = obj.getString("DESCRIPTION");
                            String image = obj.optString("IMAGE", null);
                            String date = obj.getString("DATE");

                            Log.d(TAG, "Parsed event: " + title + " (EID: " + eid + ")");
                            events.add(new Event(eid, title, description, image, date));
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error at index " + i, e);
                            Toast.makeText(getContext(), "Error parsing event data", Toast.LENGTH_SHORT).show();
                        }
                    }
                    adapter.updateEvents(events);
                    //Toast.makeText(getContext(), "Loaded " + events.size() + " events", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "Volley error in loadEventsFromServer: " + error.toString(), error);
                    Toast.makeText(getContext(), "Failed to load events: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(getContext()).add(request);
    }
}
