package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.RailwayService;

class OSMRailwayServiceParserTest {

    @Test
    void railwayService() {
        EnumEncodedValue<RailwayService> classEnc = RailwayService.create();
        classEnc.init(new EncodedValue.InitializerConfig());
        OSMRailwayServiceParser parser = new OSMRailwayServiceParser(classEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("service", "siding");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.SIDING, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("railway", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.SIDING, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("service", "invalid");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.NONE, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("service", "siding;yard");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.NONE, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("service", "spur");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.SPUR, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("service", "crossover");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.CROSSOVER, classEnc.getEnum(false, edgeId, edgeIntAccess));

        way.setTag("railway", "tram");
        way.setTag("service", "yard");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayService.YARD, classEnc.getEnum(false, edgeId, edgeIntAccess));
    }

}