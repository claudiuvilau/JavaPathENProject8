package com.openclassrooms.tourguide.model;

public class AttractionsNearUser {

    private String attractionName;
    private double latitudeAttraction;
    private double longitudeAttraction;
    private double latitudeUser;
    private double longitudeUser;
    private double distanceMiles;
    private int rewardPoints;

    public AttractionsNearUser() {
    }

    public AttractionsNearUser(String attractionName, double latitudeAttraction, double longitudeAttraction,
            double latitudeUser, double longitudeUser, double distanceMiles, int rewardPoints) {
        this.attractionName = attractionName;
        this.latitudeAttraction = latitudeAttraction;
        this.longitudeAttraction = longitudeAttraction;
        this.latitudeUser = latitudeUser;
        this.longitudeUser = longitudeUser;
        this.distanceMiles = distanceMiles;
        this.rewardPoints = rewardPoints;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public double getLatitudeAttraction() {
        return latitudeAttraction;
    }

    public void setLatitudeAttraction(double latitudeAttraction) {
        this.latitudeAttraction = latitudeAttraction;
    }

    public double getLongitudeAttraction() {
        return longitudeAttraction;
    }

    public void setLongitudeAttraction(double longitudeAttraction) {
        this.longitudeAttraction = longitudeAttraction;
    }

    public double getLatitudeUser() {
        return latitudeUser;
    }

    public void setLatitudeUser(double latitudeUser) {
        this.latitudeUser = latitudeUser;
    }

    public double getLongitudeUser() {
        return longitudeUser;
    }

    public void setLongitudeUser(double longitudeUser) {
        this.longitudeUser = longitudeUser;
    }

    public double getDistanceMiles() {
        return distanceMiles;
    }

    public void setDistanceMiles(double distanceMiles) {
        this.distanceMiles = distanceMiles;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

}
