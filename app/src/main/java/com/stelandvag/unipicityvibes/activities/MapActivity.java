package com.stelandvag.unipicityvibes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.models.Event;
import com.stelandvag.unipicityvibes.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    // UI Elements
    private ImageButton backButton;
    private GoogleMap mMap;

    // Data
    private List<Event> allEvents = new ArrayList<>();
    private Map<String, Event> markerEventMap = new HashMap<>();

    // Firebase
    private DatabaseReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize Firebase
        eventsRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL)
                .getReference(Constants.EVENTS_REF);

        // Initialize UI
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Setup map UI settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Setup marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // Setup info window click listener
        mMap.setOnInfoWindowClickListener(marker -> {
            Event event = markerEventMap.get(marker.getId());
            if (event != null) {
                Intent intent = new Intent(MapActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
                startActivity(intent);
            }
        });

        // Load events
        loadEvents();
    }

    private void loadEvents() {
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                addMarkersToMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this,
                        "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarkersToMap() {
        if (mMap == null || allEvents.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Event event : allEvents) {
            LatLng position = new LatLng(event.getLatitude(), event.getLongitude());

            // Choose marker color based on category
            float markerColor = getMarkerColor(event.getCategory());

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(event.getTitle())
                    .snippet(event.getVenue() + " • " + (int) event.getPrice()+ " €")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                markerEventMap.put(marker.getId(), event);
            }

            boundsBuilder.include(position);
        }

        // Move camera to show all markers if possible

        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // is pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {


            // If only one marker or error, zoom to default location
            if (!allEvents.isEmpty()) {
                Event first = allEvents.get(0);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(first.getLatitude(), first.getLongitude()), 12f));
            }
        }
    }

    private float getMarkerColor(String category) {
        switch (category) {
            case Constants.CATEGORY_THEATER:
                return BitmapDescriptorFactory.HUE_VIOLET;
            case Constants.CATEGORY_CINEMA:
                return BitmapDescriptorFactory.HUE_BLUE;
            case Constants.CATEGORY_CONCERT:
                return BitmapDescriptorFactory.HUE_ORANGE;
            case Constants.CATEGORY_SPORTS:
                return BitmapDescriptorFactory.HUE_GREEN;
            case Constants.CATEGORY_EXHIBITION:
                return BitmapDescriptorFactory.HUE_YELLOW;
            case Constants.CATEGORY_FESTIVAL:
                return BitmapDescriptorFactory.HUE_ROSE;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }
}