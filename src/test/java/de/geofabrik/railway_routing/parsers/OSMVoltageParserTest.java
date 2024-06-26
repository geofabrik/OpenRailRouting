package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.Voltage;

class OSMVoltageParserTest {

    @Test
    void voltage() {
        DecimalEncodedValue voltEnc = Voltage.create();
        voltEnc.init(new EncodedValue.InitializerConfig());
        OSMVoltageParser parser = new OSMVoltageParser(voltEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0.0, voltEnc.getDecimal(false, edgeId, edgeIntAccess), 10);
        way.setTag("voltage", "15000");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(15000.0, voltEnc.getDecimal(false, edgeId, edgeIntAccess), 10);

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("voltage", "750");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(750.0, voltEnc.getDecimal(false, edgeId, edgeIntAccess), 10);

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("voltage", "2400");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(2400.0, voltEnc.getDecimal(false, edgeId, edgeIntAccess), 10);

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0.0, voltEnc.getDecimal(false, edgeId, edgeIntAccess), 10);
    }

}