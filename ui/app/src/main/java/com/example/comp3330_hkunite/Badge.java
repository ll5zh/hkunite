package com.example.comp3330_hkunite;

public class Badge {
    private String name;
    private String image; //i will use the image later

    public Badge(String name, String image) {
        this.name = name;
        this.image = image;
    }
    public String getName() {
        return name;
    }
    public String getImage() {
        return image;
    }
}