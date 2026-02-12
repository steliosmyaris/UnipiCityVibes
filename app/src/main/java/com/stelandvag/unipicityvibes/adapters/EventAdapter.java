package com.stelandvag.unipicityvibes.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> eventList;
    private OnEventClickListener listener;

    // Interface for click handling
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(Context context, List<Event> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void updateList(List<Event> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView categoryBadge, eventTitle, eventDate, eventPrice, eventSeats;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDate = itemView.findViewById(R.id.eventDate);
            eventPrice = itemView.findViewById(R.id.eventPrice);
            eventSeats = itemView.findViewById(R.id.eventSeats);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEventClick(eventList.get(pos));
                }
            });
        }

        public void bind(Event event) {
            eventTitle.setText(event.getTitle());
            categoryBadge.setText(event.getCategory().toUpperCase());
            eventPrice.setText(String.format(Locale.getDefault(), "€%.0f", event.getPrice()));
            eventSeats.setText(event.getAvailableSeats() + " left");

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            eventDate.setText(sdf.format(new Date(event.getDateTime())));

            // Load image with Glide
            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(event.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_event)
                        .into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.placeholder_event);
            }
        }
    }
}