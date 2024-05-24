package de.geofabrik.railway_routing.reader;

import static com.graphhopper.routing.util.TransportationMode.CAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.OSMRoadAccessParser;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

public class OSMRailwayReaderTest {
    private OSMRailwayReader reader;
    private List<ReaderWay> ways;
    static private PointList pointList = new PointList();
    static private List<Map<String, Object>> nodeTags = new ArrayList<Map<String, Object>>();
    
    public OSMRailwayReaderTest() {
        EnumEncodedValue<RoadAccess> roadAccessEnc = RoadAccess.create();
        EncodingManager em = new EncodingManager.Builder().add(roadAccessEnc).build();
        RailwayOSMParsers osmParsers = new RailwayOSMParsers();
        osmParsers.addWayTagParser(new OSMRoadAccessParser(roadAccessEnc, OSMRoadAccessParser.toOSMRestrictions(CAR)));
        BaseGraph graph = new BaseGraph.Builder(em).create();
        reader = new OSMRailwayReader(graph, osmParsers, new OSMReaderConfig());
        ways = new ArrayList<ReaderWay>(5);
        pointList.add(new GHPoint(49.0, 9.0));
        pointList.add(new GHPoint(49.000001, 9.000001));
        nodeTags.add(new HashMap<String, Object>());
        nodeTags.add(new HashMap<String, Object>());
    }
    
    @BeforeEach
    public void prepare() {
        ways.clear();
    }
    
    private void wayCallback(int fromIndex, int toIndex, PointList pointList, ReaderWay way,
            List<Map<String, Object>> nodeTags) {
        ways.add(way);
    }
    
    private boolean wayHasOrMissesTag(ReaderWay way, String key, String value) {
        return key == null || (value == null && way.getTag(key) == null) || way.hasTag(key, value);
    }
    
    private boolean wayInList(String key1, String value1, String key2, String value2, String key3, String value3) {
        for (ReaderWay way : ways) {
            if (wayHasOrMissesTag(way, key1, value1) && wayHasOrMissesTag(way, key2, value2)
                    && wayHasOrMissesTag(way, key3, value3)) {
                return true;
            }
        }
        return false;
    }
    
    @Test
    public void testElectricalCompatibilityMultiAC() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("electrified", "contact_line");
        way.setTag("voltage", "25000;15000");
        way.setTag("frequency", "50;16.7");
        reader.duplicateAndAddEdge(0, 1, pointList, way, nodeTags, this::wayCallback);
        assertEquals(2, ways.size());
        assertTrue(wayInList("electrified", "contact_line", "voltage", "15000", "frequency", "16.7"));
        assertTrue(wayInList("electrified", "contact_line", "voltage", "25000", "frequency", "50"));
    }
    
    @Test
    public void testElectricalCompatibilityMultiDC() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("electrified", "contact_line");
        way.setTag("voltage", "1500;3000");
        way.setTag("frequency", "0");
        reader.duplicateAndAddEdge(0, 1, pointList, way, nodeTags, this::wayCallback);
        assertEquals(2, ways.size());
        assertTrue(wayInList("electrified", "contact_line", "voltage", "1500", "frequency", "0"));
        assertTrue(wayInList("electrified", "contact_line", "voltage", "3000", "frequency", "0"));
    }
    
    @Test
    public void testMultiElectricity() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("electrified", "contact_line;rail");
        way.setTag("voltage", "15000;750");
        way.setTag("frequency", "16.7;0");
        reader.duplicateAndAddEdge(0, 1, pointList, way, nodeTags, this::wayCallback);
        assertEquals(2, ways.size());
        assertTrue(wayInList("electrified", "contact_line", "voltage", "15000", "frequency", "16.7"));
        assertTrue(wayInList("electrified", "rail", "voltage", "750", "frequency", "0"));
    }
    
    @Test
    public void testIgnoreElectricity() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("electrified", "contact_line");
        way.setTag("voltage", "1500;3000;15000");
        way.setTag("frequency", "16.7;0");
        reader.duplicateAndAddEdge(0, 1, pointList, way, nodeTags, this::wayCallback);
        assertEquals(1, ways.size());
        assertTrue(wayInList("electrified", "contact_line", "voltage", null, "frequency", null));
    }
    
    @Test
    public void testMultiGauge() {
        ReaderWay way = new ReaderWay(1);
        way.setTag("gauge", "1435;1000");
        reader.duplicateAndAddEdge(0, 1, pointList, way, nodeTags, this::wayCallback);
        assertEquals(2, ways.size());
        assertTrue(wayInList("gauge", "1435", "voltage", null, "frequency", null));
        assertTrue(wayInList("gauge", "1000", "voltage", null, "frequency", null));
    }
    
    private void assertList(String[] expected, List<String> parsed) {
        assertEquals(expected.length, parsed.size());
        for (String e : Arrays.asList(expected)) {
            assertTrue(parsed.contains(e));
        }
    }
    
    @Test
    public void testSplitValue() {
        String value = "1500;15000";
        String[] expected1 = {"1500", "15000"};
        assertList(expected1, OSMRailwayReader.tagValueToList(value));
        value = null;
        String[] expected2 = {null};
        List<String> parsed = OSMRailwayReader.tagValueToList(value);
        assertEquals(1, parsed.size());
        assertNull(parsed.get(0));
    }
}
