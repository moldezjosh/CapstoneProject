package com.capstoneproject.capstone;

/**
 * Created by moldezjosh on 3/28/2019.
 */

public class Report {
    private String Latitude;
    private String Longitude;
    private String Authenticity;
    private String Email;
    private String Url;

    public Report(String latitude, String longitude, String authenticity, String email, String url) {
        Latitude = latitude;
        Longitude = longitude;
        Authenticity = authenticity;
        Email = email;
        Url = url;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getAuthenticity() {
        return Authenticity;
    }

    public void setAuthenticity(String authenticity) {
        Authenticity = authenticity;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }
}

