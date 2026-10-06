package com.raghupathi.banoth.status.motivator.quote;

import androidx.annotation.DrawableRes;

public class Category {
    public final String id;
    public final int titleRes;
    public final int subtitleRes;
    public final int iconRes;
    public final int backgroundRes;
    public final String assetBaseName;

    public Category(String id, int titleRes, int subtitleRes, int iconRes, int backgroundRes, String assetBaseName) {
        this.id = id;
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.iconRes = iconRes;
        this.backgroundRes = backgroundRes;
        this.assetBaseName = assetBaseName;
    }
}
