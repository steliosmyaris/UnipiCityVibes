package com.stelandvag.unipicityvibes.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    // UI Elements
    private TabLayout tabLayout;
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameEditText, emailEditText, passwordEditText;
    private MaterialButton authButton;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    // State
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference(Constants.USERS_REF);

        // Initialize UI
        initViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        }
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        authButton = findViewById(R.id.authButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Tab switching between Login and Sign Up
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLoginMode = (tab.getPosition() == 0);
                updateUI();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Auth button click
        authButton.setOnClickListener(v -> {
            if (isLoginMode) {
                loginUser();
            } else {
                registerUser();
            }
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            nameInputLayout.setVisibility(View.GONE);
            authButton.setText("Login");
        } else {
            nameInputLayout.setVisibility(View.VISIBLE);
            authButton.setText("Sign Up");
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Fetch user data from database and save to prefs
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserDataAndNavigate(user.getUid(), email);
                        } else {
                            showLoading(false);
                            navigateToMain();
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save user data to database
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), name, email);
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());

        usersRef.child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Save to SharedPreferences
                        saveUserToPrefs(name, email);
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToPrefs(String name, String email) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_USER_NAME, name)
                .putString(Constants.PREF_USER_EMAIL, email)
                .apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        authButton.setEnabled(!show);
    }

    private void fetchUserDataAndNavigate(String userId, String email) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                String name = "Guest"; // Default fallback
                if (snapshot.exists() && snapshot.child("name").getValue() != null) {
                    name = snapshot.child("name").getValue(String.class);
                }

                // Save to SharedPreferences
                saveUserToPrefs(name, email);

                Toast.makeText(LoginActivity.this, "Welcome back, " + name + "!",
                        Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                // Still navigate even if fetch fails
                saveUserToPrefs("Guest", email);
                navigateToMain();
            }
        });
    }
}