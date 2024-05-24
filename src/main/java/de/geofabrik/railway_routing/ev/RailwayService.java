package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.util.Helper;

public enum RailwayService {
    NONE, SIDING, YARD, SPUR, CROSSOVER;

    public static final String KEY = "railway_service";

    public static EnumEncodedValue<RailwayService> create() {
        return new EnumEncodedValue<>(RailwayService.KEY, RailwayService.class);
    }

    @Override
    public String toString() {
        return Helper.toLowerCase(super.toString());
    }

    public static RailwayService find(String name) {
        if (name == null || name.isEmpty())
            return NONE;
        try {
            return RailwayService.valueOf(Helper.toUpperCase(name));
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}