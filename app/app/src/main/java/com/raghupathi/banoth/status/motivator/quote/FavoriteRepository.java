package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FavoriteRepository {
    private static final String KEY = "favorites_list";
    private static final String LEGACY_KEY = "dm_favs";

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

    public static void addFavorite(Context context, String quoteId) {
        Set<String> favorites = getFavorites(context);
        favorites.add(quoteId);
        save(context, favorites);
    }

    public static void removeFavorite(Context context, String quoteId) {
        Set<String> favorites = getFavorites(context);
        favorites.remove(quoteId);
        save(context, favorites);
    }

    public static void removeFavorite(Context context, QuoteRepository.QuoteItem quote) {
        Set<String> favorites = getFavorites(context);
        favorites.remove(quote.id);
        favorites.remove(quote.text);
        save(context, favorites);
    }

    public static boolean toggleFavorite(Context context, String quoteId) {
        Set<String> favorites = getFavorites(context);
        boolean added;
        if (favorites.contains(quoteId)) {
            favorites.remove(quoteId);
            added = false;
        } else {
            favorites.add(quoteId);
            added = true;
        }

        public static boolean toggleFavorite(Context context, QuoteRepository.QuoteItem quote) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> favorites = getFavorites(context);
            boolean added;
            if (favorites.remove(quote.id) | favorites.remove(quote.text)) {
                added = false;
            } else {
                favorites.add(quote.id);
                added = true;
            }
            prefs.edit().putStringSet(KEY, new LinkedHashSet<>(favorites)).apply();
            return added;
        }
        save(context, favorites);
        return added;
    }

    public static List<String> getFavoriteQuotes(Context context) {
        return new ArrayList<>(getFavorites(context));
    }

    public static void migrateLegacyFavorites(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(LEGACY_KEY)) return;
        Set<String> current = getFavorites(context);
        Set<String> legacy = prefs.getStringSet(LEGACY_KEY, new HashSet<>());
        if (legacy != null) current.addAll(legacy);
        prefs.edit().putStringSet(KEY, new LinkedHashSet<>(current)).remove(LEGACY_KEY).apply();
    }

    private static void save(Context context, Set<String> favorites) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(KEY, new LinkedHashSet<>(favorites))
                .apply();
    }
}
