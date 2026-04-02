package com.example.breeze_seas;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    private ArrayList<Event> events;
    private EventAdapter adapter;
    private SessionViewModel viewModel;
    private OrganizeViewModel organizeViewModel;
    private EventHandler organizeEventHandler;
    private User user;

    /**
     * Creates the top-level organizer fragment using the shared organize layout.
     */
    public OrganizeFragment() {
        super(R.layout.fragment_organize);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        user = viewModel.getUser().getValue();

        // Setup EventHandler class if necessary
        organizeViewModel = new ViewModelProvider(requireActivity()).get(OrganizeViewModel.class);
        if (!organizeViewModel.eventHandlerIsInitialized()) {
            organizeViewModel.setEventHandler(new EventHandler(
                    getActivity(),
                    getContext().getApplicationContext(),
                    EventDB.getAllEventsOrganizedByUserQuery(user),
                    viewModel.getAndroidID().getValue(),
                    false,
                    false));
        }
        // Save reference to EventHandler list of events
        events = organizeViewModel.getEventHandler().getEvents().getValue();
        // Grab reference
        organizeEventHandler = organizeViewModel.getEventHandler();
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

        // Get realtime updated events from organizeEventHandler
        // The observer also runs on startup.
        organizeEventHandler.getEvents().observe(getViewLifecycleOwner(), updatedEvents -> {
            // Ignore the updatedEvents as the fragment's event list is already the same reference to the EventHandler
            loadEvents();
        });

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
                openCreateEventChooser();
            }
        });
    }

    /**
     * Shows the organizer create-event chooser so the user can pick public or private first.
     */
    private void openCreateEventChooser() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_event_visibility, null, false);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.create_public_event_option).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the public create-event flow from the chooser.
             *
             * @param v Public-event option that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openCreateEventFlow(false);
            }
        });

        dialogView.findViewById(R.id.create_private_event_option).setOnClickListener(new View.OnClickListener() {
            /**
             * Opens the private create-event flow from the chooser.
             *
             * @param v Private-event option that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openCreateEventFlow(true);
            }
        });

        dialogView.findViewById(R.id.create_event_visibility_cancel).setOnClickListener(new View.OnClickListener() {
            /**
             * Closes the visibility chooser without opening the create-event form.
             *
             * @param v Cancel button that was tapped.
             */
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Opens the organizer create-event form with the requested visibility type preselected.
     *
     * @param isPrivateEvent {@code true} for a private event, {@code false} for a public event.
     */
    private void openCreateEventFlow(boolean isPrivateEvent) {
        ((MainActivity) requireActivity()).openSecondaryFragment(CreateEventFragment.newInstance(isPrivateEvent));
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
     * Refresh the RecyclerView adapter to show all events that are
     * organized by the current user (co-organizer or original organizer)
     */
    private void loadEvents() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Opens the organizer preview flow for the selected event.
     *
     * @param event Organizer-owned event selected from the list.
     */
    private void openEventPreview(@NonNull Event event) {
        // Set Event Shown for OrganizerEventPreviewFragment
        organizeEventHandler.setEventShown(event);

        // Switch to Organizer Event Preview fragment
        ((MainActivity) requireActivity()).openSecondaryFragment(new OrganizerEventPreviewFragment());
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
            FrameLayout ivPosterFrame;
            ImageView ivPoster;
            TextView tvName, tvTypeChip, tvDates, tvCap, tvDetails, tvAction;

            /**
             * Creates a ViewHolder for one organizer event row.
             *
             * @param itemView Inflated row view associated with this holder.
             */
            VH(@NonNull View itemView) {
                super(itemView);
                ivPosterFrame = itemView.findViewById(R.id.ivEventPosterFrame);
                ivPoster = itemView.findViewById(R.id.ivEventPoster);
                tvName = itemView.findViewById(R.id.tvEventName);
                tvTypeChip = itemView.findViewById(R.id.tvEventTypeChip);
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

            // Handle Image
            holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
            holder.ivPoster.setVisibility(View.GONE);
            holder.ivPosterFrame.setVisibility(View.GONE);
            if (e.getImage() != null) {
                try {
                    // Show image if possible
                    holder.ivPoster.setImageBitmap(e.getImage().display());
                    holder.ivPoster.setVisibility(View.VISIBLE);
                    holder.ivPosterFrame.setVisibility(View.VISIBLE);
                } catch (Exception ignored) {
                    // Hide image
                    holder.ivPoster.setImageResource(R.drawable.ic_image_placeholder);
                    holder.ivPoster.setVisibility(View.GONE);
                    holder.ivPosterFrame.setVisibility(View.GONE);
                }
            }

            holder.tvName.setText(e.getName());
            holder.tvTypeChip.setText(e.isPrivate()
                    ? R.string.organize_event_type_private
                    : R.string.organize_event_type_public);
            holder.tvTypeChip.setBackgroundResource(e.isPrivate()
                    ? R.drawable.bg_ticket_status_solid
                    : R.drawable.bg_ticket_status_outline);
            holder.tvTypeChip.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(),
                    e.isPrivate() ? R.color.white : R.color.text_primary
            ));

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
