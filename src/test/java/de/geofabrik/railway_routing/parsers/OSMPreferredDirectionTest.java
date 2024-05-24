package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.storage.IntsRef;
import de.geofabrik.railway_routing.ev.PreferredDirection;

class OSMPreferredDirectionTest {

    @Test
    void preferredDirection() {
        BooleanEncodedValue prefDirEnc = PreferredDirection.create();
        prefDirEnc.init(new EncodedValue.InitializerConfig());
        OSMPreferredDirectionParser parser = new OSMPreferredDirectionParser(prefDirEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertTrue(prefDirEnc.getBool(false, edgeId, edgeIntAccess));
        assertTrue(prefDirEnc.getBool(true, edgeId, edgeIntAccess));
        way.setTag("railway:preferred_direction", "forward");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertTrue(prefDirEnc.getBool(false, edgeId, edgeIntAccess));
        assertFalse(prefDirEnc.getBool(true, edgeId, edgeIntAccess));
        way.setTag("railway:preferred_direction", "backward");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertFalse(prefDirEnc.getBool(false, edgeId, edgeIntAccess));
        assertTrue(prefDirEnc.getBool(true, edgeId, edgeIntAccess));
        way.setTag("railway:preferred_direction", "both");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertTrue(prefDirEnc.getBool(false, edgeId, edgeIntAccess));
        assertTrue(prefDirEnc.getBool(true, edgeId, edgeIntAccess));
        way.setTag("railway:preferred_direction", "nonsense");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertTrue(prefDirEnc.getBool(false, edgeId, edgeIntAccess));
        assertTrue(prefDirEnc.getBool(true, edgeId, edgeIntAccess));
    }
}