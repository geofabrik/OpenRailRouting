package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;

public class RailAverageSpeedParser extends AbstractAverageSpeedParser {

    protected final Integer defaultSpeed = 25;

    public RailAverageSpeedParser(DecimalEncodedValue speedEnc, DecimalEncodedValue ferrySpeedEnc) {
        super(speedEnc, ferrySpeedEnc);
    }

    public RailAverageSpeedParser(EncodedValueLookup lookup) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key("rail")),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY)
        );
    }

    /**
     * Get assumed speed from railway type
     */
    protected double getSpeed(ReaderWay way) {
        if (way.hasTag("service", "siding")) {
            return 40;
        } else if (way.hasTag("service", "yard")) {
            return 25;
        } else if (way.hasTag("service", "crossover")) {
            return 60;
        } else if (way.hasTag("usage", "main")) {
            return 100;
        } else if (way.hasTag("usage", "branch")) {
            return 50;
        }
        return defaultSpeed;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        double speed = getSpeed(way);
        setSpeed(false, edgeId, edgeIntAccess, applyMaxSpeed(way, speed, false));
        setSpeed(true, edgeId, edgeIntAccess, applyMaxSpeed(way, speed, true));
    }

    /**
     * @param way needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @param backward if speed for backward direction should be returned.
     * @return The assumed speed.
     */
    protected double applyMaxSpeed(ReaderWay way, double speed, boolean backward) {
        double maxSpeed = OSMRailwayMaxSpeedParser.parseMaxSpeed(way, backward);
        if (maxSpeed == MaxSpeed.MAXSPEED_MISSING) {
            return speed;
        }
        return Math.min(Math.max(maxSpeed * 0.9, avgSpeedEnc.getSmallestNonZeroValue()), avgSpeedEnc.getMaxStorableDecimal());
    }
}
