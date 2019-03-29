package com.capstoneproject.capstone;

/**
 * Created by moldezjosh on 3/28/2019.
 */

public class Report {
    private String Latitude;
    private String Longitude;
    private String Email;
    private String Url;
    private String LocationType;
    private String Location;
    private String Detail;
    private String Product;

    public Report(String location, String latitude, String longitude, String locationType, String product, String detail, String email, String url) {
        Location = location;
        Latitude = latitude;
        Longitude = longitude;
        LocationType = locationType;
        Product = product;
        Detail = detail;
        Email = email;
        Url = url;
    }
    public String getLocation() {
        return Location;
    }

    public void setLocation(String location) {
        Location = location;
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

    public String getLocationType() {
        return LocationType;
    }

    public void setLocationType(String locationType) {
        LocationType = locationType;
    }

    public String getProduct() {
        return Product;
    }

    public void setProduct(String product) {
        Product = product;
    }

    public String getDetail() {
        return Detail;
    }

    public void setDetail(String detail) {
        Detail = detail;
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

