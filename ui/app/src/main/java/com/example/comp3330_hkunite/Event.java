package com.example.comp3330_hkunite;

public class Event {
    private int eid;
    private String title;
    private String description;
    private String imageUrl;
    private String date;

    private int cid;
    private String categoryName;
    private String ownerUsername;
    private String location;


    public Event(int eid, String title, String description, String imageUrl, String date, int cid, String categoryName, String ownerUsername, String location) {
        this.eid = eid;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.date = date;
        this.cid = cid;
        this.categoryName = categoryName;
        this.ownerUsername = ownerUsername;
        this.location = location;
    }



    public int getCid() { return cid; }
    public String getCategoryName() { return categoryName; }

    public String getOwnerUsername() {
        return ownerUsername;
    }
    public int getEid() { return eid; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getDate() { return date; }
    public String getLocation() { return location; }
}

