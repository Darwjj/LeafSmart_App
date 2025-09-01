package com.example.leafsmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter is used for displaying and filtering the disease list
 */

public class DiseaseAdapter extends RecyclerView.Adapter<DiseaseAdapter.ViewHolder> implements Filterable {
    public interface OnItemClickListener {
        void onItemClick(String diseaseName);
    }

    private final List<String> diseaseList; // List displayed
    private final OnItemClickListener listener; // Click listener for each disease
    private List<String> fullDiseaseList; // Full, unfiltered list for all diseases


    public DiseaseAdapter(List<String> diseaseList, OnItemClickListener listener) {
        this.diseaseList = new ArrayList<>(diseaseList);
        this.fullDiseaseList = new ArrayList<>(diseaseList);
        this.listener = listener;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String diseaseName = diseaseList.get(position);
        holder.textView.setText(diseaseName);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(diseaseName));
    }

    @Override
    public int getItemCount() {
        return diseaseList.size();
    }
//
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<String> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(fullDiseaseList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (String item : fullDiseaseList) {
                        if (item.toLowerCase().contains(filterPattern)) {
                            filtered.add(item);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                diseaseList.clear();
                diseaseList.addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        };
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
        }
    }
}
