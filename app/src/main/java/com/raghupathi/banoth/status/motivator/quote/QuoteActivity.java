package com.raghupathi.banoth.status.motivator.quote;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.raghupathi.banoth.status.motivator.quote.databinding.ActivityQuoteBinding;

import java.io.File;
import java.io.IOException;

public class QuoteActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "category_id";

    private ActivityQuoteBinding binding;
    private QuoteRepository repository;
    private QuoteRepository.QuoteItem current;
    private String categoryId;
    private String language;
    private int currentBackgroundRes;
    private boolean loading;

    private final QuoteRepository.QuoteCallback quoteCallback = new QuoteRepository.QuoteCallback() {
        @Override
        public void onQuote(@NonNull QuoteRepository.QuoteItem quote, @NonNull QuoteSession session,
                            @NonNull QuoteRepository.LoadStatus status) {
            if (isFinishing() || isDestroyed()) return;
            // Ignore late results for a category/language that is no longer displayed.
            if (!quote.categoryId.equals(categoryId) || !session.language.equals(language)) return;
            loading = false;
            if (status == QuoteRepository.LoadStatus.OFFLINE_FALLBACK) {
                Toast.makeText(QuoteActivity.this, R.string.offline_quotes_notice, Toast.LENGTH_SHORT).show();
            } else if (status == QuoteRepository.LoadStatus.RESHUFFLED) {
                Toast.makeText(QuoteActivity.this, R.string.new_cycle_notice, Toast.LENGTH_SHORT).show();
            }
            current = quote;
            render();
        }

        @Override
        public void onError(@NonNull String failedCategoryId, @NonNull String failedLanguage) {
            if (isFinishing() || isDestroyed()) return;
            if (!failedCategoryId.equals(categoryId) || !failedLanguage.equals(language)) return;
            loading = false;
            current = null;
            showError();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = QuoteRepository.getInstance(this);
        String requested = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        categoryId = repository.findCategory(requested) != null ? requested : "MOTIVATION";
        language = repository.getLanguage();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNext.setOnClickListener(v -> nextQuote());
        binding.btnRetry.setOnClickListener(v -> loadCategory());
        binding.btnCopy.setOnClickListener(v -> copyQuote());
        binding.btnShare.setOnClickListener(v -> shareText());
        binding.btnShareImage.setOnClickListener(v -> shareImage());
        binding.btnFavorite.setOnClickListener(v -> {
            if (current == null) return;
            FavoriteRepository.toggleFavorite(this, current);
            renderFavorite();
        });

        loadCategory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // A language change invalidates the displayed quotes; reload the same category in the new language.
        String selected = repository.getLanguage();
        if (!selected.equals(language)) {
            language = selected;
            current = null;
            loadCategory();
        }
    }

    private void loadCategory() {
        showLoading(R.string.loading_quotes);
        repository.fetchQuotes(categoryId, language, quoteCallback);
    }

    private void nextQuote() {
        if (loading || current == null) return;
        QuoteSession session = repository.getSession(categoryId, language);
        if (session == null || !session.hasNext()) {
            // End of the cycle: the repository fetches fresh quotes or reshuffles this category.
            showLoading(R.string.refreshing_quotes);
        }
        repository.getNextQuote(categoryId, language, quoteCallback);
    }

    private void showLoading(int messageRes) {
        loading = true;
        binding.textQuote.setVisibility(View.GONE);
        binding.textAuthor.setVisibility(View.GONE);
        binding.stateContainer.setVisibility(View.VISIBLE);
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.textStateTitle.setText(messageRes);
        binding.textStateMessage.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);
        setActionsEnabled(false);
    }

    private void showError() {
        binding.textQuote.setVisibility(View.GONE);
        binding.textAuthor.setVisibility(View.GONE);
        binding.stateContainer.setVisibility(View.VISIBLE);
        binding.progressLoading.setVisibility(View.GONE);
        binding.textStateTitle.setText(R.string.unable_to_load_quotes);
        binding.textStateMessage.setText(R.string.check_internet_connection);
        binding.textStateMessage.setVisibility(View.VISIBLE);
        binding.btnRetry.setVisibility(View.VISIBLE);
        setActionsEnabled(false);
        Category category = repository.findCategory(categoryId);
        if (category != null) {
            currentBackgroundRes = CategoryBackgroundCatalog.getCardBackground(categoryId, category.backgroundRes);
            Glide.with(this).load(currentBackgroundRes).centerCrop().thumbnail(0.25f).into(binding.backgroundImage);
        }
    }

    private void render() {
        binding.stateContainer.setVisibility(View.GONE);
        binding.textQuote.setVisibility(View.VISIBLE);
        binding.textAuthor.setVisibility(View.VISIBLE);
        binding.textQuote.setText(current.text);
        binding.textAuthor.setText(current.author.isEmpty() ? "" : "— " + current.author);
        int background = repository.getBackgroundRes(current);
        if (background != currentBackgroundRes) {
            currentBackgroundRes = background;
            Glide.with(this).load(currentBackgroundRes).centerCrop().thumbnail(0.25f).into(binding.backgroundImage);
        }
        setActionsEnabled(true);
        renderFavorite();
    }

    private void setActionsEnabled(boolean enabled) {
        binding.btnNext.setEnabled(enabled);
        binding.btnFavorite.setEnabled(enabled);
        binding.btnShareImage.setEnabled(enabled);
        binding.btnCopy.setEnabled(enabled);
        binding.btnShare.setEnabled(enabled);
    }

    private void renderFavorite() {
        if (current == null) return;
        boolean favorite = FavoriteRepository.isFavorite(this, current);
        binding.btnFavorite.setIconResource(favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        binding.btnFavorite.setContentDescription(getString(favorite ? R.string.remove_favorite : R.string.favorite));
    }

    private void copyQuote() {
        if (current == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("quote", current.text));
        Toast.makeText(this, R.string.copy, Toast.LENGTH_SHORT).show();
    }

    private void shareText() {
        if (current == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, current.author.isEmpty() ? current.text : current.text + "\n\n— " + current.author);
        startActivity(Intent.createChooser(share, getString(R.string.share)));
    }

    // The 1080x1920 PNG is generated only when the user shares it, not on every Next.
    private void shareImage() {
        if (current == null) return;
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
