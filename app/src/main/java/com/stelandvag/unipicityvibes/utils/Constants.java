package com.stelandvag.unipicityvibes.utils;

public class Constants {
    // Firebase Database References
    public static final String EVENTS_REF = "events";
    public static final String RESERVATIONS_REF = "reservations";
    public static final String USERS_REF = "users";
    // Firebase Database URL
    public static final String FIREBASE_DB_URL = "https://myunipicityvibes-default-rtdb.europe-west1.firebasedatabase.app";

    // SharedPreferences
    public static final String PREFS_NAME = "UnipiCityVibePrefs";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_DARK_THEME = "dark_theme";
    public static final String PREF_FONT_SIZE = "font_size";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PREF_LANGUAGE = "language";

    // Location
    public static final int PROXIMITY_RADIUS_METERS = 2000;
    public static final int LOCATION_UPDATE_INTERVAL = 10000;
    public static final int LOCATION_FASTEST_INTERVAL = 5000;

    // Event Categories
    public static final String CATEGORY_THEATER = "theater";
    public static final String CATEGORY_CINEMA = "cinema";
    public static final String CATEGORY_CONCERT = "concert";
    public static final String CATEGORY_SPORTS = "sports";
    public static final String CATEGORY_EXHIBITION = "exhibition";
    public static final String CATEGORY_FESTIVAL = "festival";
}