package com.stelandvag.unipicityvibes.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.adapters.CalendarEventAdapter;
import com.stelandvag.unipicityvibes.models.Event;
import com.stelandvag.unipicityvibes.utils.Constants;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends BaseActivity implements CalendarEventAdapter.OnEventClickListener {

    // UI Elements
    private ImageButton backButton, prevMonthButton, nextMonthButton;
    private TextView monthYearText, selectedDateText;
    private CalendarView calendarView;
    private RecyclerView eventsRecyclerView;
    private LinearLayout emptyState;

    // Data
    private List<Event> allEvents = new ArrayList<>();
    private List<Event> eventsOnSelectedDate = new ArrayList<>();
    private Set<LocalDate> datesWithEvents = new HashSet<>();
    private LocalDate selectedDate = LocalDate.now();
    private YearMonth currentMonth = YearMonth.now();

    // Adapter
    private CalendarEventAdapter adapter;

    // Firebase
    private DatabaseReference eventsRef;

    // Formatter
    private DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize Firebase
        eventsRef = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL)
                .getReference(Constants.EVENTS_REF);

        // Initialize UI
        initViews();
        setupRecyclerView();
        setupCalendar();
        setupListeners();

        // Update UI
        updateMonthYearText();
        updateSelectedDateText();

        // Load events
        loadEvents();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        monthYearText = findViewById(R.id.monthYearText);
        selectedDateText = findViewById(R.id.selectedDateText);
        calendarView = findViewById(R.id.calendarView);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CalendarEventAdapter(this, eventsOnSelectedDate, this);
        eventsRecyclerView.setAdapter(adapter);
    }

    private void setupCalendar() {
        // Setup day binder
        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, CalendarDay day) {
                container.day = day;
                TextView dayText = container.dayText;
                View eventDot = container.eventDot;

                dayText.setText(String.valueOf(day.getDate().getDayOfMonth()));

                if (day.getPosition() == DayPosition.MonthDate) {
                    // Current month
                    dayText.setVisibility(View.VISIBLE);

                    // Check if this date has events
                    if (datesWithEvents.contains(day.getDate())) {
                        eventDot.setVisibility(View.VISIBLE);
                    } else {
                        eventDot.setVisibility(View.INVISIBLE);
                    }

                    // Highlight selected date
                    if (day.getDate().equals(selectedDate)) {
                        dayText.setBackgroundResource(R.drawable.selected_day_background);
                        dayText.setTextColor(getResources().getColor(R.color.white, null));
                    } else {
                        dayText.setBackground(null);
                        dayText.setTextColor(getResources().getColor(R.color.black, null));
                    }
                } else {
                    // Other months
                    dayText.setVisibility(View.INVISIBLE);
                    eventDot.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Setup calendar range (6 months back to 6 months forward)
        YearMonth startMonth = currentMonth.minusMonths(6);
        YearMonth endMonth = currentMonth.plusMonths(6);
        calendarView.setup(startMonth, endMonth, DayOfWeek.MONDAY);
        calendarView.scrollToMonth(currentMonth);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        prevMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            calendarView.scrollToMonth(currentMonth);
            updateMonthYearText();
        });

        nextMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            calendarView.scrollToMonth(currentMonth);
            updateMonthYearText();
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            updateMonthYearText();
            return kotlin.Unit.INSTANCE;
        });
    }

    private void updateMonthYearText() {
        monthYearText.setText(currentMonth.format(monthFormatter));
    }

    private void updateSelectedDateText() {
        selectedDateText.setText("Events on " + selectedDate.format(dateFormatter));
    }

    private void loadEvents() {
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allEvents.clear();
                datesWithEvents.clear();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        event.setEventId(eventSnapshot.getKey());
                        allEvents.add(event);

                        // Add date to set
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(event.getDateTime());
                        LocalDate eventDate = LocalDate.of(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                        );
                        datesWithEvents.add(eventDate);
                    }
                }

                // Refresh calendar to show dots
                calendarView.notifyCalendarChanged();
                filterEventsForSelectedDate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CalendarActivity.this,
                        "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterEventsForSelectedDate() {
        eventsOnSelectedDate.clear();

        for (Event event : allEvents) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getDateTime());
            LocalDate eventDate = LocalDate.of(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
            );

            if (eventDate.equals(selectedDate)) {
                eventsOnSelectedDate.add(event);
            }
        }

        // Sort by time
        Collections.sort(eventsOnSelectedDate, (e1, e2) ->
                Long.compare(e1.getDateTime(), e2.getDateTime()));

        // Update UI
        adapter.notifyDataSetChanged();

        if (eventsOnSelectedDate.isEmpty()) {
            eventsRecyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            eventsRecyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    // ViewContainer for calendar days
    class DayViewContainer extends ViewContainer {
        CalendarDay day;
        TextView dayText;
        View eventDot;

        public DayViewContainer(@NonNull View view) {
            super(view);
            dayText = view.findViewById(R.id.dayText);
            eventDot = view.findViewById(R.id.eventDot);

            view.setOnClickListener(v -> {
                if (day.getPosition() == DayPosition.MonthDate) {
                    LocalDate oldDate = selectedDate;
                    selectedDate = day.getDate();
                    calendarView.notifyDateChanged(day.getDate());
                    if (oldDate != null) {
                        calendarView.notifyDateChanged(oldDate);
                    }
                    updateSelectedDateText();
                    filterEventsForSelectedDate();
                }
            });
        }
    }
}