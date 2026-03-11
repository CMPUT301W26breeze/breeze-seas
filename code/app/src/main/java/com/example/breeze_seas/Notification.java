package com.example.breeze_seas;

import com.google.firebase.Timestamp;

public class Notification {

    private String notificationId;
    private NotificationType type;
    private String content; // should be an empty string if  not an announcement
    private String eventId;
    private Timestamp sentAt;

    public Notification() {
        this.notificationId = null;
        this.type = null;
        this.content = null;
        this.eventId = null;
        this.sentAt = null;
    }

    public Notification(String notificationId, NotificationType type, String content, String eventId, Timestamp sentAt) {
        this.notificationId = notificationId;
        this.type = type;
        this.content = content;
        this.eventId = eventId;
        this.sentAt = sentAt;
    }

    public Notification(NotificationType type, String content) {
        this.notificationId = null;
        this.type = type;
        this.content = content;
        this.eventId = null;
        this.sentAt = null;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public String getDisplayMessage() {
        switch (type) {
            case WIN:
                return "Congratulations! You won the lottery for " + content + "!";
            case LOSS:
                return "We're sorry, but you were not selected for " + content + ".";
            case ANNOUNCEMENT_SELECTED:
            case ANNOUNCEMENT_WAITLIST:
            case ANNOUNCEMENT_CANCELLED:
                return content;
            default:
                return content;
        }
    }




}
