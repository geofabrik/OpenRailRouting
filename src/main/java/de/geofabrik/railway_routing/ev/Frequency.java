package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValueImpl;

public class Frequency {
    public static final String KEY = "frequency";

    public static DecimalEncodedValue create() {
        return new DecimalEncodedValueImpl(KEY, 5, 2.5, false);
    }

}
