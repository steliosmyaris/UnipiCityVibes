package com.stelandvag.unipicityvibes.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.adapters.EventAdapter;
import com.stelandvag.unipicityvibes.models.Event;
import com.stelandvag.unipicityvibes.utils.Constants;
import com.stelandvag.unipicityvibes.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends BaseActivity implements EventAdapter.OnEventClickListener {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Elements
    private ImageButton settingsButton;
    private MaterialButton filterButton, mapButton, calendarButton, searchButton;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private RecyclerView trendingRecyclerView, nearYouRecyclerView;
    private TextView locationPermissionText;
    private RecyclerView allEventsRecyclerView;
    private EventAdapter allEventsAdapter;
    private List<Event> allEventsList = new ArrayList<>();

    // Notificatioans
    private NotificationHelper notificationHelper;

    // Adapters
    private EventAdapter trendingAdapter, nearYouAdapter;

    // Data in array
    private List<Event> allEvents = new ArrayList<>();
    private List<Event> trendingEvents = new ArrayList<>();
    private List<Event> nearYouEvents = new ArrayList<>();

    // Firebase
    private DatabaseReference eventsRef;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Filter
    private Set<String> selectedCategories = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eventsRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference(Constants.EVENTS_REF);

        // Initialize Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI
        initViews();
        setupRecyclerViews();
        setupListeners();

        //Initialize filters
        initFilters();

        //Load data
        loadEvents();
        onResume();

        // Initialize Notification
        notificationHelper = new NotificationHelper(this);
        checkNotificationPermission();
    }

    private void initViews() {
        settingsButton = findViewById(R.id.settingsButton);
        filterButton = findViewById(R.id.filterButton);
        mapButton = findViewById(R.id.mapButton);
        calendarButton = findViewById(R.id.calendarButton);
        searchButton = findViewById(R.id.searchButton);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        searchEditText = findViewById(R.id.searchEditText);
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView);
        nearYouRecyclerView = findViewById(R.id.nearYouRecyclerView);
        locationPermissionText = findViewById(R.id.locationPermissionText);
        allEventsRecyclerView = findViewById(R.id.allEventsRecyclerView);
    }

    private void initFilters() {
        selectedCategories.add(Constants.CATEGORY_THEATER);
        selectedCategories.add(Constants.CATEGORY_CINEMA);
        selectedCategories.add(Constants.CATEGORY_CONCERT);
        selectedCategories.add(Constants.CATEGORY_SPORTS);
        selectedCategories.add(Constants.CATEGORY_EXHIBITION);
        selectedCategories.add(Constants.CATEGORY_FESTIVAL);
    }

    private void setupRecyclerViews() {
        // Trending (Horizontal scroll)
        trendingRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new EventAdapter(this, trendingEvents, this);
        trendingRecyclerView.setAdapter(trendingAdapter);

        // Near You (Horizontal scroll
        nearYouRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        nearYouAdapter = new EventAdapter(this, nearYouEvents, this);
        nearYouRecyclerView.setAdapter(nearYouAdapter);

        // All Events - Horizontal scroll
        allEventsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        allEventsAdapter = new EventAdapter(this, allEventsList, this);
        allEventsRecyclerView.setAdapter(allEventsAdapter);
    }

    private void setupListeners() {
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        filterButton.setOnClickListener(v -> showFilterDialog());

        mapButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        });

        calendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarActivity.class);
            startActivity(intent);
        });

        searchButton.setOnClickListener(v -> toggleSearch());

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterEvents(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void toggleSearch() {
        if (searchInputLayout.getVisibility() == View.GONE) {
            searchInputLayout.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
        } else {
            searchInputLayout.setVisibility(View.GONE);
            searchEditText.setText("");
            filterEvents("");
        }
    }

    private void loadEvents() {
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventId(eventSnapshot.getKey());
                        allEvents.add(event);
                    }
                }
                updateTrendingList();
                updateNearYouList();
                updateAllEventsList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Failed to load events: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTrendingList() {
        trendingEvents.clear();

        long now = System.currentTimeMillis();

        // Filter by selected categories AND only future events
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            if (selectedCategories.contains(event.getCategory()) && event.getDateTime() >= now) {
                filtered.add(event);
            }
        }

        // Sort by booked seats (most popular first)
        Collections.sort(filtered, (e1, e2) ->
                Integer.compare(e2.getBookedSeats(), e1.getBookedSeats()));

        // Take top 10
        int limit = Math.min(filtered.size(), 10);
        for (int i = 0; i < limit; i++) {
            trendingEvents.add(filtered.get(i));
        }

        trendingAdapter.notifyDataSetChanged();
    }

    private void updateNearYouList() {
        nearYouEvents.clear();

        if (currentLocation == null) {
            locationPermissionText.setVisibility(View.VISIBLE);
            nearYouRecyclerView.setVisibility(View.GONE);
            return;
        }

        locationPermissionText.setVisibility(View.GONE);
        nearYouRecyclerView.setVisibility(View.VISIBLE);

        long now = System.currentTimeMillis();

        // Calculate distance and filter nearby events
        for (Event event : allEvents) {
            // Check if category is selected
            if (!selectedCategories.contains(event.getCategory())) {
                continue;
            }

            // Skip past events
            if (event.getDateTime() < now) {
                continue;
            }

            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    event.getLatitude(),
                    event.getLongitude(),
                    results
            );
            float distanceInMeters = results[0];

            // Add events within 5km for "Near You" section
            if (distanceInMeters <= 5000) {
                nearYouEvents.add(event);
            }

            // Trigger notification if within proximity radius (200 meters)
            if (distanceInMeters <= Constants.PROXIMITY_RADIUS_METERS) {
                notificationHelper.showNearbyEventNotification(event, distanceInMeters);
            }
        }

        // Sort by distance (closest first)
        Collections.sort(nearYouEvents, (e1, e2) -> {
            float[] r1 = new float[1];
            float[] r2 = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    e1.getLatitude(), e1.getLongitude(), r1);
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    e2.getLatitude(), e2.getLongitude(), r2);
            return Float.compare(r1[0], r2[0]);
        });

        nearYouAdapter.notifyDataSetChanged();

        if (nearYouEvents.isEmpty()) {
            locationPermissionText.setText(getString(R.string.no_events_nearby));
            locationPermissionText.setVisibility(View.VISIBLE);
        }
    }

    private void filterEvents(String query) {
        if (query.isEmpty()) {
            updateTrendingList();
            updateNearYouList();
            updateAllEventsList();
            return;
        }

        String lowerQuery = query.toLowerCase();

        // Filter trending
        trendingEvents.clear();
        for (Event event : allEvents) {
            if (event.getTitle().toLowerCase().contains(lowerQuery)) {
                trendingEvents.add(event);
            }
        }
        trendingAdapter.notifyDataSetChanged();

        // Filter near you
        List<Event> filteredNearYou = new ArrayList<>();
        for (Event event : nearYouEvents) {
            if (event.getTitle().toLowerCase().contains(lowerQuery)) {
                filteredNearYou.add(event);
            }
        }
        nearYouEvents.clear();
        nearYouEvents.addAll(filteredNearYou);
        nearYouAdapter.notifyDataSetChanged();

        // Filter all events
        allEventsList.clear();
        for (Event event : allEvents) {
            if (event.getTitle().toLowerCase().contains(lowerQuery)) {
                allEventsList.add(event);
            }
        }
        allEventsAdapter.notifyDataSetChanged();
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        CheckBox checkTheater = dialogView.findViewById(R.id.checkTheater);
        CheckBox checkCinema = dialogView.findViewById(R.id.checkCinema);
        CheckBox checkConcert = dialogView.findViewById(R.id.checkConcert);
        CheckBox checkSports = dialogView.findViewById(R.id.checkSports);
        CheckBox checkExhibition = dialogView.findViewById(R.id.checkExhibition);
        CheckBox checkFestival = dialogView.findViewById(R.id.checkFestival);

        // Set current state
        checkTheater.setChecked(selectedCategories.contains(Constants.CATEGORY_THEATER));
        checkCinema.setChecked(selectedCategories.contains(Constants.CATEGORY_CINEMA));
        checkConcert.setChecked(selectedCategories.contains(Constants.CATEGORY_CONCERT));
        checkSports.setChecked(selectedCategories.contains(Constants.CATEGORY_SPORTS));
        checkExhibition.setChecked(selectedCategories.contains(Constants.CATEGORY_EXHIBITION));
        checkFestival.setChecked(selectedCategories.contains(Constants.CATEGORY_FESTIVAL));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    // Update selected categories
                    selectedCategories.clear();
                    if (checkTheater.isChecked()) selectedCategories.add(Constants.CATEGORY_THEATER);
                    if (checkCinema.isChecked()) selectedCategories.add(Constants.CATEGORY_CINEMA);
                    if (checkConcert.isChecked()) selectedCategories.add(Constants.CATEGORY_CONCERT);
                    if (checkSports.isChecked()) selectedCategories.add(Constants.CATEGORY_SPORTS);
                    if (checkExhibition.isChecked()) selectedCategories.add(Constants.CATEGORY_EXHIBITION);
                    if (checkFestival.isChecked()) selectedCategories.add(Constants.CATEGORY_FESTIVAL);

                    // Refresh lists with filter
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Select All", (dialog, which) -> {
                    selectedCategories.clear();
                    selectedCategories.add(Constants.CATEGORY_THEATER);
                    selectedCategories.add(Constants.CATEGORY_CINEMA);
                    selectedCategories.add(Constants.CATEGORY_CONCERT);
                    selectedCategories.add(Constants.CATEGORY_SPORTS);
                    selectedCategories.add(Constants.CATEGORY_EXHIBITION);
                    selectedCategories.add(Constants.CATEGORY_FESTIVAL);
                    applyFilters();
                })
                .show();
    }

    private void applyFilters() {
        updateTrendingList();
        updateNearYouList();
        updateAllEventsList();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Request new location
        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                updateNearYouList();
            } else {
                // Fallback to last known location
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, lastLocation -> {
                            if (lastLocation != null) {
                                currentLocation = lastLocation;
                                updateNearYouList();
                            } else {
                                locationPermissionText.setText(getString(R.string.enable_location));
                                locationPermissionText.setVisibility(View.VISIBLE);
                                nearYouRecyclerView.setVisibility(View.GONE);
                            }
                        });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                locationPermissionText.setText("Location permission denied");
                locationPermissionText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    private void updateAllEventsList() {
        allEventsList.clear();

        // Filter by selected categories and sort by date (upcoming first)
        List<Event> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Event event : allEvents) {
            if (selectedCategories.contains(event.getCategory()) && event.getDateTime() >= now) {
                filtered.add(event);
            }
        }

        // Sort by date (soonest first)
        Collections.sort(filtered, (e1, e2) ->
                Long.compare(e1.getDateTime(), e2.getDateTime()));

        allEventsList.addAll(filtered);
        allEventsAdapter.notifyDataSetChanged();
    }

    // Ask for permissions
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1002);
            }
        }
    }

    // Location Grab

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermission();
    }
}