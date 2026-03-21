package com.example.breeze_seas;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {
    private ImageView returnButton;
    private TextView eventTitle;
    private ImageView eventPoster;
    private Button viewQRCodeButton;
    private TextView eventCapacity;
    private TextView eventWaitingListCount;
    private TextView eventStartDate;
    private TextView eventEndDate;
    private TextView eventDescription;
    private Button joinWaitingListButton;
    private Button leaveWaitingListButton;
    private TextView eventInviteText;
    private Button acceptInviteButton;
    private Button declineInviteButton;
    private TextView eventInviteAcceptedText;
    private TextView eventInviteDeclinedText;

    private SessionViewModel viewModel;
    private Event eventShown;
    private WaitingList waitingList;
    private PendingList pendingList;
    private AcceptedList acceptedList;
    private DeclinedList declinedList;
    private User user;

    public EventDetailsFragment() {
        super(R.layout.fragment_event_details);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        user = viewModel.getUser().getValue();
        eventShown = viewModel.getEventShown().getValue();
        assert eventShown != null;

        waitingList = eventShown.getWaitingList();
        pendingList = eventShown.getPendingList();
        acceptedList = eventShown.getAcceptedList();
        declinedList = eventShown.getDeclinedList();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventTitle = view.findViewById(R.id.event_details_event_title);
        eventPoster = view.findViewById(R.id.event_details_event_photo);
        eventCapacity = view.findViewById(R.id.event_details_event_capacity);
        eventWaitingListCount = view.findViewById(R.id.event_details_event_waiting_list_count);
        eventStartDate = view.findViewById(R.id.event_details_event_start_date);
        eventEndDate = view.findViewById(R.id.event_details_event_end_date);
        eventDescription = view.findViewById(R.id.event_details_event_description);
        eventInviteText = view.findViewById(R.id.event_details_invite_text);
        eventInviteAcceptedText = view.findViewById(R.id.event_details_invite_accepted_text);
        eventInviteDeclinedText = view.findViewById(R.id.event_details_invite_declined_text);

        returnButton = view.findViewById(R.id.event_details_return_button);
        returnButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        viewQRCodeButton = view.findViewById(R.id.event_details_view_QRCode_button);
        viewQRCodeButton.setOnClickListener(v -> {
            // TODO: implement logic to show QRCode
        });

        joinWaitingListButton = view.findViewById(R.id.event_details_join_waitlist_button);
        joinWaitingListButton.setOnClickListener(v -> {
            waitingList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    int waitingListCapacity = waitingList.getCapacity();
                    int waitingListSize = waitingList.getSize();

                    if ((waitingListCapacity != -1) && (waitingListSize >= waitingListCapacity)) {
                        Log.w("waitingList DB Call", "Waiting list capacity reached for event " + eventShown.getEventId());
                        Toast.makeText(requireContext(), "The waiting list is full for this event.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    waitingList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            showWaiting();
                            updateView();
                            refreshTickets();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("waitingList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("waitingList DB Call", "Unable to refresh users", e);
                }
            });
        });

        leaveWaitingListButton = view.findViewById(R.id.event_details_leave_waitlist_button);
        leaveWaitingListButton.setOnClickListener(v -> {
            waitingList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    waitingList.removeUserFromDB(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            waitingList.popUser(user);
                            showJoin();
                            updateView();
                            refreshTickets();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("waitingList DB Call", "Unable to delete user from DB", e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("waitingList DB Call", "Unable to refresh users", e);
                }
            });
        });

        acceptInviteButton = view.findViewById(R.id.event_details_accept_invite_button);
        acceptInviteButton.setOnClickListener(v -> {
            acceptedList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    int acceptedListCapacity = acceptedList.getCapacity();
                    int acceptedListSize = waitingList.getSize();

                    if (acceptedListSize >= acceptedListCapacity) {
                        Toast.makeText(requireContext(), "The event is already full.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    acceptedList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            waitingList.popUser(user);
                            showAccepted();
                            updateView();
                            refreshTickets();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("acceptedList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("acceptedList DB Call", "Unable to refresh users", e);
                }
            });
        });

        declineInviteButton = view.findViewById(R.id.event_details_accept_invite_button);
        declineInviteButton = view.findViewById(R.id.event_details_decline_invite_button);
        declineInviteButton.setOnClickListener(v -> {
            declinedList.refresh(new StatusList.ListUpdateListener() {
                @Override
                public void onUpdate() {
                    declinedList.addUser(user, new StatusList.ListUpdateListener() {
                        @Override
                        public void onUpdate() {
                            waitingList.popUser(user);
                            showDeclined();
                            updateView();
                            refreshTickets();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("pendingList DB Call", "Unable to add user to DB", e);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("pendingList DB Call", "Unable to refresh users", e);
                }
            });
        });

        updateView();

        if (waitingList.userIsInList(user)) {
            showWaiting();
        } else if (pendingList.userIsInList(user)) {
            showPending();
        } else if (acceptedList.userIsInList(user)) {
            showAccepted();
        } else if (declinedList.userIsInList(user)) {
            showDeclined();
        } else {
            showJoin();
        }
    }

    private void updateView() {
        eventTitle.setText(eventShown.getName());

        String posterBase64 = eventShown.getImage();
        if (posterBase64 == null) {
            Log.d("PosterDebug", "poster string is null");
        } else {
            Log.d("PosterDebug", "poster prefix = "
                    + posterBase64.substring(0, Math.min(20, posterBase64.length())));
            Log.d("PosterDebug", "poster length = " + posterBase64.length());
        }

        Bitmap posterBitmap = decodePosterBase64(posterBase64);
        if (posterBitmap != null) {
            Log.d("PosterDebug", "bitmap decoded: "
                    + posterBitmap.getWidth() + "x" + posterBitmap.getHeight());
            eventPoster.setImageBitmap(posterBitmap);
        } else {
            Log.d("PosterDebug", "bitmap is null, showing placeholder");
            eventPoster.setImageResource(R.drawable.ic_image_placeholder);
        }

        eventCapacity.setText(fmt("Capacity:", String.valueOf(eventShown.getEventCapacity())));
        eventWaitingListCount.setText(fmt("Currently in Waiting List:", String.valueOf(waitingList.getSize())));
        eventStartDate.setText(fmt("Starts:", formatTimestamp(eventShown.getRegistrationStartTimestamp())));
        eventEndDate.setText(fmt("Ends:", formatTimestamp(eventShown.getRegistrationEndTimestamp())));
        eventDescription.setText(eventShown.getDescription());
    }

    private Bitmap decodePosterBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            Log.d("PosterDebug", "Base64 string empty");
            return null;
        }

        String normalized = base64.trim();

        try {
            byte[] bytes;
            try {
                bytes = Base64.decode(normalized, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                Log.d("PosterDebug", "NO_WRAP decode failed, trying DEFAULT");
                bytes = Base64.decode(normalized, Base64.DEFAULT);
            }

            Log.d("PosterDebug", "decoded byte length = " + bytes.length);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

            if (bitmap == null) {
                Log.d("PosterDebug", "BitmapFactory returned null");
            }

            return bitmap;
        } catch (Exception e) {
            Log.e("PosterDebug", "Failed to decode poster", e);
            return null;
        }
    }

    private String fmt(String header, String value) {
        return header + "\n" + value;
    }

    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return sdf.format(new Date(timestamp.toDate().getTime()));
    }

    private void showJoin() {
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        joinWaitingListButton.setVisibility(View.VISIBLE);
    }

    private void showWaiting() {
        joinWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        leaveWaitingListButton.setVisibility(View.VISIBLE);
    }

    private void showPending() {
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        eventInviteText.setVisibility(View.VISIBLE);
        acceptInviteButton.setVisibility(View.VISIBLE);
        declineInviteButton.setVisibility(View.VISIBLE);
    }

    private void showAccepted() {
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteDeclinedText.setVisibility(View.GONE);

        eventInviteAcceptedText.setVisibility(View.VISIBLE);
    }

    private void showDeclined() {
        joinWaitingListButton.setVisibility(View.GONE);
        leaveWaitingListButton.setVisibility(View.GONE);
        eventInviteText.setVisibility(View.GONE);
        acceptInviteButton.setVisibility(View.GONE);
        declineInviteButton.setVisibility(View.GONE);
        eventInviteAcceptedText.setVisibility(View.GONE);

        eventInviteDeclinedText.setVisibility(View.VISIBLE);
    }

    private void refreshTickets() {
        String preferredDeviceId = user == null ? null : user.getDeviceId();
        TicketDB.getInstance().refreshTickets(requireContext(), preferredDeviceId);
    }
}