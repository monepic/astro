package com.monepic.astro;

/**
 * 
 * @author monepic
 *
 */
public class Location {

    private final double latitude;
    private final double longitude;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override public String toString() {
        return String.format("{latitude:%f, longitude:%f}", latitude, longitude);
    }

    @Override public boolean equals(Object obj) {
        if(this == obj) { return true; }
        if (!(obj instanceof Location)) { return false; }
        Location that = (Location)obj;
        return that.getLatitude() == getLatitude() 
                && that.getLongitude() == getLongitude();
    }

    @Override public int hashCode() {
        int p = 101, hash = 1;
        hash = p * hash + Double.hashCode(latitude);
        hash = p * hash + Double.hashCode(longitude);
        return hash;
    }
}
