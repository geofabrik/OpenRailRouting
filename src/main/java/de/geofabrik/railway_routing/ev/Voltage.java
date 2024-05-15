package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.IntEncodedValue;

public class Voltage {
    public static final String KEY = "voltage";

    public static IntEncodedValue create() {
        return new IntEncodedValueImplWithFactor(KEY, 9, 50, false);
    }
}
