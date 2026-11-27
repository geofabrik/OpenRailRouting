package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;

/**
 * Whether an edge belongs to at least one OSM route relation with
 * {@code route=train}, i.e. a track used by a mapped passenger train service.
 * Custom models can prefer such tracks for passenger trains
 * ({@code "if": "!train_route", "multiply_by": ...}): the shortest path by
 * track geometry may run over lines without passenger traffic.
 */
public class TrainRoute {
    public static final String KEY = "train_route";

    public static BooleanEncodedValue create() {
        return new SimpleBooleanEncodedValue(KEY);
    }
}
