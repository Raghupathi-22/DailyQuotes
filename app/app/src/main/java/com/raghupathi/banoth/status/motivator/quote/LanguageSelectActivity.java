package com.raghupathi.banoth.status.motivator.quote;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class LanguageSelectActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_select);
        findViewById(R.id.btn_en).setOnClickListener(v -> selectLanguage("en"));
        findViewById(R.id.btn_te).setOnClickListener(v -> selectLanguage("te"));
        findViewById(R.id.btn_hi).setOnClickListener(v -> selectLanguage("hi"));
        findViewById(R.id.btn_ta).setOnClickListener(v -> selectLanguage("ta"));
    }

    private void selectLanguage(String language) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(QuoteRepository.PREF_LANGUAGE, language)
                .apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
