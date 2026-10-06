package com.raghupathi.banoth.status.motivator.quote;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.raghupathi.banoth.status.motivator.quote.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private QuoteRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = QuoteRepository.getInstance(this);
        FavoriteRepository.migrateLegacyFavorites(this);

        binding.recyclerCategories.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerCategories.setAdapter(new MoodRecyclerAdapter(this, repository.getCategories(), category -> openQuote(category.id)));

        binding.textDailyQuote.setText(repository.getDailyQuote("MOTIVATION").text);
        binding.textSelectedLanguage.setText(getLanguageName(repository.getLanguage()));
        binding.btnOpenDailyQuote.setOnClickListener(v -> openQuote("MOTIVATION"));
        binding.btnFavorites.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));
    }

    private void openQuote(String categoryId) {
        Intent intent = new Intent(this, QuoteActivity.class);
        intent.putExtra("category_id", categoryId);
        startActivity(intent);
    }

    private int getLanguageName(String language) {
        if ("te".equals(language)) return R.string.telugu;
        if ("hi".equals(language)) return R.string.hindi;
        if ("ta".equals(language)) return R.string.tamil;
        return R.string.english;
    }
}
