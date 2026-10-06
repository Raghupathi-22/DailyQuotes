package com.raghupathi.banoth.status.motivator.quote;

import androidx.annotation.DrawableRes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CategoryBackgroundCatalog {
    private static final Map<String, int[]> BACKGROUNDS;

    static {
        Map<String, int[]> backgrounds = new HashMap<>();
        backgrounds.put("MORNING", new int[]{
                R.drawable.morning_1, R.drawable.morning_2, R.drawable.morning_3, R.drawable.morning_4,
                R.drawable.morning_5, R.drawable.morning_6, R.drawable.morning_7, R.drawable.morning_8,
                R.drawable.morning_9, R.drawable.morning_10, R.drawable.morning_11, R.drawable.morning_12,
                R.drawable.morning_13, R.drawable.morning_14, R.drawable.morning_15, R.drawable.morning_16,
                R.drawable.morning_17, R.drawable.morning_18, R.drawable.morning_19, R.drawable.morning_20});
        backgrounds.put("EVENING", new int[]{
                R.drawable.evening_1, R.drawable.evening_2, R.drawable.evening_3, R.drawable.evening_4,
                R.drawable.evening_5, R.drawable.evening_6, R.drawable.evening_7, R.drawable.evening_8,
                R.drawable.evening_9, R.drawable.evening_10, R.drawable.evening_11, R.drawable.evening_12,
                R.drawable.evening_13, R.drawable.evening_14, R.drawable.evening_15, R.drawable.evening_16,
                R.drawable.evening_17, R.drawable.evening_18, R.drawable.evening_19, R.drawable.evening_20});
        backgrounds.put("NIGHT", new int[]{
                R.drawable.night_1, R.drawable.night_2, R.drawable.night_3, R.drawable.night_4,
                R.drawable.night_5, R.drawable.night_6, R.drawable.night_7, R.drawable.night_8,
                R.drawable.night_9, R.drawable.night_10, R.drawable.night_11, R.drawable.night_12,
                R.drawable.night_13, R.drawable.night_14, R.drawable.night_15, R.drawable.night_16,
                R.drawable.night_17, R.drawable.night_18, R.drawable.night_19, R.drawable.night_20});
        backgrounds.put("MOTIVATION", new int[]{
                R.drawable.motivation_1, R.drawable.motivation_2, R.drawable.motivation_3, R.drawable.motivation_4,
                R.drawable.motivation_5, R.drawable.motivation_6, R.drawable.motivation_7, R.drawable.motivation_8,
                R.drawable.motivation_9, R.drawable.motivation_10, R.drawable.motivation_11, R.drawable.motivation_12,
                R.drawable.motivation_13, R.drawable.motivation_14, R.drawable.motivation_15, R.drawable.motivation_16,
                R.drawable.motivation_17, R.drawable.motivation_18, R.drawable.motivation_19, R.drawable.motivation_20});
        backgrounds.put("LOVE", new int[]{
                R.drawable.love_1, R.drawable.love_2, R.drawable.love_3, R.drawable.love_4, R.drawable.love_5,
                R.drawable.love_6, R.drawable.love_7, R.drawable.love_8, R.drawable.love_9, R.drawable.love_10,
                R.drawable.love_11, R.drawable.love_12, R.drawable.love_13, R.drawable.love_14, R.drawable.love_15,
                R.drawable.love_16, R.drawable.love_17, R.drawable.love_18, R.drawable.love_19, R.drawable.love_20});
        backgrounds.put("BREAKUP", new int[]{
                R.drawable.breakup_1, R.drawable.breakup_2, R.drawable.breakup_3, R.drawable.breakup_4,
                R.drawable.breakup_5, R.drawable.breakup_6, R.drawable.breakup_7, R.drawable.breakup_8,
                R.drawable.breakup_9, R.drawable.breakup_10, R.drawable.breakup_11, R.drawable.breakup_12,
                R.drawable.breakup_13, R.drawable.breakup_14, R.drawable.breakup_15, R.drawable.breakup_16,
                R.drawable.breakup_17, R.drawable.breakup_18, R.drawable.breakup_19, R.drawable.breakup_20});
        backgrounds.put("EDUCATION", new int[]{
                R.drawable.education_1, R.drawable.education_2, R.drawable.education_3, R.drawable.education_4,
                R.drawable.education_5, R.drawable.education_6, R.drawable.education_7, R.drawable.education_8,
                R.drawable.education_9, R.drawable.education_10, R.drawable.education_11, R.drawable.education_12,
                R.drawable.education_13, R.drawable.education_14, R.drawable.education_15, R.drawable.education_16,
                R.drawable.education_17, R.drawable.education_18, R.drawable.education_19, R.drawable.education_20});
        backgrounds.put("FESTIVAL", new int[]{
                R.drawable.festival_1, R.drawable.festival_2, R.drawable.festival_3, R.drawable.festival_4,
                R.drawable.festival_5, R.drawable.festival_6, R.drawable.festival_7, R.drawable.festival_8,
                R.drawable.festival_9, R.drawable.festival_10, R.drawable.festival_11, R.drawable.festival_12,
                R.drawable.festival_13, R.drawable.festival_14, R.drawable.festival_15, R.drawable.festival_16,
                R.drawable.festival_17, R.drawable.festival_18, R.drawable.festival_19, R.drawable.festival_20});
        backgrounds.put("STRESS_RELIEF", new int[]{
                R.drawable.stress_1, R.drawable.stress_2, R.drawable.stress_3, R.drawable.stress_4,
                R.drawable.stress_5, R.drawable.stress_6, R.drawable.stress_7, R.drawable.stress_8,
                R.drawable.stress_9, R.drawable.stress_10, R.drawable.stress_11, R.drawable.stress_12,
                R.drawable.stress_13, R.drawable.stress_14, R.drawable.stress_15, R.drawable.stress_16,
                R.drawable.stress_17, R.drawable.stress_18, R.drawable.stress_19, R.drawable.stress_20});
        backgrounds.put("RELAX", new int[]{
                R.drawable.relax_1, R.drawable.relax_2, R.drawable.relax_3, R.drawable.relax_4,
                R.drawable.relax_5, R.drawable.relax_6, R.drawable.relax_7, R.drawable.relax_8,
                R.drawable.relax_9, R.drawable.relax_10, R.drawable.relax_11, R.drawable.relax_12,
                R.drawable.relax_13, R.drawable.relax_14, R.drawable.relax_15, R.drawable.relax_16,
                R.drawable.relax_17, R.drawable.relax_18, R.drawable.relax_19, R.drawable.relax_20});
        BACKGROUNDS = Collections.unmodifiableMap(backgrounds);
    }

    private CategoryBackgroundCatalog() {
    }

    @DrawableRes
    public static int getBackground(String categoryId, String quoteId, @DrawableRes int fallback) {
        int[] resources = BACKGROUNDS.get(categoryId);
        if (resources == null || resources.length == 0) return fallback;
        int hash = (categoryId + ":" + quoteId + ":v2").hashCode();
        return resources[(hash & Integer.MAX_VALUE) % resources.length];
    }

    @DrawableRes
    public static int getCardBackground(String categoryId, @DrawableRes int fallback) {
        return getBackground(categoryId, "category-card", fallback);
    }
}
