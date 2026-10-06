package com.raghupathi.banoth.status.motivator.quote;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.raghupathi.banoth.status.motivator.quote.databinding.ActivityQuoteBinding;

import java.io.File;
import java.io.IOException;

public class QuoteActivity extends AppCompatActivity {
    private ActivityQuoteBinding binding;
    private QuoteRepository repository;
    private QuoteRepository.QuoteItem current;
    private String categoryId = "MOTIVATION";
    private int currentBackgroundRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = QuoteRepository.getInstance(this);
        categoryId = getIntent().getStringExtra("category_id") == null ? "MOTIVATION" : getIntent().getStringExtra("category_id");
        current = repository.getDailyQuote(categoryId);
        render();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNext.setOnClickListener(v -> {
            current = repository.getRandomQuote(categoryId, current.id);
            render();
        });
        binding.btnCopy.setOnClickListener(v -> copyQuote());
        binding.btnShare.setOnClickListener(v -> shareText());
        binding.btnShareImage.setOnClickListener(v -> shareImage());
        binding.btnFavorite.setOnClickListener(v -> {
            FavoriteRepository.toggleFavorite(this, current);
            renderFavorite();
        });
    }

    private void render() {
        binding.textQuote.setText(current.text.isEmpty() ? getString(R.string.no_quotes_available) : current.text);
        binding.textAuthor.setText(current.author == null || current.author.isEmpty() ? "" : "— " + current.author);
        currentBackgroundRes = repository.getBackgroundRes(current);
        Glide.with(this).load(currentBackgroundRes)
                .centerCrop().thumbnail(0.25f).into(binding.backgroundImage);
        renderFavorite();
    }

    private void renderFavorite() {
        boolean favorite = FavoriteRepository.isFavorite(this, current);
        binding.btnFavorite.setIconResource(favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        binding.btnFavorite.setContentDescription(getString(favorite ? R.string.remove_favorite : R.string.favorite));
    }

    private void copyQuote() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("quote", current.text));
        Toast.makeText(this, R.string.copy, Toast.LENGTH_SHORT).show();
    }

    private void shareText() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, current.author == null || current.author.isEmpty() ? current.text : current.text + "\n\n— " + current.author);
        startActivity(Intent.createChooser(share, getString(R.string.share)));
    }

    private void shareImage() {
        try {
            File image = QuoteImageGenerator.generate(this, current, currentBackgroundRes);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", image));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.share_image)));
        } catch (IOException exception) {
            Toast.makeText(this, R.string.share_image_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
