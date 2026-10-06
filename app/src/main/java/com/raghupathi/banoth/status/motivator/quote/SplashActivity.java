package com.raghupathi.banoth.status.motivator.quote;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        boolean languageChosen = PreferenceManager.getDefaultSharedPreferences(this)
                .contains(QuoteRepository.PREF_LANGUAGE);
        startActivity(new Intent(this, languageChosen ? MainActivity.class : LanguageSelectActivity.class));
        finish();
    }
}
