package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.IntEncodedValue;

public class Frequency {
    public static final String KEY = "frequency";

    public static IntEncodedValue create() {
        return new IntEncodedValueImplWithFactor(KEY, 4, 5, false);
    }

}
