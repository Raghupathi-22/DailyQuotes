package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuoteRepository {
    public static final String PREF_LANGUAGE = "selected_language";
    private static final String PREF_DAILY_DATE = "daily_quote_date";
    private static final String PREF_DAILY_INDEX = "daily_quote_index";

    private static QuoteRepository instance;
    private final Context context;
    private final Map<String, List<QuoteItem>> cache = new LinkedHashMap<>();
    private final List<Category> categories = new ArrayList<>();

    private QuoteRepository(Context context) {
        this.context = context.getApplicationContext();
        categories.add(new Category("MORNING", R.string.category_morning, R.string.category_morning_desc, R.drawable.ic_morning, R.drawable.morning_1, "quotes_morning"));
        categories.add(new Category("EVENING", R.string.category_evening, R.string.category_evening_desc, R.drawable.ic_evening, R.drawable.evening_1, "quotes_evening"));
        categories.add(new Category("NIGHT", R.string.category_night, R.string.category_night_desc, R.drawable.ic_night, R.drawable.night_1, "quotes_night"));
        categories.add(new Category("MOTIVATION", R.string.category_motivation, R.string.category_motivation_desc, R.drawable.ic_motivation, R.drawable.motivation_1, "quotes_motivation"));
        categories.add(new Category("LOVE", R.string.category_love, R.string.category_love_desc, R.drawable.ic_heart, R.drawable.love_1, "quotes_love"));
        categories.add(new Category("BREAKUP", R.string.category_breakup, R.string.category_breakup_desc, R.drawable.ic_breakup, R.drawable.breakup_1, "quotes_breakup"));
        categories.add(new Category("EDUCATION", R.string.category_education, R.string.category_education_desc, R.drawable.ic_education, R.drawable.education_1, "quotes_education"));
        categories.add(new Category("FESTIVAL", R.string.category_festival, R.string.category_festival_desc, R.drawable.ic_festival, R.drawable.festival_1, "quotes_festival"));
        categories.add(new Category("STRESS_RELIEF", R.string.category_stress_relief, R.string.category_stress_relief_desc, R.drawable.ic_stress, R.drawable.stress_1, "quotes_sad"));
        categories.add(new Category("RELAX", R.string.category_relax, R.string.category_relax_desc, R.drawable.ic_relax, R.drawable.relax_1, "quotes_good"));
    }

    public static QuoteRepository getInstance(Context context) {
        if (instance == null) {
            instance = new QuoteRepository(context);
        }
        return instance;
    }

    public List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    public Category findCategory(String id) {
        for (Category category : categories) {
            if (category.id.equals(id)) return category;
        }
        return categories.get(0);
    }

    public int getBackgroundRes(QuoteItem quote) {
        Category category = findCategory(quote.categoryId);
        return CategoryBackgroundCatalog.getBackground(category.id, quote.id, category.backgroundRes);
    }

    public QuoteItem getDailyQuote(String categoryId) {
        List<QuoteItem> items = getQuotes(categoryId);
        if (items.isEmpty()) return QuoteItem.empty(categoryId);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String keyBase = categoryId + "_" + getLanguage();
        String savedDate = prefs.getString(PREF_DAILY_DATE + keyBase, null);
        int index = prefs.getInt(PREF_DAILY_INDEX + keyBase, -1);
        if (!date.equals(savedDate) || index < 0 || index >= items.size()) {
            index = Math.abs((date + keyBase).hashCode()) % items.size();
            prefs.edit().putString(PREF_DAILY_DATE + keyBase, date).putInt(PREF_DAILY_INDEX + keyBase, index).apply();
        }
        return items.get(index);
    }

    public QuoteItem getRandomQuote(String categoryId, String excludeId) {
        List<QuoteItem> items = getQuotes(categoryId);
        if (items.isEmpty()) return QuoteItem.empty(categoryId);
        if (items.size() == 1) return items.get(0);
        int currentIndex = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(excludeId)) {
                currentIndex = i;
                break;
            }
        }
        return items.get((currentIndex + 1) % items.size());
    }

    public List<QuoteItem> getQuotes(String categoryId) {
        String language = getLanguage();
        String cacheKey = categoryId + "_" + language;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
        List<QuoteItem> result = new ArrayList<>();
        String base = findCategory(categoryId).assetBaseName;
        for (String suffix : Arrays.asList("_" + language, "", "_en")) {
            String asset = base + suffix + ".json";
            result = loadQuotesFromAsset(asset, categoryId);
            if (!result.isEmpty()) break;
        }
        cache.put(cacheKey, result);
        return result;
    }

    public List<QuoteItem> getFavoriteQuoteItems() {
        List<QuoteItem> items = new ArrayList<>();
        for (Category category : categories) {
            for (QuoteItem item : getQuotes(category.id)) {
                if (FavoriteRepository.isFavorite(context, item)) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    public QuoteItem findQuoteById(String id) {
        for (Category category : categories) {
            for (QuoteItem item : getQuotes(category.id)) {
                if (item.id.equals(id)) return item;
            }
        }
        return null;
    }

    public String getLanguage() {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LANGUAGE, "en");
    }

    private List<QuoteItem> loadQuotesFromAsset(String assetName, String categoryId) {
        try {
            AssetManager assets = context.getAssets();
            InputStream input = assets.open(assetName);
            byte[] buffer = new byte[input.available()];
            int read = input.read(buffer);
            input.close();
            if (read <= 0) return new ArrayList<>();
            String json = new String(buffer, StandardCharsets.UTF_8).trim();
            List<QuoteItem> items = new ArrayList<>();
            if (json.startsWith("[")) {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    String value = array.optString(i, "").trim();
                    if (!value.isEmpty()) items.add(new QuoteItem(categoryId + "_" + i, value, "", categoryId));
                }
            } else {
                JSONObject object = new JSONObject(json);
                JSONArray array = object.optJSONArray("quotes");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject quote = array.optJSONObject(i);
                        if (quote == null) continue;
                        String text = quote.optString("quote", quote.optString("text", "")).trim();
                        String author = quote.optString("author", "").trim();
                        String id = quote.optString("id", categoryId + "_" + i).trim();
                        if (!text.isEmpty()) items.add(new QuoteItem(id, text, author, categoryId));
                    }
                }
            }
            return items;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static class QuoteItem {
        public final String id;
        public final String text;
        public final String author;
        public final String categoryId;

        public QuoteItem(String id, String text, String author, String categoryId) {
            this.id = id;
            this.text = text;
            this.author = author;
            this.categoryId = categoryId;
        }

        public static QuoteItem empty(String categoryId) {
            return new QuoteItem(categoryId + "_empty", "", "", categoryId);
        }
    }
}
