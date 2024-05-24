package de.geofabrik.railway_routing.parsers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.ArrayEdgeIntAccess;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

public class RailSpeedParserTest {
    
    DecimalEncodedValue speedEnc;
    RailAverageSpeedParser parser;
    
    public RailSpeedParserTest() {
        parser = createSpeedParser(createEncodingManager(), new PMap());
    }

    private static RailAverageSpeedParser createSpeedParser(EncodedValueLookup lookup, PMap properties) {
        return new RailAverageSpeedParser(lookup, properties);
    }

    private EncodingManager createEncodingManager() {
        speedEnc = VehicleSpeed.create("rail", 5, 5, true);
        return new EncodingManager.Builder()
                .add(VehicleAccess.create("rail"))
                .add(speedEnc)
                .add(FerrySpeed.create())
                .addTurnCostEncodedValue(TurnCost.create("rail", 1))
                .build();
    }

    @Test
    public void testBidirectionalSpeed() {
        IntsRef relFlags = new IntsRef(2);
        ReaderWay way = new ReaderWay(29L);
        way.setTag("railway", "rail");
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(25.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(25.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed", "80");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(70.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(70.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed:forward", "140");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(125.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(70.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
        way.setTag("maxspeed:backward", "50");
        parser.handleWayTags(edgeId, edgeIntAccess, way, relFlags);
        assertEquals(125.0, speedEnc.getDecimal(false, edgeId, edgeIntAccess), 5.0);
        assertEquals(45.0, speedEnc.getDecimal(true, edgeId, edgeIntAccess), 5.0);
    }
}
