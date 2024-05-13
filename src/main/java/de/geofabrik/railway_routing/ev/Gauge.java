package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;

public class Gauge {
    public static final String KEY = "gauge";

    public static IntEncodedValue create() {
        return new IntEncodedValueImpl(KEY, 11, false);
    }

}