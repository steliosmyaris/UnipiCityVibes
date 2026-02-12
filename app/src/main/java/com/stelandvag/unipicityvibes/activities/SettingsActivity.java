package com.stelandvag.unipicityvibes.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.utils.Constants;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    // UI Elements
    private ImageButton backButton;
    private TextInputEditText nameEditText, emailEditText;
    private MaterialButton saveProfileButton, signOutButton;
    private SwitchMaterial darkThemeSwitch, notificationsSwitch;
    private RadioGroup fontSizeRadioGroup;
    private RadioButton fontSmall, fontMedium, fontLarge;
    private Spinner languageSpinner;

    // SharedPreferences
    private SharedPreferences prefs;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    // Languages
    private String[] languageNames = {"English", "Ελληνικά", "Español"};
    private String[] languageCodes = {"en", "el", "es"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL)
                .getReference(Constants.USERS_REF);

        // Initialize UI
        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        signOutButton = findViewById(R.id.signOutButton);
        darkThemeSwitch = findViewById(R.id.darkThemeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        fontSizeRadioGroup = findViewById(R.id.fontSizeRadioGroup);
        fontSmall = findViewById(R.id.fontSmall);
        fontMedium = findViewById(R.id.fontMedium);
        fontLarge = findViewById(R.id.fontLarge);
        languageSpinner = findViewById(R.id.languageSpinner);

        // Setup the language spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
    }

    private void loadCurrentSettings() {
        // Load profile
        String name = prefs.getString(Constants.PREF_USER_NAME, "");
        String email = prefs.getString(Constants.PREF_USER_EMAIL, "");
        nameEditText.setText(name);
        emailEditText.setText(email);

        // Load dark theme
        boolean isDarkTheme = prefs.getBoolean(Constants.PREF_DARK_THEME, false);
        darkThemeSwitch.setChecked(isDarkTheme);

        // Load notifications
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true);
        notificationsSwitch.setChecked(notificationsEnabled);

        // Load font size
        String fontSize = prefs.getString(Constants.PREF_FONT_SIZE, "medium");
        switch (fontSize) {
            case "small":
                fontSmall.setChecked(true);
                break;
            case "large":
                fontLarge.setChecked(true);
                break;
            default:
                fontMedium.setChecked(true);
                break;
        }

        // Load language
        String currentLanguage = prefs.getString(Constants.PREF_LANGUAGE, "en");
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                languageSpinner.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        saveProfileButton.setOnClickListener(v -> saveProfile());

        darkThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.PREF_DARK_THEME, isChecked).apply();

            // Apply theme
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        fontSizeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String fontSize = "medium";
            if (checkedId == R.id.fontSmall) {
                fontSize = "small";
            } else if (checkedId == R.id.fontLarge) {
                fontSize = "large";
            }

            String currentSize = prefs.getString(Constants.PREF_FONT_SIZE, "medium");
            if (!fontSize.equals(currentSize)) {
                prefs.edit().putString(Constants.PREF_FONT_SIZE, fontSize).apply();
                Toast.makeText(this, "Restarting to apply font size...", Toast.LENGTH_SHORT).show();

                // Give SharedPreferences time to save before restarting
                new Handler().postDelayed(() -> restartApp(), 300);
            }
        });

        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            private boolean isFirstSelection = true;
            private String previousLanguage;

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (isFirstSelection) {
                    isFirstSelection = false;
                    previousLanguage = prefs.getString(Constants.PREF_LANGUAGE, "en");
                    return;
                }

                String selectedLanguage = languageCodes[position];
                String currentLanguage = prefs.getString(Constants.PREF_LANGUAGE, "en");

                if (!selectedLanguage.equals(previousLanguage)) {
                    prefs.edit().putString(Constants.PREF_LANGUAGE, selectedLanguage).apply();
                    Toast.makeText(SettingsActivity.this, "Restarting to apply language...", Toast.LENGTH_SHORT).show();
                    previousLanguage = selectedLanguage;

                    // Give SharedPreferences time to save before restarting
                    new Handler().postDelayed(() -> restartApp(), 300);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        signOutButton.setOnClickListener(v -> showSignOutDialog());
    }

    private void saveProfile() {
        String name = nameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            return;
        }

        // Save to SharedPreferences
        prefs.edit().putString(Constants.PREF_USER_NAME, name).apply();

        // Save to Firebase
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            usersRef.child(userId).child("name").setValue(name)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Restart activity to apply changes
        Toast.makeText(this, "Language changed. Restart app to apply.", Toast.LENGTH_LONG).show();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("@string/sign_out_confirm")
                .setPositiveButton("@string/sign_out", (dialog, which) -> signOut())
                .setNegativeButton("@string/cancel", null)
                .show();
    }

    private void signOut() {
        // Delete collected SharedPreferences
        prefs.edit().clear().apply();

        // Sign out from Firebase
        mAuth.signOut();

        // Navigate to Login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // In SettingsActivity.java, add this method:
    private void restartApp() {
        // Save all pending preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.apply(); // Force async write

        // Wait a bit for preferences to save, then restart
        new Handler().postDelayed(() -> {
            // Recreate current activity to apply changes
            recreate();

            // Then restart the entire app
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finishAffinity();
            }, 100);
        }, 100);
    }
}