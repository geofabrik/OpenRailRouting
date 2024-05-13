package de.geofabrik.railway_routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;

public class RailFlagEncoderTest {

    private static final String carName = "rail";

    private EncodingManager createEncodingManager() {
        return createEncodingManager(5, 11, true);
    }

    private EncodingManager createEncodingManager(int bits, int factor, boolean twoDirections) {
        return new EncodingManager.Builder()
                .add(VehicleAccess.create(carName))
                .add(VehicleSpeed.create(carName, bits, factor, twoDirections))
                .add(FerrySpeed.create())
                .addTurnCostEncodedValue(TurnCost.create(carName, 1))
                .build();
    }


    private static ReaderWay getRailwayTrack() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        return way;
    }

    private static ReaderWay getElectrifiedWay(String electrified, String voltage, String frequency) {
        ReaderWay way = getRailwayTrack();
        if (electrified != null) {
            way.setTag("electrified", electrified);
        }
        if (voltage != null) {
            way.setTag("voltage", voltage);
        }
        if (frequency != null) {
            way.setTag("frequency", frequency);
        }
        return way;
    }

    private ReaderWay getYardWay() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway",  "rail");
        way.setTag("service",  "yard");
        return way;
    }

    private static RailAccessParser createAccessParser(EncodedValueLookup lookup, PMap properties) {
        return new RailAccessParser(lookup, properties);
    }

    private static RailAverageSpeedParser createAvgSpeedParser(EncodedValueLookup lookup, PMap properties) {
        return new RailAverageSpeedParser(lookup, properties);
    }

    private RailAverageSpeedParser createSpeedParser(int bits, int factor) {
        EncodingManager em = createEncodingManager(bits, factor, false);
        return createAvgSpeedParser(em, new PMap());
    }

    private double encodeSpeed(int bits, int factor, ReaderWay way) {
        EncodingManager em = createEncodingManager(bits, factor, false);
        final RailAverageSpeedParser speedParser = createAvgSpeedParser(em, new PMap());
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(em.getIntsForFlags());
        int edgeId = 0;
        speedParser.handleWayTags(edgeId, edgeIntAccess, way);
        return speedParser.getAverageSpeedEnc().getDecimal(false, edgeId, edgeIntAccess);
    }


    @Test
    public void testNarrowGauge() {
        PMap properties = new PMap();
        properties.putObject("electrified", "");
        properties.putObject("railway", "rail;narrow_gauge");
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "narrow_gauge");
        assertEquals(e.getAccess(way), WayAccess.WAY);
    }

    @Test
    public void testApplyMaxSpeed() {
        RailAverageSpeedParser e = createSpeedParser(3, 7);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, e.applyMaxSpeed(way, 50), 4.0);
        assertEquals(50 * 0.9, e.applyMaxSpeed(way, 100), 4.0);
        assertEquals(50 * 0.9, e.applyMaxSpeed(way, 25), 4.0);
    }

    @Test
    public void testSpeedEncoderMax49() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, encodeSpeed(3, 7, way), 4.0);
        way.setTag("maxspeed", "100");
        assertEquals(50 * 0.9, encodeSpeed(3, 7, way), 4.0);
        way.setTag("maxspeed", "25");
        assertEquals(25 * 0.9, encodeSpeed(3, 7, way), 4.0);
    }

    @Test
    public void testSpeedEncoderMax105() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, encodeSpeed(4, 7, way), 4.0);
        way.setTag("maxspeed", "100");
        assertEquals(100 * 0.9, encodeSpeed(4, 7, way), 4.0);
        way.setTag("maxspeed", "25");
        assertEquals(25 * 0.9, encodeSpeed(4, 7, way), 4.0);
    }

    @Test
    public void testLowSpeedTrack() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "5");
        assertEquals(5, encodeSpeed(5, 10, way), 5.0);
    }

    @Test
    public void testElectricalCompatibilityMultiAC() {
        PMap properties = new PMap();
        properties.putObject("electrified", "contact_line");
        properties.putObject("voltages", "25000;15000");
        properties.putObject("frequencies", "250;16.7;16.67");
        RailAccessParser encoder = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way1));
        ReaderWay way2 = getElectrifiedWay("contact_line", "11000", "16.7");
        assertFalse(encoder.hasCompatibleElectricity(way2));
        ReaderWay way3 = getElectrifiedWay("contact_line", "1500", "0");
        assertFalse(encoder.hasCompatibleElectricity(way3));
        ReaderWay way4 = getElectrifiedWay("rail", "750", "0");
        assertFalse(encoder.hasCompatibleElectricity(way4));
        ReaderWay way5 = getElectrifiedWay("contact_line", null, null);
        assertTrue(encoder.hasCompatibleElectricity(way5));
        ReaderWay way6 = getElectrifiedWay("yes", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way6));
        ReaderWay way7 = getElectrifiedWay("no", null, null);
        assertFalse(encoder.hasCompatibleElectricity(way7));
        // tagging error but we should treat it safe
        ReaderWay way8 = getElectrifiedWay("no", "750", "0");
        assertFalse(encoder.hasCompatibleElectricity(way8));
        // electrified not set should be treated as accessible
        // (siding and yard tracks often lack these tags)
        ReaderWay way9 = getElectrifiedWay(null, null, null);
        assertTrue(encoder.hasCompatibleElectricity(way9));
    }

    @Test
    public void testElectricalCompatibilityDiesel() {
        PMap properties = new PMap();
        properties.putObject("electrified", "");
        RailAccessParser encoder = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way1));
        ReaderWay way2 = getElectrifiedWay("no", null, null);
        assertTrue(encoder.hasCompatibleElectricity(way2));
        // electrified not set should be treated as accessible
        // (siding and yard tracks often lack these tags)
        ReaderWay way3 = getElectrifiedWay(null, null, null);
        assertTrue(encoder.hasCompatibleElectricity(way3));
    }

    @Test
    public void testGaugeSensitiveEncoder() {
        PMap properties = new PMap();
        properties.putObject("gauges", "1435");
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getRailwayTrack();
        way1.setTag("gauge", "1435");
        assertTrue(e.hasCompatibleGauge(way1));
        ReaderWay way2 = getRailwayTrack();
        assertTrue(e.hasCompatibleGauge(way2));
    }

    @Test
    public void testGaugeAgnosticEncoder() {
        PMap properties = new PMap();
        properties.putObject("electrified", "");
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(e.hasCompatibleGauge(way1));
        ReaderWay way2 = getRailwayTrack();
        way2.setTag("gauge", "1000");
        assertTrue(e.hasCompatibleGauge(way2));
    }

    @Test
    public void testYardTrackOnAcceptingEncoder() {
        PMap properties = new PMap();
        properties.putObject("electrified", "");
        properties.putObject("yardSpur", true);
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertNotEquals(e.getAccess(way1), WayAccess.CAN_SKIP);
        ReaderWay way2 = getYardWay();
        assertNotEquals(e.getAccess(way2), WayAccess.CAN_SKIP);
    }

    @Test
    public void testYardTrackOnNonAcceptingEncoder() {
        PMap properties = new PMap();
        properties.putObject("electrified", "");
        properties.putObject("yardSpur", false);
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertNotEquals(e.getAccess(way1), WayAccess.CAN_SKIP);
        ReaderWay way2 = getYardWay();
        assertEquals(e.getAccess(way2), WayAccess.CAN_SKIP);
    }

    @Test
    public void testAcceptsTrack() {
        PMap properties = new PMap();
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        assertEquals(e.getAccess(way), WayAccess.WAY);
    }

    @Test
    public void testRejectRoad() {
        PMap properties = new PMap();
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "primary");
        assertEquals(WayAccess.CAN_SKIP, e.getAccess(way));
        way.setTag("railway", "rail");
        assertEquals(WayAccess.WAY, e.getAccess(way));
    }
}
