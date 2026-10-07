package com.raghupathi.banoth.status.motivator.quote;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Client for the Quotable-compatible quote API (https://api.quotable.kurokeita.dev).
 *
 * Verified response of GET /api/quotes?tags=Love&limit=100&page=0:
 * {"data":[{"id":"...","content":"...","author":{"name":"..."},"tags":[{"name":"Love"}]}],
 *  "metadata":{"total":20,"page":0,"lastPage":0,"hasNextPage":false}}
 *
 * Server rules: "tags" uses "|" for OR, "limit" must be 10/25/50/100, pages are zero-based.
 * The API serves English quotes only.
 */
public final class QuoteApiService {
    static final String BASE_URL = "https://api.quotable.kurokeita.dev/api/quotes";
    static final int PAGE_LIMIT = 100;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    public static final class ApiQuote {
        public final String id;
        public final String content;
        public final String author;
        public final List<String> tags;

        ApiQuote(String id, String content, String author, List<String> tags) {
            this.id = id;
            this.content = content;
            this.author = author;
            this.tags = tags;
        }
    }

    public static final class ApiPage {
        public final List<ApiQuote> quotes;
        public final int page;
        public final int lastPage;
        public final int total;

        ApiPage(List<ApiQuote> quotes, int page, int lastPage, int total) {
            this.quotes = quotes;
            this.page = page;
            this.lastPage = lastPage;
            this.total = total;
        }
    }

    @WorkerThread
    @NonNull
    public ApiPage fetchQuotesByTags(@NonNull List<String> tags, int page) throws IOException, JSONException {
        StringBuilder tagQuery = new StringBuilder();
        for (String tag : tags) {
            if (tagQuery.length() > 0) tagQuery.append('|');
            tagQuery.append(tag);
        }
        String url = BASE_URL
                + "?limit=" + PAGE_LIMIT
                + "&page=" + Math.max(0, page)
                + "&tags=" + URLEncoder.encode(tagQuery.toString(), "UTF-8");
        return parsePage(get(url));
    }

    private String get(String spec) throws IOException {
        URL url = new URL(spec);
        if (!"https".equals(url.getProtocol())) throw new IOException("Only HTTPS is allowed");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setInstanceFollowRedirects(false);
            int code = connection.getResponseCode();
            if (code != HttpsURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    if (output.size() > MAX_RESPONSE_BYTES) throw new IOException("Response too large");
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    static ApiPage parsePage(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray data = root.optJSONArray("data");
        List<ApiQuote> quotes = new ArrayList<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                String content = item.optString("content", "").trim();
                if (content.isEmpty()) continue;
                String author = "";
                JSONObject authorObject = item.optJSONObject("author");
                if (authorObject != null) author = authorObject.optString("name", "").trim();
                else author = item.optString("author", "").trim();
                List<String> tags = new ArrayList<>();
                JSONArray tagArray = item.optJSONArray("tags");
                if (tagArray != null) {
                    for (int t = 0; t < tagArray.length(); t++) {
                        JSONObject tagObject = tagArray.optJSONObject(t);
                        String name = tagObject != null ? tagObject.optString("name", "") : tagArray.optString(t, "");
                        if (!name.isEmpty()) tags.add(name);
                    }
                }
                quotes.add(new ApiQuote(item.optString("id", "").trim(), content, author,
                        Collections.unmodifiableList(tags)));
            }
        }
        JSONObject metadata = root.optJSONObject("metadata");
        int page = metadata != null ? metadata.optInt("page", 0) : 0;
        int lastPage = metadata != null ? metadata.optInt("lastPage", 0) : 0;
        int total = metadata != null ? metadata.optInt("total", quotes.size()) : quotes.size();
        return new ApiPage(quotes, page, Math.max(0, lastPage), total);
    }
}
