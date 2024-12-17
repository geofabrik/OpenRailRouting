package de.geofabrik.railway_routing.parsers;

import java.util.Set;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.util.PMap;

public class RailAccessParser extends AbstractAccessParser {

    public static final String DEFAULT_NAME = "rail";
    private static final Set<String> ALLOWED_RAIL_TYPES = Set.of(
        "rail", "light_rail", "tram", "subway", "construction", "proposed", "funicular", "monorail",
    );

    public RailAccessParser(BooleanEncodedValue accessEnc) {
        super(accessEnc, TransportationMode.TRAIN);
    }

    public RailAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", DEFAULT_NAME)))
        );
    }

    public WayAccess getAccess(ReaderWay way) {
        String railway = way.getTag("railway");
        if (railway == null) {
            return WayAccess.CAN_SKIP;
        }
        if (ALLOWED_RAIL_TYPES.contains(railway)) {
            return WayAccess.WAY;
        }
        return WayAccess.CAN_SKIP;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        WayAccess access = getAccess(way);
        if (access.canSkip()) {
            return;
        }
        accessEnc.setBool(false, edgeId, edgeIntAccess, true);
        accessEnc.setBool(true, edgeId, edgeIntAccess, true);
    }
}
