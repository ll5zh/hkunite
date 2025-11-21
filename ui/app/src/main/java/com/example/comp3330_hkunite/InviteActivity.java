package com.example.comp3330_hkunite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InviteActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> allUsers = new ArrayList<>();
    private int eventId;
    private LinearLayout selectedUsersContainer;
    private View selectedUsersDivider;
    private HorizontalScrollView selectedUsersScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        selectedUsersContainer = findViewById(R.id.selectedUsersContainer);
        selectedUsersDivider = findViewById(R.id.selectedUsersDivider);
        selectedUsersScroll = findViewById(R.id.selectedUsersScroll);

        eventId = getIntent().getIntExtra("EVENT_ID", -1);
        if (eventId == -1) {
            Toast.makeText(this, "No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        int currentUid = prefs.getInt("USER_ID", -1);
        Log.d("InviteActivity", "Loaded UID: " + currentUid);

        recyclerView = findViewById(R.id.recyclerViewInvite);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(userAdapter);

        EditText editTextSearch = findViewById(R.id.editTextSearchUsers);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Confirm button
        // Confirm button (arrow ImageButton)
        ImageButton confirmButton = findViewById(R.id.btnConfirmInvite);
        confirmButton.setOnClickListener(v -> {
            List<User> selectedUsers = new ArrayList<>();
            for (User u : allUsers) {
                if (u.isSelected()) {
                    selectedUsers.add(u);
                }
            }

            if (selectedUsers.isEmpty()) {
                Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                return;
            }

            for (User u : selectedUsers) {
                sendInvite(u.getUid(), eventId);
            }

            Toast.makeText(this, "Invites sent!", Toast.LENGTH_SHORT).show();
            finish();
        });


        loadUsersFromServer();
    }

    private void loadUsersFromServer() {
        // Pass eventId to exclude already invited or participating users
        String url = "http://10.0.2.2:5001/users?eid=" + eventId;

        JsonObjectRequest request = new JsonObjectRequest(
                url,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray usersArray = response.getJSONArray("users");
                            allUsers.clear();

                            SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
                            int currentUid = prefs.getInt("USER_ID", -1);

                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject obj = usersArray.getJSONObject(i);
                                int uid = obj.getInt("uid");

                                // Skip current user
                                if (uid == currentUid) {
                                    Log.d("InviteActivity", "Skipping current user " + uid);
                                    continue;
                                }

                                String name = obj.optString("name", "Unknown");
                                String image = obj.optString("image", null);
                                allUsers.add(new User(uid, name, image));
                            }
                            userAdapter.updateList(allUsers);

                            // Hook into adapter clicks
                            userAdapter.setOnUserClickListener(user -> {
                                if (user.isSelected()) {
                                    addSelectedUserChip(user);
                                } else {
                                    removeSelectedUserChip(user);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing user data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error: " + error.toString(), Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }


    private void filterUsers(String keyword) {
        List<User> filteredList = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (User user : allUsers) {
            if (user.getName().toLowerCase().contains(lowerKeyword)) {
                filteredList.add(user); // same object, selection preserved
            }
        }

        userAdapter.updateList(filteredList);
    }

    private void sendInvite(int uid, int eid) {
        String url = "http://10.70.208.59:5001/add-invite?uid=" + uid + "&eid=" + eid;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    // success
                },
                error -> {
                    Toast.makeText(this, "Failed to invite user " + uid, Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    // --- Selected user chips with animations ---
    private void addSelectedUserChip(User user) {
        View chip = LayoutInflater.from(this).inflate(R.layout.item_selected_user, selectedUsersContainer, false);

        ImageView img = chip.findViewById(R.id.selectedUserImage);
        TextView name = chip.findViewById(R.id.selectedUserName);
        ImageButton remove = chip.findViewById(R.id.removeSelectedUser);

        name.setText(user.getName());
        Glide.with(this).load(user.getImageUrl()).circleCrop().into(img);

        remove.setOnClickListener(v -> {
            user.setSelected(false);
            userAdapter.notifyDataSetChanged();

            // Animate chip removal
            chip.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        selectedUsersContainer.removeView(chip);
                        if (selectedUsersContainer.getChildCount() == 0) {
                            hideDivider();
                        }
                    });
        });

        chip.setTag(user.getUid());
        selectedUsersContainer.addView(chip);

        // Animate chip popping in
        chip.setScaleX(0.8f);
        chip.setScaleY(0.8f);
        chip.setAlpha(0f);
        chip.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200);

        showDivider();

        // Auto-scroll to end so new chip is visible
        selectedUsersScroll.post(() -> selectedUsersScroll.fullScroll(View.FOCUS_RIGHT));
    }

    private void removeSelectedUserChip(User user) {
        for (int i = 0; i < selectedUsersContainer.getChildCount(); i++) {
            View chip = selectedUsersContainer.getChildAt(i);
            Object tag = chip.getTag();
            if (tag instanceof Integer && ((Integer) tag) == user.getUid()) {
                // Smooth removal animation for consistency
                chip.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            selectedUsersContainer.removeView(chip);
                            if (selectedUsersContainer.getChildCount() == 0) {
                                hideDivider();
                            }
                        });
                break;
            }
        }
    }


    // --- Divider animations ---
    private void showDivider() {
        if (selectedUsersDivider.getVisibility() != View.VISIBLE) {
            selectedUsersDivider.setAlpha(0f);
            selectedUsersDivider.setVisibility(View.VISIBLE);
            selectedUsersDivider.animate().alpha(1f).setDuration(200);
        }
    }

    private void hideDivider() {
        if (selectedUsersDivider.getVisibility() == View.VISIBLE) {
            selectedUsersDivider.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> selectedUsersDivider.setVisibility(View.GONE));
        }
    }
}
