package de.geofabrik.sncf_railway_routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.util.PMap;

import org.junit.Before;

public class RailFlagEncoderTest {
    
    private RailFlagEncoder encoder;
    
    private ReaderWay getElectrifiedWay(String electrified, String voltage, String frequency) {
        ReaderWay way = new ReaderWay(1);
        way.setTag("electrified", electrified);
        way.setTag("voltage", voltage);
        way.setTag("frequency", frequency);
        return way;
    }
    
    @Before
    public void setUp() {
        encoder = new RailFlagEncoder();
        encoder.setSpeedCorrectionFactor(0.9);
    }

    @Test
    public void testSpeed50vehicle50track() {
        encoder.setMaxPossibleSpeed(50);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "50");
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 50), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 100), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 25), 0.0);
    }

    @Test
    public void testSpeed50vehicle100track() {
        encoder.setMaxPossibleSpeed(50);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "100");
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 50), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 100), 0.0);
        assertEquals(50 * 0.9, encoder.applyMaxSpeed(way, 25), 0.0);
    }

    @Test
    public void testSpeed100vehicle50track() {
        encoder.setMaxPossibleSpeed(100);
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
        PMap properties = new PMap();
        properties.put("name", "tgv");
        properties.put("max_speed", 319);
        properties.put("speedFactor", 11);
        RailFlagEncoder encoderTGV = new RailFlagEncoder(properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("maxspeed", "100");
        assertEquals(100 * 0.9, encoderTGV.applyMaxSpeed(way, 100), 0.0);
        assertEquals(100 * 0.9, encoderTGV.applyMaxSpeed(way, 50), 0.0);
    }
    
    @Test
    public void testElectricalCompatibilityMultiAC() {
        PMap properties = new PMap();
        properties.put("electrifiedValues", "contact_line");
        properties.put("acceptedVoltages", "25000;15000");
        properties.put("acceptedFrequencies", "250;16.7;16.67");
        properties.put("name", "test");
        encoder.initFromProperties(properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way1));
        ReaderWay way2 = getElectrifiedWay("contact_line", "11000", "16.7");
        assertFalse(encoder.hasCompatibleElectricity(way2));
        ReaderWay way3 = getElectrifiedWay("contact_line", "1500", "0");
        assertFalse(encoder.hasCompatibleElectricity(way3));
        ReaderWay way4 = getElectrifiedWay("rail", "750", "0");
        assertFalse(encoder.hasCompatibleElectricity(way4));
        ReaderWay way5 = getElectrifiedWay("contact_line", "", "");
        assertTrue(encoder.hasCompatibleElectricity(way5));
        ReaderWay way6 = getElectrifiedWay("yes", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way6));
        ReaderWay way7 = getElectrifiedWay("no", "", "");
        assertFalse(encoder.hasCompatibleElectricity(way7));
        // tagging error but we should treat it safe
        ReaderWay way8 = getElectrifiedWay("no", "750", "0");
        assertFalse(encoder.hasCompatibleElectricity(way8));
    }
    
    @Test
    public void testElectricalCompatibilityDiesel() {
        PMap properties = new PMap();
        properties.put("electrifiedValues", "");
        properties.put("name", "test");
        encoder.initFromProperties(properties);
        ReaderWay way1 = getElectrifiedWay("contact_line", "15000", "16.7");
        assertTrue(encoder.hasCompatibleElectricity(way1));
        ReaderWay way2 = getElectrifiedWay("no", "", "");
        assertTrue(encoder.hasCompatibleElectricity(way2));
    }
}
