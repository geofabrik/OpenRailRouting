package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.RailwayMaxSpeed;

class RailMaxSpeedParserTest {
    private DecimalEncodedValue maxSpeedEnc;
    private OSMRailwayMaxSpeedParser parser;
    private EdgeIntAccess edgeIntAccess;
    private IntsRef relFlags;
    
    public RailMaxSpeedParserTest() {
        maxSpeedEnc = RailwayMaxSpeed.create();
        maxSpeedEnc.init(new EncodedValue.InitializerConfig());
        parser = new OSMRailwayMaxSpeedParser(maxSpeedEnc);
        edgeIntAccess = new ArrayEdgeIntAccess(1);
        relFlags = new IntsRef(2);
    }

    private void encodeAndAssertSpeeds(ReaderWay way, double expectedForward, double expectedBackward) {
        parser.handleWayTags(1, edgeIntAccess, way, relFlags);
        assertEquals(expectedForward, maxSpeedEnc.getDecimal(false, 1, edgeIntAccess), 0.5);
        assertEquals(expectedBackward, maxSpeedEnc.getDecimal(true, 1, edgeIntAccess), 0.5);
    }

    private void encodeAndAssertSpeeds(ReaderWay way, double expected) {
        encodeAndAssertSpeeds(way, expected, expected);
    }

    @Test
    public void testParser() {
        ReaderWay way = new ReaderWay(5);
        way.setTag("railway", "rail");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "80");
        encodeAndAssertSpeeds(way, 80.0);
        way.setTag("maxspeed", "140");
        encodeAndAssertSpeeds(way, 140.0);
        way.setTag("maxspeed", "160");
        encodeAndAssertSpeeds(way, 160.0);
        way.setTag("maxspeed", "210");
        encodeAndAssertSpeeds(way, 210.0);
        way.setTag("maxspeed", "280");
        encodeAndAssertSpeeds(way, 280.0);
        way.setTag("maxspeed", "320");
        encodeAndAssertSpeeds(way, 320.0);
        way.setTag("maxspeed", "350");
        encodeAndAssertSpeeds(way, 350.0);
        way.setTag("maxspeed:backward", "120");
        encodeAndAssertSpeeds(way, 350.0, 120.0);
        way.setTag("maxspeed:forward", "180");
        encodeAndAssertSpeeds(way, 180.0, 120.0);
        way.removeTag("maxspeed");
        encodeAndAssertSpeeds(way, 180.0, 120.0);
        way.clearTags();
        way.setTag("maxspeed", "none");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "-1");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "-60");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "125mph");
        encodeAndAssertSpeeds(way, 202);
        way.setTag("maxspeed", "40abc");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
    }

    @Test
    public void testBidirectionalSpeed() {
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        encodeAndAssertSpeeds(way, MaxSpeed.MAXSPEED_MISSING);
        way.setTag("maxspeed", "80");
        encodeAndAssertSpeeds(way, 80);
        way.setTag("maxspeed:forward", "140");
        encodeAndAssertSpeeds(way, 140, 80);
        way.setTag("maxspeed:backward", "50");
        encodeAndAssertSpeeds(way, 140, 50);
        way.clearTags();
        way.setTag("railway", "rail");
        way.setTag("maxspeed", "200");
        encodeAndAssertSpeeds(way, 200);
        way.setTag("maxspeed:backward", "70");
        encodeAndAssertSpeeds(way, 200, 70);
        way.clearTags();
        way.setTag("railway", "rail");
        way.setTag("maxspeed", "320");
        encodeAndAssertSpeeds(way, 320);
    }
}
