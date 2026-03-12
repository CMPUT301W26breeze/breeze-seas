package com.example.breeze_seas;

import com.google.firebase.Timestamp;

public class Notification {

    private String notificationId;
    private NotificationType type;
    private String content; // should be an empty string if  not an announcement
    private String eventId;
    private String eventName;
    private String userId;
    private Timestamp sentAt;

    public Notification() {
        this.notificationId = null;
        this.type = null;
        this.content = null;
        this.eventId = null;
        this.eventName = null;
        this.userId = null;
        this.sentAt = null;
    }

    public Notification( NotificationType type,
                        String content, String eventId,String eventName,
                        String userId) {
        this.notificationId = null;
        this.type = type;
        this.content = content;
        this.eventId = eventId;
        this.eventName = eventName;
        this.userId = userId;
        this.sentAt = null;
    }

    public Notification(NotificationType type, String content, Timestamp sentAt) {
        this.notificationId = null;
        this.type = type;
        this.content = content;
        this.eventId = null;
        this.eventName = null;
        this.userId = null;
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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayMessage() {
        switch (type) {
            case WIN:
                return "Congratulations! You won the lottery for " + eventName + "!";
            case LOSS:
                return "We're sorry, but you were not selected for " + eventName + ".";
            case ANNOUNCEMENT_SELECTED:
            case ANNOUNCEMENT_WAITLIST:
            case ANNOUNCEMENT_CANCELLED:
                return content;
            default:
                return content;
        }
    }




}
