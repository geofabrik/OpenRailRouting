package de.geofabrik.railway_routing.ev;

import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.util.Helper;

public enum Electrified {
    UNSET, OTHER, CONTACT_LINE, RAIL, NO;

    public static final String KEY = "electrified";

    public static EnumEncodedValue<Electrified> create() {
        return new EnumEncodedValue<>(Electrified.KEY, Electrified.class);
    }

    @Override
    public String toString() {
        return Helper.toLowerCase(super.toString());
    }

    public static Electrified find(String name) {
        if (name == null || name.isEmpty())
            return OTHER;
        try {
            return Electrified.valueOf(Helper.toUpperCase(name));
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}