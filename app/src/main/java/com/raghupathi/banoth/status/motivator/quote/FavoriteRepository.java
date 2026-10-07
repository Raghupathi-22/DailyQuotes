package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Favorites are keyed by the stable quote id (API id or SHA-256 content id), never by list position.
 * The quote content is stored alongside so favorites fetched from the internet remain readable offline.
 */
public class FavoriteRepository {
    private static final String KEY = "favorites_list";
    private static final String LEGACY_KEY = "dm_favs";
    private static final String QUOTE_PREFIX = "favorite_quote_";

    public static Set<String> getFavorites(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return new LinkedHashSet<>(prefs.getStringSet(KEY, new HashSet<>()));
    }

    public static boolean isFavorite(Context context, String quoteId) {
        return getFavorites(context).contains(quoteId);
    }

    public static boolean isFavorite(Context context, QuoteRepository.QuoteItem quote) {
        Set<String> favorites = getFavorites(context);
        return favorites.contains(quote.id) || favorites.contains(quote.text);
    }

    public static void addFavorite(Context context, QuoteRepository.QuoteItem quote) {
        Set<String> favorites = getFavorites(context);
        favorites.add(quote.id);
        save(context, favorites, quote, null);
    }

    public static void removeFavorite(Context context, String quoteId) {
        Set<String> favorites = getFavorites(context);
        favorites.remove(quoteId);
        save(context, favorites, null, quoteId);
    }

    public static void removeFavorite(Context context, QuoteRepository.QuoteItem quote) {
        Set<String> favorites = getFavorites(context);
        favorites.remove(quote.id);
        favorites.remove(quote.text);
        save(context, favorites, null, quote.id);
    }

    public static boolean toggleFavorite(Context context, QuoteRepository.QuoteItem quote) {
        Set<String> favorites = getFavorites(context);
        if (favorites.remove(quote.id) | favorites.remove(quote.text)) {
            save(context, favorites, null, quote.id);
            return false;
        }
        favorites.add(quote.id);
        save(context, favorites, quote, null);
        return true;
    }

    public static List<String> getFavoriteQuotes(Context context) {
        return new ArrayList<>(getFavorites(context));
    }

    @Nullable
    public static QuoteRepository.QuoteItem getStoredQuote(Context context, String quoteId) {
        String json = PreferenceManager.getDefaultSharedPreferences(context).getString(QUOTE_PREFIX + quoteId, null);
        if (json == null) return null;
        try {
            JSONObject object = new JSONObject(json);
            String text = object.optString("text", "");
            if (text.isEmpty()) return null;
            return new QuoteRepository.QuoteItem(quoteId, text, object.optString("author", ""),
                    object.optString("categoryId", ""), object.optString("language", ""));
        } catch (Exception exception) {
            return null;
        }
    }

    public static void migrateLegacyFavorites(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(LEGACY_KEY)) return;
        Set<String> current = getFavorites(context);
        Set<String> legacy = prefs.getStringSet(LEGACY_KEY, new HashSet<>());
        if (legacy != null) current.addAll(legacy);
        prefs.edit().putStringSet(KEY, new LinkedHashSet<>(current)).remove(LEGACY_KEY).apply();
    }

    private static void save(Context context, Set<String> favorites,
                             @Nullable QuoteRepository.QuoteItem added, @Nullable String removedId) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putStringSet(KEY, new LinkedHashSet<>(favorites));
        if (added != null) {
            try {
                editor.putString(QUOTE_PREFIX + added.id, new JSONObject()
                        .put("text", added.text)
                        .put("author", added.author)
                        .put("categoryId", added.categoryId)
                        .put("language", added.language)
                        .toString());
            } catch (Exception ignored) {
                // The id is still saved; content falls back to the local catalogue lookup.
            }
        }
        if (removedId != null) editor.remove(QUOTE_PREFIX + removedId);
        editor.apply();
    }
}
