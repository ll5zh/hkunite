package com.example.comp3330_hkunite;

public class User {
    private int uid;
    private String name;
    private String imageUrl;
    private boolean selected; // NEW

    public User(int uid, String name, String imageUrl) {
        this.uid = uid;
        this.name = name;
        this.imageUrl = imageUrl;
        this.selected = false;
    }

    public int getUid() { return uid; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}


