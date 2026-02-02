package de.geofabrik.railway_routing.parsers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

public class RailSpeedParserTest {
    
    DecimalEncodedValue speedEnc;
    RailAverageSpeedParser parser;
    
    public RailSpeedParserTest() {
        parser = createSpeedParser(createEncodingManager(), new PMap());
    }

    private static RailAverageSpeedParser createSpeedParser(EncodedValueLookup lookup, PMap properties) {
        return new RailAverageSpeedParser(lookup, properties);
    }

    private EncodingManager createEncodingManager() {
        speedEnc = VehicleSpeed.create("rail", 6, 5, true);
        return new EncodingManager.Builder()
                .add(VehicleAccess.create("rail"))
                .add(speedEnc)
                .add(FerrySpeed.create())
                .addTurnCostEncodedValue(TurnCost.create("rail", 1))
                .build();
    }

    private void assertSpeeds(ReaderWay way, double expectedForward, double expectedBackward) {
        assertEquals(expectedForward, OSMRailwayMaxSpeedParser.parseMaxSpeed(way, false), 0.5);
        assertEquals(expectedBackward, OSMRailwayMaxSpeedParser.parseMaxSpeed(way, true), 0.5);
    }

    private void assertSpeeds(ReaderWay way, double expected) {
        assertSpeeds(way, expected, expected);
    }

    @Test
    public void testParser() {
        ReaderWay way = new ReaderWay(5);
        way.setTag("railway", "rail");
        assertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "80");
        assertSpeeds(way, 80.0);
        way.setTag("maxspeed", "140");
        assertSpeeds(way, 140.0);
        way.setTag("maxspeed", "160");
        assertSpeeds(way, 160.0);
        way.setTag("maxspeed", "210");
        assertSpeeds(way, 210.0);
        way.setTag("maxspeed", "280");
        assertSpeeds(way, 280.0);
        way.setTag("maxspeed", "320");
        assertSpeeds(way, 320.0);
        way.setTag("maxspeed", "350");
        assertSpeeds(way, 350.0);
        way.setTag("maxspeed:backward", "120");
        assertSpeeds(way, 350.0, 120.0);
        way.setTag("maxspeed:forward", "180");
        assertSpeeds(way, 180.0, 120.0);
        way.removeTag("maxspeed");
        assertSpeeds(way, 180.0, 120.0);
        way.clearTags();
        way.setTag("maxspeed", "none");
        assertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "-1");
        assertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "-60");
        assertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "125mph");
        assertSpeeds(way, 201.168);
        way.setTag("maxspeed", "40abc");
        assertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
    }

    @Test
    public void testBidirectionalSpeed() {
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(25.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(25.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed", "80");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(70.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(70.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed:forward", "140");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(125.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(70.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed:backward", "50");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(125.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(45.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.clearTags();
        way.setTag("railway", "rail");
        way.setTag("maxspeed", "200");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(180.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(180.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed:backward", "70");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(180.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(65.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
    }
}
