package com.raghupathi.banoth.status.motivator.quote;

public class Mood {
    private final String name;
    private final int iconResId;

    public Mood(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }
    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
}
