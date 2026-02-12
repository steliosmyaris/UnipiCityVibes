package com.stelandvag.unipicityvibes.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.ViewHolder> {

    private Context context;
    private List<Event> eventList;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public CalendarEventAdapter(Context context, List<Event> eventList, OnEventClickListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventTime, eventTitle, eventVenue, categoryBadge, eventPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTime = itemView.findViewById(R.id.eventTime);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventVenue = itemView.findViewById(R.id.eventVenue);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            eventPrice = itemView.findViewById(R.id.eventPrice);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEventClick(eventList.get(pos));
                }
            });
        }

        public void bind(Event event) {
            eventTitle.setText(event.getTitle());
            eventVenue.setText("📍 " + event.getVenue());
            categoryBadge.setText(event.getCategory().toUpperCase());
            eventPrice.setText(String.format(Locale.getDefault(), "€%.0f", event.getPrice()));

            // Format time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            eventTime.setText(timeFormat.format(new Date(event.getDateTime())));
        }
    }
}