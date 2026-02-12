package com.stelandvag.unipicityvibes.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.models.Event;
import com.stelandvag.unipicityvibes.models.Reservation;
import com.stelandvag.unipicityvibes.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends BaseActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    // UI Elements
    private ImageView eventImage;
    private ImageButton backButton;
    private TextView categoryBadge, eventTitle, eventDate, eventTime;
    private TextView eventPrice, eventSeats, eventCapacity;
    private TextView eventVenue, eventDescription, eventId;
    private MaterialButton viewMapButton, bookButton;

    // Data
    private String eventIdString;
    private Event currentEvent;
    private boolean hasBooked = false;

    // Firebase
    private DatabaseReference eventsRef;
    private DatabaseReference reservationsRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Get event ID from intent
        eventIdString = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventIdString == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        eventsRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL)
                .getReference(Constants.EVENTS_REF);
        reservationsRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL)
                .getReference(Constants.RESERVATIONS_REF);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI
        initViews();
        setupListeners();

        // Load data
        loadEventDetails();
        checkExistingReservation();
    }

    private void initViews() {
        eventImage = findViewById(R.id.eventImage);
        backButton = findViewById(R.id.backButton);
        categoryBadge = findViewById(R.id.categoryBadge);
        eventTitle = findViewById(R.id.eventTitle);
        eventDate = findViewById(R.id.eventDate);
        eventTime = findViewById(R.id.eventTime);
        eventPrice = findViewById(R.id.eventPrice);
        eventSeats = findViewById(R.id.eventSeats);
        eventCapacity = findViewById(R.id.eventCapacity);
        eventVenue = findViewById(R.id.eventVenue);
        eventDescription = findViewById(R.id.eventDescription);
        eventId = findViewById(R.id.eventId);
        viewMapButton = findViewById(R.id.viewMapButton);
        bookButton = findViewById(R.id.bookButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        viewMapButton.setOnClickListener(v -> {
            if (currentEvent != null) {
                openInMaps(currentEvent.getLatitude(), currentEvent.getLongitude(),
                        currentEvent.getVenue());
            }
        });

        bookButton.setOnClickListener(v -> {
            if (hasBooked) {
                Toast.makeText(this, "You already have a reservation!", Toast.LENGTH_SHORT).show();
            } else {
                makeReservation();
            }
        });
    }

    private void loadEventDetails() {
        eventsRef.child(eventIdString).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentEvent = snapshot.getValue(Event.class);
                if (currentEvent != null) {
                    currentEvent.setEventId(snapshot.getKey());
                    displayEventDetails();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EventDetailActivity.this,
                        "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayEventDetails() {
        eventTitle.setText(currentEvent.getTitle());
        categoryBadge.setText(currentEvent.getCategory().toUpperCase());
        eventDescription.setText(currentEvent.getDescription());
        eventVenue.setText(currentEvent.getVenue());
        eventPrice.setText(String.format(Locale.getDefault(), "€%.0f", currentEvent.getPrice()));
        eventSeats.setText(currentEvent.getAvailableSeats() + " left");
        eventCapacity.setText("of " + currentEvent.getCapacity());
        eventId.setText("Event Code: #" + currentEvent.getEventId());

        // Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date dateTime = new Date(currentEvent.getDateTime());
        eventDate.setText(dateFormat.format(dateTime));
        eventTime.setText(timeFormat.format(dateTime));

        // Load image
        if (currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentEvent.getImageUrl())
                    .centerCrop()
                    .into(eventImage);
        }

        // Update button state
        updateBookButton();
    }

    private void checkExistingReservation() {
        if (currentUser == null) return;

        reservationsRef.orderByChild("userId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot resSnapshot : snapshot.getChildren()) {
                            Reservation res = resSnapshot.getValue(Reservation.class);
                            if (res != null && res.getEventId().equals(eventIdString)) {
                                hasBooked = true;
                                updateBookButton();
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateBookButton() {
        if (currentEvent == null) return;

        // Check if event is in the past
        boolean isPastEvent = currentEvent.getDateTime() < System.currentTimeMillis();

        if (isPastEvent) {
            bookButton.setText(getResources().getString(R.string.event_ended));
            bookButton.setEnabled(false);
        } else if (hasBooked) {
            bookButton.setText(getResources().getString(R.string.youre_going));
            bookButton.setEnabled(false);
        } else if (currentEvent.getAvailableSeats() <= 0) {
            bookButton.setText(getResources().getString(R.string.sold_out));
            bookButton.setEnabled(false);
        } else {
            bookButton.setText(getResources().getString(R.string.ill_be_there));
            bookButton.setEnabled(true);
        }
    }

    private void makeReservation() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to make a reservation", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent == null || currentEvent.getAvailableSeats() <= 0) {
            Toast.makeText(this, "No seats available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user name from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userName = prefs.getString(Constants.PREF_USER_NAME, "Guest");

        // Create reservation
        String reservationId = reservationsRef.push().getKey();
        Reservation reservation = new Reservation(
                reservationId,
                eventIdString,
                currentUser.getUid(),
                userName,
                System.currentTimeMillis()
        );

        // Save reservation
        reservationsRef.child(reservationId).setValue(reservation)
                .addOnSuccessListener(aVoid -> {
                    // Update booked seats
                    int newBookedSeats = currentEvent.getBookedSeats() + 1;
                    eventsRef.child(eventIdString).child("bookedSeats").setValue(newBookedSeats);

                    hasBooked = true;
                    updateBookButton();
                    Toast.makeText(this, "Reservation confirmed! 🎉", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to make reservation", Toast.LENGTH_SHORT).show();
                });
    }

    private void openInMaps(double lat, double lng, String label) {
        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + label + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");


        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // If Google Maps isn't installed, open it on browser
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }
}