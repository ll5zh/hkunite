package com.example.comp3330_hkunite;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder> {
    private List<Event> fullEvents;
    private List<Event> filteredEvents;
    private Context context;

    public ExploreAdapter(Context context, List<Event> events) {
        this.context = context;
        this.fullEvents = new ArrayList<>(events);
        this.filteredEvents = new ArrayList<>(events);
    }

    public void filter(String keyword) {
        filteredEvents.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredEvents.addAll(fullEvents);
        } else {
            String lowerKeyword = keyword.toLowerCase();
            for (Event event : fullEvents) {
                if (event.getTitle().toLowerCase().contains(lowerKeyword) ||
                        event.getDescription().toLowerCase().contains(lowerKeyword)) {
                    filteredEvents.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_explore, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Event event = filteredEvents.get(position);
        Glide.with(context).load(event.getImageUrl()).into(holder.imageExplore);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailActivity.class);
            intent.putExtra("EID", event.getEid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredEvents.size();
    }

    static class ExploreViewHolder extends RecyclerView.ViewHolder {
        ImageView imageExplore;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            imageExplore = itemView.findViewById(R.id.imageExplore);
        }
    }
    public void updateEvents(List<Event> newEvents) {
        fullEvents.clear();
        fullEvents.addAll(newEvents);
        filteredEvents.clear();
        filteredEvents.addAll(newEvents);
        notifyDataSetChanged();
    }

}
