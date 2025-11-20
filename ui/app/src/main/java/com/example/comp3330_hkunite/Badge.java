package com.example.comp3330_hkunite;

public class Badge {
    private String name;
    private String imageUrl; //this holds image url

    public Badge(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}