package com.example.comp3330_hkunite;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    public static final String PREF_NAME = "HKUnitePrefs";

    //server and login
    private static final String TAG = "LoginActivity";
    //server address from EventDetailActivity:
//    private static final String BASE_URL = "http://10.70.208.59:5001";
    private static final String BASE_URL = "http://10.70.170.80:5001";

    //ui stuff:
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView signUpText;

    //volley logic:
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(this); //initialize volley

        emailEditText = findViewById(R.id.login_email); //initialize ui
        passwordEditText = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signUpText = findViewById(R.id.login_signup_text);

        loginButton.setOnClickListener(new View.OnClickListener() { //listener for login button
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) { //simple validation for now
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    //this is the REAL login
                    performLogin(email, password);
                }
            }
        });

        // listener for Sign Up text
        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: You will create SignUpActivity later --> yeah, I'll see about this
                // Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                // startActivity(intent);

                Toast.makeText(LoginActivity.this, "Sign Up page not yet implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //network request to log the user in:
    private void performLogin(String email, String password) {
        String url = BASE_URL + "/login";

        //create JSON object to send
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error", e);
            return;
        }

        //volley request:
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    //if successful:
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            //login successful!!
                            JSONObject user = response.getJSONObject("user");
                            int uid = user.getInt("uid");
                            String name = user.getString("name");

                            Log.i(TAG, "Login successful for user: " + name + " (uid: " + uid + ")");

                            //saving the uid of user and name in SharedPreferences
                            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                            editor.putInt("USER_ID", uid);
                            editor.putString("USER_NAME", name);
                            editor.apply();

                            //going to main activity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish(); //close login activity

                        } else {
                            //success false from server
                            String message = response.optString("message", "Invalid credentials");
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error on success", e);
                    }
                },
                error -> {
                    //network error :(
                    Log.e(TAG, "Volley error: " + error.toString());
                    Toast.makeText(LoginActivity.this, "Login failed: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        );
        queue.add(request); //add request to queue
    }
}