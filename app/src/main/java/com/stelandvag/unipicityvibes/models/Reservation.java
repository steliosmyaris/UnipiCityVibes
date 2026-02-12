package com.stelandvag.unipicityvibes.models;

public class Reservation {
    private String reservationId;
    private String eventId;
    private String userId;
    private String userName;
    private long timestamp;

    // Empty  constructor
    public Reservation() {}

    public Reservation(String reservationId, String eventId, String userId,
                       String userName, long timestamp) {
        this.reservationId = reservationId;
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.timestamp = timestamp;
    }

    // Setters Getters
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
