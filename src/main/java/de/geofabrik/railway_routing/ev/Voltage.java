package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValueImpl;

public class Voltage {
    public static final String KEY = "voltage";

    public static DecimalEncodedValue create() {
        return new DecimalEncodedValueImpl(KEY, 9, 50, false);
    }
}
