package com.raghupathi.banoth.status.motivator.quote;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.VH> {
    private final Context context;
    private final List<QuoteRepository.QuoteItem> items = new ArrayList<>();
    private final Runnable onChanged;

    public FavoritesAdapter(Context context, List<QuoteRepository.QuoteItem> items, Runnable onChanged) {
        this.context = context;
        this.items.addAll(items);
        this.onChanged = onChanged;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        QuoteRepository.QuoteItem item = items.get(position);
        holder.textFavorite.setText(item.text);
        holder.btnRemove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            FavoriteRepository.removeFavorite(context, item);
            items.remove(adapterPosition);
            notifyItemRemoved(adapterPosition);
            onChanged.run();
        });
        holder.btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, item.author.isEmpty() ? item.text : item.text + "\n\n— " + item.author);
            context.startActivity(Intent.createChooser(share, context.getString(R.string.share)));
        });
        holder.itemView.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("quote", item.text));
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void submitItems(List<QuoteRepository.QuoteItem> updatedItems) {
        items.clear();
        items.addAll(updatedItems);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView textFavorite;
        ImageButton btnRemove;
        View btnShare;
        VH(View itemView) {
            super(itemView);
            textFavorite = itemView.findViewById(R.id.textFavorite);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}
