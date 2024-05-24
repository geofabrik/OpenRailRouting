package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.RailwayClass;

class OSMRailwayClassParserTest {

    @Test
    void railwayClass() {
        EnumEncodedValue<RailwayClass> classEnc = RailwayClass.create();
        classEnc.init(new EncodedValue.InitializerConfig());
        OSMRailwayClassParser parser = new OSMRailwayClassParser(classEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("highway", "primary");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.OTHER, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("railway", "disused");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.OTHER, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("railway", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.RAIL, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "tram");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.TRAM, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "light_rail");
        way.setTag("voltage", "2400");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.LIGHT_RAIL, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "subway");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.SUBWAY, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "narrow_gauge");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.NARROW_GAUGE, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "funicular");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.FUNICULAR, classEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail;tram");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(RailwayClass.OTHER, classEnc.getEnum(false, edgeId, edgeIntAccess));
    }

}