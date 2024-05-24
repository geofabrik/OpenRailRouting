package de.geofabrik.railway_routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.parsers.RailAccessParser;

public class RailAccessParserTest {

    private static final String carName = "rail";

    private EncodingManager createEncodingManager() {
        return createEncodingManager(5, 11, true);
    }

    private EncodingManager createEncodingManager(int bits, int factor, boolean twoDirections) {
        return new EncodingManager.Builder()
                .add(VehicleAccess.create(carName))
                .add(VehicleSpeed.create(carName, bits, factor, twoDirections))
                .add(FerrySpeed.create())
                .addTurnCostEncodedValue(TurnCost.create(carName, 1))
                .build();
    }

    private static RailAccessParser createAccessParser(EncodedValueLookup lookup, PMap properties) {
        return new RailAccessParser(lookup, properties);
    }

    @Test
    public void testAcceptsTrack() {
        PMap properties = new PMap();
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("railway", "rail");
        assertEquals(e.getAccess(way), WayAccess.WAY);
    }

    @Test
    public void testRejectRoad() {
        PMap properties = new PMap();
        RailAccessParser e = createAccessParser(createEncodingManager(), properties);
        ReaderWay way = new ReaderWay(1);
        way.setTag("highway", "primary");
        assertEquals(WayAccess.CAN_SKIP, e.getAccess(way));
        way.setTag("railway", "rail");
        assertEquals(WayAccess.WAY, e.getAccess(way));
    }
}
