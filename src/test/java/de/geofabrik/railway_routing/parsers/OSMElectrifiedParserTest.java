package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.Electrified;

class OSMElectrifiedParserTest {

    @Test
    void railwayClass() {
        EnumEncodedValue<Electrified> classEnc = Electrified.create();
        classEnc.init(new EncodedValue.InitializerConfig());
        OSMElectrifiedParser parser = new OSMElectrifiedParser(classEnc);
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("highway", "primary");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.UNSET, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("railway", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.UNSET, classEnc.getEnum(false, edgeId, edgeIntAccess));
        way.setTag("electrified", "no");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.NO, classEnc.getEnum(false, edgeId, edgeIntAccess));

        way.setTag("electrified", "contact_line");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.CONTACT_LINE, classEnc.getEnum(false, edgeId, edgeIntAccess));

        way.setTag("electrified", "rail");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.RAIL, classEnc.getEnum(false, edgeId, edgeIntAccess));

        way.setTag("electrified", "magic_carpet");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(Electrified.OTHER, classEnc.getEnum(false, edgeId, edgeIntAccess));
    }

}