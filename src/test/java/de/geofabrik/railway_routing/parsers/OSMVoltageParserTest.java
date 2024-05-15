package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.Voltage;

class OSMVoltageParserTest {

    @Test
    void voltage() {
        IntEncodedValue voltEnc = Voltage.create();
        voltEnc.init(new EncodedValue.InitializerConfig());
        OSMVoltageParser parser = new OSMVoltageParser(voltEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        way.setTag("voltage", "15000");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(15000, voltEnc.getInt(false, edgeId, edgeIntAccess), 10);

        // different direction
        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("voltage", "750");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(750, voltEnc.getInt(false, edgeId, edgeIntAccess), 10);
    }

}