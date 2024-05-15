package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.RailwayClass;

public class OSMRailwayClassParser implements TagParser {
    
    protected final EnumEncodedValue<RailwayClass> railwayClassEnc;

    public OSMRailwayClassParser(EnumEncodedValue<RailwayClass> roadClassEnc) {
        this.railwayClassEnc = roadClassEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay, IntsRef relationFlags) {
        String railwayClassTag = readerWay.getTag("highway");
        if (railwayClassTag == null)
            return;
        RailwayClass railwayClass = RailwayClass.find(railwayClassTag);
        railwayClassEnc.setEnum(false, edgeId, edgeIntAccess, railwayClass);
    }
}
