package com.example.leafsmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * RecyclerView Adapter for display save history list.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    private final List<HistoryItem> historyList; // Data source for the list
    private final OnItemClickListener listener; // Click listener callback

    public HistoryAdapter(List<HistoryItem> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        holder.bind(historyList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampText, diseaseText, confidenceText;

        public ViewHolder(View view) {
            super(view);
            timestampText = view.findViewById(R.id.textTimestamp);
            diseaseText = view.findViewById(R.id.textDiseaseName);
            confidenceText = view.findViewById(R.id.textConfidence);
        }

        public void bind(final HistoryItem item, final OnItemClickListener listener) {
            timestampText.setText(item.getFormattedDate());
            diseaseText.setText(item.getDiseaseName());
            confidenceText.setText(String.format("Confidence: %.2f%%", item.getConfidence()));
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
