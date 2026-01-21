package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.util.parsers.OSMRoadAccessParser;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.util.PMap;

public class RailAccessParser extends AbstractAccessParser {

    public static final String DEFAULT_NAME = "rail";

    public RailAccessParser(BooleanEncodedValue accessEnc) {
        super(accessEnc, OSMRoadAccessParser.toOSMRestrictions(TransportationMode.TRAIN));
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
        if (railway.equals("rail") || railway.equals("light_rail") || railway.equals("tram") || railway.equals("subway") || railway.equals("narrow_gauge")) {
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
