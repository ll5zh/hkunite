package com.example.comp3330_hkunite;

public class Event {
    private int eid;
    private String title;
    private String description;
    private String imageUrl;
    private String date;

    public Event(int eid, String title, String description, String imageUrl, String date) {
        this.eid = eid;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.date = date;
    }

    public int getEid() { return eid; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getDate() { return date; }
}

