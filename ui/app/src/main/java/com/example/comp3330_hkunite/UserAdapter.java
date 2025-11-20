package com.example.comp3330_hkunite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private Context context;

    // Listener for clicks
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private OnUserClickListener listener;

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public UserAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    public void updateList(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.userName.setText(user.getName());

        Glide.with(context)
                .load(user.getImageUrl())
                .placeholder(R.drawable.default_profile)
                .circleCrop()
                .into(holder.userImage);

        // Update background based on selection
        holder.itemView.setSelected(user.isSelected());

        // Toggle selection on click
        holder.itemView.setOnClickListener(v -> {
            user.setSelected(!user.isSelected());
            notifyItemChanged(position);

            // Notify InviteActivity so it can add/remove chips
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageView userImage;

        UserViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userImage = itemView.findViewById(R.id.userImage);
        }
    }
}
