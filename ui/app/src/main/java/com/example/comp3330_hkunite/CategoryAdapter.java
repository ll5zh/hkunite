package com.example.comp3330_hkunite;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private Context context;
    private List<String> categoryNames;
    private Map<String, List<Event>> categorizedEvents;

    public CategoryAdapter(Context context, Map<String, List<Event>> categorizedEvents) {
        this.context = context;
        this.categorizedEvents = categorizedEvents;
        this.categoryNames = new ArrayList<>(categorizedEvents.keySet());
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_section, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {

        String category = categoryNames.get(position);
        holder.textCategoryLabel.setText(category);
        Log.d("CategoryAdapter", "Binding category: " + category);

        ExploreAdapter exploreAdapter = new ExploreAdapter(context, categorizedEvents.get(category));
        holder.recyclerViewCategoryEvents.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerViewCategoryEvents.setAdapter(exploreAdapter);
    }

    @Override
    public int getItemCount() {
        return categoryNames.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textCategoryLabel;
        RecyclerView recyclerViewCategoryEvents;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategoryLabel = itemView.findViewById(R.id.textCategoryLabel);
            recyclerViewCategoryEvents = itemView.findViewById(R.id.recyclerViewCategoryEvents);
        }
    }
}
