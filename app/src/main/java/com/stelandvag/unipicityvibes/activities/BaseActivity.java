package com.stelandvag.unipicityvibes.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.stelandvag.unipicityvibes.utils.Constants;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // IMPORTANT: Apply settings BEFORE super.attachBaseContext
        super.attachBaseContext(applySettings(newBase));
    }

    private Context applySettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        // Get saved language
        String languageCode = prefs.getString(Constants.PREF_LANGUAGE, "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        // Get saved font size
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

        // Apply configuration
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        config.fontScale = fontScale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        } else {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }

    // Helper method to restart activity with new settings
    public void restartWithSettings() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}