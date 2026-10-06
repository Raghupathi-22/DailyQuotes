package com.raghupathi.banoth.status.motivator.quote;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raghupathi.banoth.status.motivator.quote.databinding.ActivityFavoritesBinding;

public class FavoritesActivity extends AppCompatActivity {
    private ActivityFavoritesBinding binding;
    private QuoteRepository repository;
    private FavoritesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = QuoteRepository.getInstance(this);

        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(this, repository.getFavoriteQuoteItems(), this::refresh);
        binding.recyclerFavorites.setAdapter(adapter);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repository != null) refresh();
    }

    private void refresh() {
        adapter.submitItems(repository.getFavoriteQuoteItems());
        boolean empty = adapter.getItemCount() == 0;
        binding.textEmpty.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.recyclerFavorites.setVisibility(empty ? android.view.View.GONE : android.view.View.VISIBLE);
    }
}
