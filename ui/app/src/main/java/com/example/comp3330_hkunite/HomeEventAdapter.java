package com.example.comp3330_hkunite;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeEventAdapter extends RecyclerView.Adapter<HomeEventAdapter.ExploreViewHolder> {
    private List<Event> fullEvents;
    private List<Event> filteredEvents;
    private Context context;

    public HomeEventAdapter(Context context, List<Event> events) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.homeeventview, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Event event = filteredEvents.get(position);

        holder.textEventTitle.setText(event.getTitle());
        holder.textEventDescription.setText(event.getDescription());
        Glide.with(context).load(event.getImageUrl()).into(holder.imageExplore);

        Log.d("ExploreAdapter", "Binding event: " + event.getTitle());

        // Touch listener for press animation
        holder.itemView.setOnTouchListener((v, motionEvent) -> {
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    ViewCompat.setElevation(v, 8f);
                    break;
                case MotionEvent.ACTION_UP:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    ViewCompat.setElevation(v, 2f);
                    // Required for accessibility + proper click handling
                    v.performClick();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    ViewCompat.setElevation(v, 2f);
                    break;
            }
            return true; // consume touch, but performClick() ensures click still fires
        });

//        holder.itemView.setOnClickListener(v -> {
//            // Create the fragment instance
//            EditEventFragment fragment = new EditEventFragment();
//
//            // Pass arguments to the fragment
//            Bundle args = new Bundle();
//            args.putString("EID", event.getEid());
//            fragment.setArguments(args);
//
//            // Get the FragmentManager from the context
//            FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
//
//            // Begin the transaction to replace the current fragment
//            FragmentTransaction transaction = fragmentManager.beginTransaction();
//            transaction.replace(R.id.fragment_container, fragment); // R.id.fragment_container is the layout where fragments are hosted
//            transaction.addToBackStack(null); // Optional: allows user to navigate back
//            transaction.commit();
//        });


    }

    @Override
    public int getItemCount() {
        return filteredEvents.size();
    }

    static class ExploreViewHolder extends RecyclerView.ViewHolder {
        ImageView imageExplore;
        TextView textEventTitle;
        TextView textEventDescription;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            imageExplore = itemView.findViewById(R.id.imageExplore);
            textEventTitle = itemView.findViewById(R.id.textEventTitle);
            textEventDescription = itemView.findViewById(R.id.textEventDescription);
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
