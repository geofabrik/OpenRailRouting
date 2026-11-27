package de.geofabrik.railway_routing.parsers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.TrainRoute;

class OSMTrainRouteParserTest {

    private static boolean member(String routeValue) {
        BooleanEncodedValue enc = TrainRoute.create();
        enc.init(new EncodedValue.InitializerConfig());
        EncodedValue.InitializerConfig relConfig = new EncodedValue.InitializerConfig();
        OSMTrainRouteParser parser = new OSMTrainRouteParser(enc, relConfig);
        IntsRef relFlags = new IntsRef(2);
        if (routeValue != null) {
            ReaderRelation relation = new ReaderRelation(1);
            relation.setTag("type", "route");
            relation.setTag("route", routeValue);
            parser.handleRelationTags(relFlags, relation);
        }
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        parser.handleWayTags(0, edgeIntAccess, way, relFlags);
        return enc.getBool(false, 0, edgeIntAccess);
    }

    @Test
    void wayInTrainRouteIsMarked() {
        assertTrue(member("train"));
    }

    @Test
    void wayOutsideTrainRoutesIsNotMarked() {
        assertFalse(member(null));
        assertFalse(member("tracks"));
        assertFalse(member("railway"));
        assertFalse(member("bus"));
    }
}
