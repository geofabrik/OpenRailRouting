package de.geofabrik.railway_routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BikeNetwork;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.FootNetwork;
import com.graphhopper.routing.ev.RouteNetwork;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;

public class RailFlagEncoderTest {

    private final BooleanEncodedValue railAccessEnc = VehicleAccess.create("car");

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

    private void initFromProperties(RailFlagEncoder e, PMap properties) {
        e.initFromProperties(properties);
    }

    private ReaderWay getYardWay() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway",  "rail");
        way.setTag("service",  "yard");
        return way;
    }

    private static RailFlagEncoder createEncoder(int maxPossibleSpeed) {
        return createEncoder(maxPossibleSpeed, 5);
    }

    private static RailFlagEncoder createEncoder(int maxPossibleSpeed, int speedFactor) {
        BooleanEncodedValue accessEnc = VehicleAccess.create("rail");
        DecimalEncodedValue speedEnc = VehicleSpeed.create("rail", 5, speedFactor, false);
        double max = speedEnc.getNextStorableValue(maxPossibleSpeed);
        RailFlagEncoder encoder = new RailFlagEncoder(accessEnc, speedEnc, "rail", null, max, new PMap());
        encoder.setSpeedCorrectionFactor(0.9);
        return encoder;
    }

    @Test
    public void testSpeed50vehicle50track() {
        RailFlagEncoder encoder = createEncoder(50);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 50), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 100), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 25), 0.0);
    }

    @Test
    public void testSpeed50vehicle100track() {
        RailFlagEncoder encoder = createEncoder(50);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "100");
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 50), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 100), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 25), 0.0);
    }

    @Test
    public void testSpeed100vehicle50track() {
        RailFlagEncoder encoder = createEncoder(100);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 50), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 100), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 25), 0.0);
    }

    /**
     * TGV test
     */
    @Test
    public void testSpeedTGV100track() {
        RailFlagEncoder encoderTGV = createEncoder(319, 11);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "100");
        assertEquals(100 * 0.9, encoderTGV.applyMaxSpeed(way, 100), 0.0);
        assertEquals(100 * 0.9, encoderTGV.applyMaxSpeed(way, 50), 0.0);
    }

    @Test
    public void testElectricalCompatibilityMultiAC() {
        RailFlagEncoder encoder = createEncoder(100);
        PMap properties = new PMap();
        properties.putObject("electrifiedValues", "contact_line");
        properties.putObject("acceptedVoltages", "25000;15000");
        properties.putObject("acceptedFrequencies", "250;16.7;16.67");
        properties.putObject("name", "test");
        encoder.initFromProperties(properties);
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
        RailFlagEncoder encoder = createEncoder(100);
        PMap properties = new PMap();
        properties.putObject("electrifiedValues", "");
        properties.putObject("name", "test");
        encoder.initFromProperties(properties);
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
        RailFlagEncoder e = createEncoder(100);
        PMap properties = new PMap();
        properties.putObject("name", "test");
        properties.putObject("acceptedGauges", "1435");
        e.initFromProperties(properties);
        ReaderWay way1 = getRailwayTrack();
        way1.setTag("gauge", "1435");
        assertTrue(e.hasCompatibleGauge(way1));
        ReaderWay way2 = getRailwayTrack();
        assertTrue(e.hasCompatibleGauge(way2));
    }

    @Test
    public void testGaugeAgnosticEncoder() {
        RailFlagEncoder e = createEncoder(100);
        e.setSpeedCorrectionFactor(0.9);
        PMap properties = new PMap();
        properties.putObject("electrifiedValues", "");
        properties.putObject("name", "test");
        e.initFromProperties(properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(e.hasCompatibleGauge(way1));
        ReaderWay way2 = getRailwayTrack();
        way2.setTag("gauge", "1000");
        assertTrue(e.hasCompatibleGauge(way2));
    }

    @Test
    public void testYardTrackOnAcceptingEncoder() {
        RailFlagEncoder e = createEncoder(100);
        PMap properties = new PMap();
        properties.putObject("electrifiedValues", "");
        properties.putObject("yardSpur", true);
        properties.putObject("name", "test");
        initFromProperties(e, properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertNotEquals(e.getAccess(way1), WayAccess.CAN_SKIP);
        ReaderWay way2 = getYardWay();
        assertNotEquals(e.getAccess(way2), WayAccess.CAN_SKIP);
    }

    @Test
    public void testYardTrackOnNonAcceptingEncoder() {
        RailFlagEncoder e = createEncoder(100);
        e.setSpeedCorrectionFactor(0.9);
        PMap properties = new PMap();
        properties.putObject("electrifiedValues", "");
        properties.putObject("yardSpur", false);
        properties.putObject("name", "test");
        initFromProperties(e, properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertNotEquals(e.getAccess(way1), WayAccess.CAN_SKIP);
        ReaderWay way2 = getYardWay();
        assertEquals(e.getAccess(way2), WayAccess.CAN_SKIP);
    }

    @Test
    public void testAcceptsTrack() {
        PMap properties = new PMap();
        properties.putObject("name", "tgv");
        properties.putObject("max_speed", 319);
        properties.putObject("speedFactor", 11);
        RailFlagEncoder e = createEncoder(319, 11);
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        assertEquals(e.getAccess(way), WayAccess.WAY);
    }
}
