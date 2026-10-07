package com.raghupathi.banoth.status.motivator.quote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * One browsing cycle for a single category + language.
 * The list is de-duplicated and shuffled once when a cycle starts; "Next" only moves the index.
 * Only accessed from the main thread.
 */
public final class QuoteSession {
    public enum Source { API, LOCAL }

    public final String categoryId;
    public final String language;
    private final List<QuoteRepository.QuoteItem> quotes = new ArrayList<>();
    private final Set<String> shownQuoteIds = new HashSet<>();
    private int currentIndex = 0;
    private Source source;
    int apiPage = 0;
    int apiLastPage = 0;

    QuoteSession(@NonNull String categoryId, @NonNull String language) {
        this.categoryId = categoryId;
        this.language = language;
    }

    /** Replaces the list with a de-duplicated, shuffled copy and starts at index 0. */
    void startCycle(@NonNull List<QuoteRepository.QuoteItem> uniqueQuotes, @NonNull Source source, @NonNull Random random) {
        String previousId = current() != null ? current().id : null;
        quotes.clear();
        quotes.addAll(uniqueQuotes);
        Collections.shuffle(quotes, random);
        // Never show the same quote twice in a row across a cycle boundary.
        if (previousId != null && quotes.size() > 1 && quotes.get(0).id.equals(previousId)) {
            Collections.swap(quotes, 0, 1 + random.nextInt(quotes.size() - 1));
        }
        this.source = source;
        currentIndex = 0;
        shownQuoteIds.clear();
        if (!quotes.isEmpty()) shownQuoteIds.add(quotes.get(0).id);
    }

    @Nullable
    public QuoteRepository.QuoteItem current() {
        return currentIndex >= 0 && currentIndex < quotes.size() ? quotes.get(currentIndex) : null;
    }

    /** True when a not-yet-shown quote remains in this cycle. */
    public boolean hasNext() {
        for (int i = currentIndex + 1; i < quotes.size(); i++) {
            if (!shownQuoteIds.contains(quotes.get(i).id)) return true;
        }
        return false;
    }

    /** Moves to the next unseen quote in the shuffled list. Returns null when the cycle is exhausted. */
    @Nullable
    QuoteRepository.QuoteItem advance() {
        for (int i = currentIndex + 1; i < quotes.size(); i++) {
            QuoteRepository.QuoteItem candidate = quotes.get(i);
            if (shownQuoteIds.add(candidate.id)) {
                currentIndex = i;
                return candidate;
            }
        }
        return null;
    }

    /** Skips the current quote if it matches the given id (used when the Daily Quote was just shown). */
    void skipIfCurrent(@Nullable String quoteId) {
        QuoteRepository.QuoteItem current = current();
        if (quoteId != null && current != null && current.id.equals(quoteId) && hasNext()) advance();
    }

    public boolean isEmpty() { return quotes.isEmpty(); }
    public int size() { return quotes.size(); }
    public int getCurrentIndex() { return currentIndex; }
    public Source getSource() { return source; }

    List<QuoteRepository.QuoteItem> snapshot() { return new ArrayList<>(quotes); }
}
