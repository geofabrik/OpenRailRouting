package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;

/**
 * Stores if the requested direction matches the directions provided by the `railway:preferred_direction=*` tag in OSM.
 */
public class PreferredDirection {

    public static String KEY = "preferred_direction";

    public static BooleanEncodedValue create() {
        return new SimpleBooleanEncodedValue(KEY, true);
    }
}
