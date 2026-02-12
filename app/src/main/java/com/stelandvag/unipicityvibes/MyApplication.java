package com.stelandvag.unipicityvibes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import com.stelandvag.unipicityvibes.utils.Constants;

import java.util.Locale;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        // Apply saved locale BEFORE the base context is attached
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(Constants.PREF_LANGUAGE, "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Load and apply saved theme
        boolean isDarkTheme = prefs.getBoolean(Constants.PREF_DARK_THEME, false);
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Apply font size globally
        applyFontSize();
    }

    private void applyFontSize() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String fontSize = prefs.getString(Constants.PREF_FONT_SIZE, "medium");
        float fontScale = 1.0f;

        switch (fontSize) {
            case "small":
                fontScale = 0.85f;
                break;
            case "large":
                fontScale = 1.2f;
                break;
            default:
                fontScale = 1.0f;
                break;
        }

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.fontScale = fontScale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}