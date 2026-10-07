package com.raghupathi.banoth.status.motivator.quote;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Category {
    /** Stable identifier (e.g. "BREAKUP"); never use display names for logic. */
    public final String id;
    public final int titleRes;
    public final int subtitleRes;
    public final int iconRes;
    public final int backgroundRes;
    /** Local fallback assets are named {@code <assetBaseName>_<language>.json}. */
    public final String assetBaseName;
    /** Tag names of the quote API that belong to this category; empty when the API has no matching tag. */
    public final List<String> apiTags;

    public Category(String id, int titleRes, int subtitleRes, int iconRes, int backgroundRes, String assetBaseName, String... apiTags) {
        this.id = id;
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.iconRes = iconRes;
        this.backgroundRes = backgroundRes;
        this.assetBaseName = assetBaseName;
        this.apiTags = Collections.unmodifiableList(Arrays.asList(apiTags));
    }
}
