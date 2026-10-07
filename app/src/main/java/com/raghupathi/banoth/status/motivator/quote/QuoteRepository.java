package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Single source of truth for quotes.
 *
 * Category browsing: FETCH (internet) -> FILTER by category -> DEDUPLICATE -> SHUFFLE ONCE -> NEXT -> NEXT ...
 * Fallback order for a category: internet API -> cached session for the SAME category+language ->
 * bundled local quotes for the SAME category+language -> error. Never another category or language.
 */
public class QuoteRepository {
    public static final String PREF_LANGUAGE = "selected_language";
    public static final List<String> SUPPORTED_LANGUAGES = Collections.unmodifiableList(Arrays.asList("en", "te", "hi", "ta"));
    private static final String TAG = "QuoteRepository";
    private static final String API_LANGUAGE = "en";
    private static final int MAX_CACHED_SESSIONS = 16;
    private static final Pattern LEGACY_INDEX_ID = Pattern.compile("^[A-Z_]+_\\d+$");

    public enum LoadStatus {
        /** Fresh category quotes were downloaded from the internet. */
        FRESH_FROM_INTERNET,
        /** The next quote of the existing in-memory session (or the session itself) was reused. */
        FROM_SESSION,
        /** This category/language is served from bundled quotes by design (no API coverage). */
        LOCAL_CATEGORY,
        /** The internet request failed; bundled quotes of the same category/language are shown. */
        OFFLINE_FALLBACK,
        /** The cycle was exhausted and the same unique list was reshuffled for a new cycle. */
        RESHUFFLED
    }

    public interface QuoteCallback {
        @MainThread
        void onQuote(@NonNull QuoteItem quote, @NonNull QuoteSession session, @NonNull LoadStatus status);

        @MainThread
        void onError(@NonNull String categoryId, @NonNull String language);
    }

    private static QuoteRepository instance;
    private final Context context;
    private final boolean debugLogging;
    private final List<Category> categories = new ArrayList<>();
    private final QuoteApiService apiService = new QuoteApiService();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Map<String, List<QuoteItem>> localCache = new ConcurrentHashMap<>();
    // Main-thread only. Key = CATEGORY_language, e.g. BREAKUP_te.
    private final Map<String, QuoteSession> sessions = new LinkedHashMap<String, QuoteSession>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, QuoteSession> eldest) {
            return size() > MAX_CACHED_SESSIONS;
        }
    };
    private final Map<String, PendingLoad> pendingLoads = new LinkedHashMap<>();

    private QuoteRepository(Context context) {
        this.context = context.getApplicationContext();
        this.debugLogging = (this.context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        // apiTags are verified tag names of the quote API. null = the API has no matching tag,
        // so the category is served only from its own bundled quotes (never from another category).
        categories.add(new Category("MORNING", R.string.category_morning, R.string.category_morning_desc, R.drawable.ic_morning, R.drawable.morning_1, "quotes_morning"));
        categories.add(new Category("EVENING", R.string.category_evening, R.string.category_evening_desc, R.drawable.ic_evening, R.drawable.evening_1, "quotes_evening"));
        categories.add(new Category("NIGHT", R.string.category_night, R.string.category_night_desc, R.drawable.ic_night, R.drawable.night_1, "quotes_night"));
        categories.add(new Category("MOTIVATION", R.string.category_motivation, R.string.category_motivation_desc, R.drawable.ic_motivation, R.drawable.motivation_1, "quotes_motivation",
                "Motivational", "Inspirational", "Success", "Perseverance"));
        categories.add(new Category("LOVE", R.string.category_love, R.string.category_love_desc, R.drawable.ic_heart, R.drawable.love_1, "quotes_love",
                "Love"));
        categories.add(new Category("BREAKUP", R.string.category_breakup, R.string.category_breakup_desc, R.drawable.ic_breakup, R.drawable.breakup_1, "quotes_breakup"));
        categories.add(new Category("EDUCATION", R.string.category_education, R.string.category_education_desc, R.drawable.ic_education, R.drawable.education_1, "quotes_education",
                "Education", "Knowledge"));
        categories.add(new Category("FESTIVAL", R.string.category_festival, R.string.category_festival_desc, R.drawable.ic_festival, R.drawable.festival_1, "quotes_festival"));
        categories.add(new Category("STRESS_RELIEF", R.string.category_stress_relief, R.string.category_stress_relief_desc, R.drawable.ic_stress, R.drawable.stress_1, "quotes_stress",
                "Self Help", "Wellness", "Health"));
        categories.add(new Category("RELAX", R.string.category_relax, R.string.category_relax_desc, R.drawable.ic_relax, R.drawable.relax_1, "quotes_relax",
                "Happiness", "Nature", "Gratitude"));
    }

    public static synchronized QuoteRepository getInstance(Context context) {
        if (instance == null) {
            instance = new QuoteRepository(context);
        }
        return instance;
    }

    // ---------------------------------------------------------------- categories & language

    public List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    @Nullable
    public Category findCategory(String id) {
        for (Category category : categories) {
            if (category.id.equals(id)) return category;
        }
        return null;
    }

    public String getLanguage() {
        String language = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LANGUAGE, "en");
        return SUPPORTED_LANGUAGES.contains(language) ? language : "en";
    }

    @MainThread
    public void setLanguage(@NonNull String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) language = "en";
        String previous = getLanguage();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_LANGUAGE, language).apply();
        if (!previous.equals(language)) {
            // Invalidate browsing sessions so no quote of the old language can be shown.
            sessions.clear();
        }
    }

    public boolean usesInternet(@NonNull String categoryId, @NonNull String language) {
        Category category = findCategory(categoryId);
        return category != null && supportsApi(category, language);
    }

    private static boolean supportsApi(Category category, String language) {
        return API_LANGUAGE.equals(language) && !category.apiTags.isEmpty();
    }

    public int getBackgroundRes(QuoteItem quote) {
        Category category = findCategory(quote.categoryId);
        int fallback = category != null ? category.backgroundRes : R.drawable.motivation_1;
        return CategoryBackgroundCatalog.getBackground(quote.categoryId, quote.id, fallback);
    }

    // ---------------------------------------------------------------- category browsing sessions

    private static String sessionKey(String categoryId, String language) {
        return categoryId + "_" + language;
    }

    @MainThread
    @Nullable
    public QuoteSession getSession(@NonNull String categoryId, @NonNull String language) {
        return sessions.get(sessionKey(categoryId, language));
    }

    /** Returns the current quote of the category session, fetching category quotes when no session exists. */
    @MainThread
    public void fetchQuotes(@NonNull String categoryId, @NonNull String language, @NonNull QuoteCallback callback) {
        QuoteSession session = sessions.get(sessionKey(categoryId, language));
        if (session != null && session.current() != null) {
            callback.onQuote(session.current(), session, LoadStatus.FROM_SESSION);
            return;
        }
        load(categoryId, language, 0, false, callback);
    }

    /** Advances to the next unseen quote of the session; refreshes/reshuffles when the cycle is exhausted. */
    @MainThread
    public void getNextQuote(@NonNull String categoryId, @NonNull String language, @NonNull QuoteCallback callback) {
        QuoteSession session = sessions.get(sessionKey(categoryId, language));
        if (session == null || session.isEmpty()) {
            fetchQuotes(categoryId, language, callback);
            return;
        }
        QuoteItem next = session.advance();
        if (next != null) {
            callback.onQuote(next, session, LoadStatus.FROM_SESSION);
            return;
        }
        refreshCategory(categoryId, language, callback);
    }

    /**
     * Starts a new cycle: tries fresh quotes from the internet first; if that fails, reshuffles
     * the existing unique list of the same category/language.
     */
    @MainThread
    public void refreshCategory(@NonNull String categoryId, @NonNull String language, @NonNull QuoteCallback callback) {
        QuoteSession session = sessions.get(sessionKey(categoryId, language));
        Category category = findCategory(categoryId);
        if (session == null || session.isEmpty() || category == null) {
            load(categoryId, language, 0, false, callback);
            return;
        }
        if (!supportsApi(category, language)) {
            session.startCycle(session.snapshot(), session.getSource(), random);
            debugLog("Category: " + categoryId + " | Language: " + language + " | cycle finished, reshuffled " + session.size() + " local quotes");
            callback.onQuote(session.current(), session, LoadStatus.RESHUFFLED);
            return;
        }
        int nextPage = session.getSource() == QuoteSession.Source.API && session.apiLastPage > 0
                ? (session.apiPage + 1) % (session.apiLastPage + 1) : 0;
        load(categoryId, language, nextPage, true, callback);
    }

    @MainThread
    private void load(String categoryId, String language, int page, boolean refresh, QuoteCallback callback) {
        String key = sessionKey(categoryId, language);
        PendingLoad pending = pendingLoads.get(key);
        if (pending != null) {
            pending.callbacks.add(callback);
            return;
        }
        pending = new PendingLoad(refresh);
        pending.callbacks.add(callback);
        pendingLoads.put(key, pending);
        final Category category = findCategory(categoryId);
        networkExecutor.execute(() -> {
            LoadResult result = category == null ? LoadResult.empty() : loadBlocking(category, language, page);
            mainHandler.post(() -> applyLoad(key, categoryId, language, result));
        });
    }

    @MainThread
    private void applyLoad(String key, String categoryId, String language, LoadResult result) {
        PendingLoad pending = pendingLoads.remove(key);
        if (pending == null) return;
        QuoteSession session = sessions.get(key);
        LoadStatus status;
        if (pending.refresh && session != null && !session.isEmpty()) {
            if (result.source == QuoteSession.Source.API && !result.quotes.isEmpty()) {
                session.startCycle(result.quotes, QuoteSession.Source.API, random);
                session.apiPage = result.page;
                session.apiLastPage = result.lastPage;
                status = LoadStatus.FRESH_FROM_INTERNET;
            } else {
                // Network failed: keep this category's cached quotes and start a reshuffled cycle.
                session.startCycle(session.snapshot(), session.getSource(), random);
                status = LoadStatus.RESHUFFLED;
            }
        } else {
            if (result.quotes.isEmpty()) {
                sessions.remove(key);
                for (QuoteCallback callback : pending.callbacks) callback.onError(categoryId, language);
                return;
            }
            session = new QuoteSession(categoryId, language);
            session.startCycle(result.quotes, result.source, random);
            session.apiPage = result.page;
            session.apiLastPage = result.lastPage;
            sessions.put(key, session);
            if (result.source == QuoteSession.Source.API) status = LoadStatus.FRESH_FROM_INTERNET;
            else status = result.networkFailed ? LoadStatus.OFFLINE_FALLBACK : LoadStatus.LOCAL_CATEGORY;
        }
        QuoteItem current = session.current();
        for (QuoteCallback callback : pending.callbacks) {
            if (current != null) callback.onQuote(current, session, status);
            else callback.onError(categoryId, language);
        }
    }

    @WorkerThread
    private LoadResult loadBlocking(Category category, String language, int page) {
        boolean networkFailed = false;
        if (supportsApi(category, language)) {
            try {
                QuoteApiService.ApiPage apiPage = apiService.fetchQuotesByTags(category.apiTags, page);
                List<QuoteItem> filtered = new ArrayList<>();
                for (QuoteApiService.ApiQuote apiQuote : apiPage.quotes) {
                    if (category.id.equals(ownerCategoryId(apiQuote.tags))) {
                        filtered.add(fromApi(apiQuote, category.id, language));
                    }
                }
                List<QuoteItem> unique = deduplicate(filtered);
                logCounts(category.id, language, "API page " + apiPage.page + "/" + apiPage.lastPage,
                        apiPage.quotes.size(), filtered.size(), unique.size());
                if (!unique.isEmpty()) return LoadResult.api(unique, apiPage.page, apiPage.lastPage);
                networkFailed = true;
            } catch (Exception exception) {
                debugLog("Category: " + category.id + " | Language: " + language
                        + " | API request failed: " + exception.getClass().getSimpleName());
                networkFailed = true;
            }
        }
        List<QuoteItem> local = getLocalQuotes(category.id, language);
        return LoadResult.local(local, networkFailed);
    }

    /**
     * An API quote can carry tags of several categories (e.g. Motivational + Happiness). It is assigned
     * to exactly one category - the first matching one in category order - so it never shows up in two.
     */
    @Nullable
    String ownerCategoryId(List<String> quoteTags) {
        for (Category category : categories) {
            if (!category.apiTags.isEmpty() && matchesCategory(quoteTags, category.apiTags)) return category.id;
        }
        return null;
    }

    private static boolean matchesCategory(List<String> quoteTags, List<String> categoryTags) {
        for (String tag : quoteTags) {
            for (String wanted : categoryTags) {
                if (wanted.equalsIgnoreCase(tag)) return true;
            }
        }
        return false;
    }

    private static QuoteItem fromApi(QuoteApiService.ApiQuote apiQuote, String categoryId, String language) {
        String id = apiQuote.id.isEmpty()
                ? stableQuoteId(categoryId, language, apiQuote.content, apiQuote.author)
                : "api_" + apiQuote.id;
        return new QuoteItem(id, apiQuote.content, apiQuote.author, categoryId, language);
    }

    // ---------------------------------------------------------------- quote of the day (separate from sessions)

    /** Deterministic daily quote from the bundled quotes of the category/language; never touches browsing sessions. */
    public QuoteItem getDailyQuote(String categoryId) {
        String language = getLanguage();
        List<QuoteItem> items = getLocalQuotes(categoryId, language);
        if (items.isEmpty()) return QuoteItem.empty(categoryId, language);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        int hash = sha256(date + "|" + categoryId + "|" + language).hashCode();
        return items.get((hash & Integer.MAX_VALUE) % items.size());
    }

    // ---------------------------------------------------------------- local fallback quotes

    /** Bundled quotes for exactly this category and language (de-duplicated). */
    public List<QuoteItem> getLocalQuotes(String categoryId, String language) {
        String cacheKey = sessionKey(categoryId, language);
        List<QuoteItem> cached = localCache.get(cacheKey);
        if (cached != null) return cached;
        Category category = findCategory(categoryId);
        if (category == null) return Collections.emptyList();
        List<QuoteItem> raw = loadQuotesFromAsset(category.assetBaseName + "_" + language + ".json", categoryId, language);
        List<QuoteItem> unique = Collections.unmodifiableList(deduplicate(raw));
        logCounts(categoryId, language, "local asset", raw.size(), raw.size(), unique.size());
        localCache.put(cacheKey, unique);
        return unique;
    }

    private List<QuoteItem> loadQuotesFromAsset(String assetName, String categoryId, String language) {
        List<QuoteItem> items = new ArrayList<>();
        try (InputStream input = context.getAssets().open(assetName)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            String json = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return items;
            JSONArray array = json.startsWith("[") ? new JSONArray(json) : new JSONObject(json).optJSONArray("quotes");
            if (array == null) return items;
            for (int i = 0; i < array.length(); i++) {
                JSONObject quote = array.optJSONObject(i);
                String text = quote != null ? quote.optString("text", quote.optString("quote", "")) : array.optString(i, "");
                String author = quote != null ? quote.optString("author", "") : "";
                text = text.trim();
                author = author.trim();
                if (!text.isEmpty()) {
                    items.add(new QuoteItem(stableQuoteId(categoryId, language, text, author), text, author, categoryId, language));
                }
            }
        } catch (Exception exception) {
            debugLog("Missing or invalid local quotes asset: " + assetName);
        }
        return items;
    }

    // ---------------------------------------------------------------- identity & de-duplication

    /** Lower-cased, NFKC-normalised text with unified quotes/dashes, collapsed whitespace and no trailing punctuation. */
    static String normalize(@Nullable String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u201C', '"').replace('\u201D', '"')
                .replace('\u2013', '-').replace('\u2014', '-')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.replaceAll("^[\"'\\s]+", "").replaceAll("[\"'\\s.!?\u0964]+$", "");
    }

    static String contentKey(String text, String author) {
        return normalize(text) + "\u0000" + normalize(author);
    }

    /** Deterministic id = SHA-256(category + language + normalized text + normalized author). */
    static String stableQuoteId(String categoryId, String language, String text, String author) {
        return "local_" + sha256(categoryId + "|" + language + "|" + contentKey(text, author)).substring(0, 32);
    }

    static List<QuoteItem> deduplicate(List<QuoteItem> quotes) {
        Map<String, QuoteItem> byContent = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        for (QuoteItem quote : quotes) {
            String key = contentKey(quote.text, quote.author);
            if (normalize(quote.text).isEmpty() || byContent.containsKey(key) || !ids.add(quote.id)) continue;
            byContent.put(key, quote);
        }
        return new ArrayList<>(byContent.values());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format(Locale.US, "%02x", b));
            return hex.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    // ---------------------------------------------------------------- favorites

    public List<QuoteItem> getFavoriteQuoteItems() {
        List<QuoteItem> items = new ArrayList<>();
        for (String id : FavoriteRepository.getFavorites(context)) {
            QuoteItem item = FavoriteRepository.getStoredQuote(context, id);
            if (item == null) item = findLocalQuoteById(id);
            if (item == null && !id.startsWith("api_") && !id.startsWith("local_") && !LEGACY_INDEX_ID.matcher(id).matches()) {
                // Very old favorites stored the quote text itself.
                item = new QuoteItem(id, id, "", "", getLanguage());
            }
            if (item != null) items.add(item);
        }
        return items;
    }

    @Nullable
    private QuoteItem findLocalQuoteById(String id) {
        if (!id.startsWith("local_")) return null;
        for (String language : SUPPORTED_LANGUAGES) {
            for (Category category : categories) {
                for (QuoteItem item : getLocalQuotes(category.id, language)) {
                    if (item.id.equals(id)) return item;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- logging

    private void logCounts(String categoryId, String language, String source, int results, int afterFilter, int afterDedup) {
        debugLog("Category: " + categoryId + " | Language: " + language + " | Source: " + source
                + " | Results: " + results + " | After category filter: " + afterFilter
                + " | After deduplication: " + afterDedup);
    }

    private void debugLog(String message) {
        if (debugLogging) Log.d(TAG, message);
    }

    // ---------------------------------------------------------------- types

    private static final class PendingLoad {
        final boolean refresh;
        final List<QuoteCallback> callbacks = new ArrayList<>();

        PendingLoad(boolean refresh) { this.refresh = refresh; }
    }

    private static final class LoadResult {
        final List<QuoteItem> quotes;
        final QuoteSession.Source source;
        final boolean networkFailed;
        final int page;
        final int lastPage;

        private LoadResult(List<QuoteItem> quotes, QuoteSession.Source source, boolean networkFailed, int page, int lastPage) {
            this.quotes = quotes;
            this.source = source;
            this.networkFailed = networkFailed;
            this.page = page;
            this.lastPage = lastPage;
        }

        static LoadResult api(List<QuoteItem> quotes, int page, int lastPage) {
            return new LoadResult(quotes, QuoteSession.Source.API, false, page, lastPage);
        }

        static LoadResult local(List<QuoteItem> quotes, boolean networkFailed) {
            return new LoadResult(quotes, QuoteSession.Source.LOCAL, networkFailed, 0, 0);
        }

        static LoadResult empty() {
            return new LoadResult(Collections.emptyList(), QuoteSession.Source.LOCAL, false, 0, 0);
        }
    }

    public static class QuoteItem {
        public final String id;
        public final String text;
        public final String author;
        public final String categoryId;
        public final String language;

        public QuoteItem(String id, String text, String author, String categoryId, String language) {
            this.id = id;
            this.text = text == null ? "" : text;
            this.author = author == null ? "" : author;
            this.categoryId = categoryId == null ? "" : categoryId;
            this.language = language == null ? "" : language;
        }

        public static QuoteItem empty(String categoryId, String language) {
            return new QuoteItem(categoryId + "_empty", "", "", categoryId, language);
        }
    }
}
