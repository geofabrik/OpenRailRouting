package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.Frequency;

class OSMFrequencyParserTest {

    @Test
    void frequency() {
        DecimalEncodedValue freqencyEnc = Frequency.create();
        freqencyEnc.init(new EncodedValue.InitializerConfig());
        OSMFrequencyParser parser = new OSMFrequencyParser(freqencyEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        way.setTag("frequency", "16.7");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(16.7, freqencyEnc.getDecimal(false, edgeId, edgeIntAccess), 2.5);

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        way.setTag("frequency", "0");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0.0, freqencyEnc.getDecimal(false, edgeId, edgeIntAccess), 2.5);

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(0.0, freqencyEnc.getDecimal(false, edgeId, edgeIntAccess), 2.5);
    }

}