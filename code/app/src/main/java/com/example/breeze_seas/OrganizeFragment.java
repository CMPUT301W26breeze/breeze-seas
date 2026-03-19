package com.example.breeze_seas;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OrganizeFragment displays the active organizer's events and routes into organizer-specific
 * secondary screens such as create-event and event preview.
 */
public class OrganizeFragment extends Fragment {

    private final List<Event> events = new ArrayList<>();
    private EventAdapter adapter;
    private SessionViewModel viewModel;

    /**
     * Creates the top-level organizer fragment using the shared organize layout.
     */
    public OrganizeFragment() {
        super(R.layout.fragment_organize);
    }

    /**
     * Binds organizer list views, wires click handlers, and triggers the initial event load.
     *
     * @param view Inflated organize-fragment root view.
     * @param savedInstanceState Previously saved state bundle, or {@code null} on first creation.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        RecyclerView rv = view.findViewById(R.id.rvMyEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EventAdapter(events, new EventAdapter.OnEventClickListener() {
            /**
             * Opens the organizer preview for the selected event row.
             *
             * @param event Event selected from the organizer list.
             */
            @Override
            public void onEventClick(Event event) {
                openEventPreview(event);
            }
        });
        rv.setAdapter(adapter);

        loadEvents();

        View createButton = view.findViewById(R.id.fabCreateEvent);
        createButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the organizer create-event flow when the primary action is pressed.
             *
             * @param v Organizer create button that was tapped.
             */
            @Override
            public void onClick(View v) {
                ((MainActivity) requireActivity()).openSecondaryFragment(new CreateEventFragment());
            }
        });
    }

    /**
     * Refreshes organizer events whenever the fragment returns to the foreground.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Loads all events from {@link EventDB}, filters them to the current organizer, and refreshes
     * the RecyclerView adapter.
     */
    private void loadEvents() {
        EventDB.getAllEvents(new EventDB.LoadEventsCallback() {
            /**
             * Replaces the visible organizer event list with the freshly loaded results.
             *
             * @param loadedEvents Events returned by {@link EventDB}.
             */
            @Override
            public void onSuccess(ArrayList<Event> loadedEvents) {
                events.clear();
                events.addAll(filterEventsForCurrentOrganizer(loadedEvents));
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            /**
             * Reports a user-visible error if organizer events cannot be loaded.
             *
             * @param e Failure returned by the event-loading request.
             */
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the organizer preview flow for the selected event.
     *
     * @param event Organizer-owned event selected from the list.
     */
    private void openEventPreview(@NonNull Event event) {
        if (viewModel != null) {
            viewModel.setEventShown(event);
        }

        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());

        OrganizerEventPreviewFragment fragment = new OrganizerEventPreviewFragment();
        fragment.setArguments(args);
        ((MainActivity) requireActivity()).openSecondaryFragment(fragment);
    }

    /**
     * Filters a loaded event list down to the events owned by the current organizer device id.
     *
     * @param loadedEvents Events returned from {@link EventDB}, or {@code null}.
     * @return Organizer-owned events, or all loaded events when no organizer id is available.
     */
    @NonNull
    private List<Event> filterEventsForCurrentOrganizer(@Nullable List<Event> loadedEvents) {
        if (loadedEvents == null) {
            return new ArrayList<>();
        }

        String currentOrganizerId = null;
        if (viewModel != null && viewModel.getAndroidID().getValue() != null) {
            currentOrganizerId = viewModel.getAndroidID().getValue();
        }

        if (currentOrganizerId == null || currentOrganizerId.trim().isEmpty()) {
            return new ArrayList<>(loadedEvents);
        }

        List<Event> filteredEvents = new ArrayList<>();
        for (Event event : loadedEvents) {
            if (event != null && currentOrganizerId.equals(event.getOrganizerId())) {
                filteredEvents.add(event);
            }
        }
        return filteredEvents;
    }

    /**
     * RecyclerView adapter used to render organizer event cards in the active organize flow.
     */
    static class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

        /**
         * Listener notified when an organizer selects an event card from the list.
         */
        interface OnEventClickListener {
            /**
             * Opens the selected organizer event.
             *
             * @param event Event selected from the organizer list.
             */
            void onEventClick(Event event);
        }

        private final List<Event> data;
        private final OnEventClickListener onEventClickListener;

        /**
         * Creates an adapter for organizer event cards.
         *
         * @param data Event list rendered by the adapter.
         * @param onEventClickListener Click listener invoked when an event card is tapped.
         */
        EventAdapter(List<Event> data, OnEventClickListener onEventClickListener) {
            this.data = data;
            this.onEventClickListener = onEventClickListener;
        }

        /**
         * ViewHolder that caches the views used by a single organizer event row.
         */
        static class VH extends RecyclerView.ViewHolder {
            ImageView ivPoster;
            TextView tvName, tvDates, tvCap, tvDetails, tvAction;

            /**
             * Creates a ViewHolder for one organizer event row.
             *
             * @param itemView Inflated row view associated with this holder.
             */
            VH(@NonNull View itemView) {
                super(itemView);
                ivPoster = itemView.findViewById(R.id.ivEventPoster);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvDates = itemView.findViewById(R.id.tvEventDates);
                tvCap = itemView.findViewById(R.id.tvEventCapacity);
                tvDetails = itemView.findViewById(R.id.tvEventDetails);
                tvAction = itemView.findViewById(R.id.tvEventAction);
            }
        }

        /**
         * Inflates one organizer event row.
         *
         * @param parent RecyclerView that will host the new row.
         * @param viewType Adapter view type for the requested row.
         * @return ViewHolder bound to the inflated organizer event view.
         */
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new VH(v);
        }

        /**
         * Binds organizer event data into a visible row.
         *
         * @param holder ViewHolder receiving the event data.
         * @param position Adapter position being bound.
         */
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event e = data.get(position);

            holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
            if (e.getImage() != null && !e.getImage().trim().isEmpty()) {
                try {
                    holder.ivPoster.setImageURI(Uri.parse(e.getImage()));
                } catch (Exception ignored) {
                    holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
                }
            }

            holder.tvName.setText(e.getName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String from = formatTimestamp(sdf, e.getRegistrationStartTimestamp());
            String to = formatTimestamp(sdf, e.getRegistrationEndTimestamp());
            holder.tvDates.setText("Reg: " + from + " → " + to);

            Integer cap = e.getWaitingListCapacity();
            holder.tvCap.setText(cap == null || cap < 0
                    ? "Waiting list cap: Unlimited"
                    : "Waiting list cap: " + cap);
            holder.tvDetails.setText(e.getDescription().trim().isEmpty()
                    ? holder.itemView.getContext().getString(R.string.organize_event_no_description)
                    : e.getDescription());
            holder.tvAction.setText(R.string.organize_event_open_preview);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                /**
                 * Opens the organizer preview for the tapped event card.
                 *
                 * @param v Event row view that was tapped.
                 */
                @Override
                public void onClick(View v) {
                    onEventClickListener.onEventClick(e);
                }
            });
        }

        /**
         * Returns the number of organizer events currently available to the adapter.
         *
         * @return Adapter item count.
         */
        @Override
        public int getItemCount() {
            return data.size();
        }

        /**
         * Formats a Firestore timestamp for organizer list display.
         *
         * @param sdf Date formatter configured for organizer display.
         * @param timestamp Firestore timestamp to format.
         * @return Display-ready date string, or {@code "Not set"} when the timestamp is absent.
         */
        @NonNull
        private String formatTimestamp(@NonNull SimpleDateFormat sdf, @Nullable Timestamp timestamp) {
            return sdf.format(new Date(timestamp.toDate().getTime()));
        }
    }
}
