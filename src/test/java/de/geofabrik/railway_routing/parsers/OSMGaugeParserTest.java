package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.Gauge;

class OSMGaugeParserTest {

    @Test
    void gauge() {
        IntEncodedValue gaugeEnc = Gauge.create();
        gaugeEnc.init(new EncodedValue.InitializerConfig());
        OSMGaugeParser parser = new OSMGaugeParser(gaugeEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0, gaugeEnc.getInt(false, edgeId, edgeIntAccess));
        way.setTag("gauge", "1435");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(1435, gaugeEnc.getInt(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("gauge", "1000");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(1000, gaugeEnc.getInt(false, edgeId, edgeIntAccess));

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            EdgeIntAccess intAccess = new ArrayEdgeIntAccess(1);
            ReaderWay w = new ReaderWay(29L);
            w.setTag("railway", "rail");
            w.setTag("gauge", "1435;1000");
            parser.handleWayTags(edgeId, intAccess, w, new IntsRef(2));
            });
        assertTrue(e.getMessage().contains("way has gauge=* tag with multiple values. This should have been cleaned by OSMReader class or its children."), e.getMessage());

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("gauge", "2000");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(2000, gaugeEnc.getInt(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("gauge", "6000");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0, gaugeEnc.getInt(false, edgeId, edgeIntAccess));
    }

}