package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.RailwayService;

public class OSMRailwayServiceParser implements TagParser {
    
    protected final EnumEncodedValue<RailwayService> railwayServiceEnc;

    public OSMRailwayServiceParser(EnumEncodedValue<RailwayService> railwayServiceEnc) {
        this.railwayServiceEnc = railwayServiceEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay, IntsRef relationFlags) {
        String railwayClassTag = readerWay.getTag("service");
        if (railwayClassTag == null)
            return;
        RailwayService railwayClass = RailwayService.find(railwayClassTag);
        railwayServiceEnc.setEnum(false, edgeId, edgeIntAccess, railwayClass);
    }
}