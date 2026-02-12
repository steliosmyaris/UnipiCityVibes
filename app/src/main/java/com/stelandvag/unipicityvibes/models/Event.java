package com.stelandvag.unipicityvibes.models;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private String category;
    private long dateTime;
    private double price;
    private String venue;
    private double latitude;
    private double longitude;
    private int capacity;
    private int bookedSeats;
    private String imageUrl;
    //Empty Constructor
    public Event() {}
    // Constructor
    public Event(String eventId, String title, String description, String category,
                 long dateTime, double price, String venue, double latitude,
                 double longitude, int capacity, int bookedSeats, String imageUrl) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.dateTime = dateTime;
        this.price = price;
        this.venue = venue;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.bookedSeats = bookedSeats;
        this.imageUrl = imageUrl;
    }

    // Setters Getters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getDateTime() { return dateTime; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }


    // Check availability
    public int getAvailableSeats() {
        return capacity - bookedSeats;
    }
}
