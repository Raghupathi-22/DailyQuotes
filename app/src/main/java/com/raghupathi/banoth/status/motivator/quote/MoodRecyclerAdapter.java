package com.raghupathi.banoth.status.motivator.quote;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MoodRecyclerAdapter extends RecyclerView.Adapter<MoodRecyclerAdapter.VH> {
    public interface Listener { void onClick(Category category); }
    private final List<Category> items;
    private final Listener listener;

    public MoodRecyclerAdapter(android.content.Context context, List<Category> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mood, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category item = items.get(position);
        holder.name.setText(item.titleRes);
        Glide.with(holder.icon)
                .load(CategoryBackgroundCatalog.getCardBackground(item.id, item.backgroundRes))
                .centerCrop()
                .override(480, 264)
                .thumbnail(0.25f)
                .placeholder(item.iconRes)
                .into(holder.icon);
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(item.titleRes));
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; TextView name;
        VH(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.imageMoodIcon);
            name = itemView.findViewById(R.id.textMoodName);
        }
    }
}
