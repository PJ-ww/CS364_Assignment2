package com.example.assignment2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.DiffUtil;

public class ActivityAdapter
        extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClick(ActivityItem item);
    }

    private final List<ActivityItem> items = new ArrayList<>();
    private final OnActivityClickListener clickListener;

    public ActivityAdapter(OnActivityClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setItems(List<ActivityItem> newItems) {
        final List<ActivityItem> updatedItems = newItems == null
                ? new ArrayList<>()
                : new ArrayList<>(newItems);
        final List<ActivityItem> oldItems = new ArrayList<>(items);

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return oldItems.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return updatedItems.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldPosition, int newPosition) {
                        return oldItems.get(oldPosition).id
                                == updatedItems.get(newPosition).id;
                    }

                    @Override
                    public boolean areContentsTheSame(int oldPosition, int newPosition) {
                        ActivityItem oldItem = oldItems.get(oldPosition);
                        ActivityItem newItem = updatedItems.get(newPosition);

                        return oldItem.startedAtMillis == newItem.startedAtMillis
                                && oldItem.durationMillis == newItem.durationMillis
                                && oldItem.steps == newItem.steps
                                && oldItem.shakes == newItem.shakes
                                && Double.compare(oldItem.stepsPerMinute,
                                newItem.stepsPerMinute) == 0
                                && Double.compare(oldItem.shakesPerMinute,
                                newItem.shakesPerMinute) == 0;
                    }
                });

        items.clear();
        items.addAll(updatedItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ActivityViewHolder holder, int position) {
        ActivityItem item = items.get(position);

        String date = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.startedAtMillis));

        holder.tvDate.setText(date);
        holder.tvCounts.setText(holder.itemView.getContext().getString(
                R.string.activity_row_counts, item.steps, item.shakes));

        holder.tvDuration.setText(holder.itemView.getContext().getString(
                R.string.activity_row_duration,
                formatDuration(item.durationMillis)));

        holder.itemView.setOnClickListener(
                view -> clickListener.onActivityClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(0, durationMillis / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d นาที",
                minutes, seconds);
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvCounts;
        final TextView tvDuration;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvActivityDate);
            tvCounts = itemView.findViewById(R.id.tvActivityCounts);
            tvDuration = itemView.findViewById(R.id.tvActivityDuration);
        }
    }
}