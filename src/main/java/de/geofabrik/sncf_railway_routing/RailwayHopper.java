package de.geofabrik.sncf_railway_routing;

import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.CmdArgs;

public class RailwayHopper extends GraphHopperOSM {
    public RailwayHopper(final CmdArgs args) {
        setEncodingManager(new EncodingManager(new RailFlagEncoder()));
    }
}
