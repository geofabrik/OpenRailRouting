package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.MaxSpeed;

public class RailwayMaxSpeed extends MaxSpeed {

    public static DecimalEncodedValue create() {
        // more bits than parent class because we have a larger range of speeds to encode
        return new DecimalEncodedValueImpl(KEY, 8, 0, 2, false, true, true);
    }

}
