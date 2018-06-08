package de.geofabrik.railway_routing;

import java.io.Serializable;

import com.graphhopper.util.GPXEntry;
import com.opencsv.bean.CsvBindByName;

public class InputCSVEntry {

    @CsvBindByName
    private double latitude;

    @CsvBindByName
    private double longitude;
    
    public InputCSVEntry() {
    }
    
    public InputCSVEntry(double lat, double lon) {
        super();
        latitude = lat;
        longitude = lon;
    }

    public void setLatitude(double lat) {
        this.latitude = lat;
    }

    public void setLongitude(double lon) {
        this.longitude = lon;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public GPXEntry toGPXEntry(long milliseconds) {
        return new GPXEntry(latitude, longitude, milliseconds);
    }
}
