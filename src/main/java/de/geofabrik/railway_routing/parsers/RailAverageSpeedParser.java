package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.util.PMap;

public class RailAverageSpeedParser extends AbstractAverageSpeedParser {

    public static final String DEFAULT_NAME = "rail";

    protected final Integer defaultSpeed = 25;
    private double speedCorrectionFactor;
    private PMap properties = new PMap();

    public RailAverageSpeedParser(DecimalEncodedValue speedEnc, DecimalEncodedValue ferrySpeedEnc, PMap properties) {
        super(speedEnc, ferrySpeedEnc);
        this.properties = properties;
        initFromProperties(properties);
    }

    public RailAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", DEFAULT_NAME))),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY),
                properties
        );
    }

    protected void initFromProperties(PMap properties) {
        if (this.properties == null) {
            this.properties = properties;
        } else {
            this.properties.putAll(properties);
        }
        this.speedCorrectionFactor = properties.getDouble("speedCorrectionFactor", 0.9);
    }

    public void setSpeedCorrectionFactor(double factor) {
        speedCorrectionFactor = factor;
    }


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
        // get assumed speed from railway type
        double speed = getSpeed(way);
        speed = applyMaxSpeed(way, speed);
        double maxPossibleSpeed = avgSpeedEnc.getMaxStorableDecimal();
        double minPossibleSpeed = avgSpeedEnc.getSmallestNonZeroValue();
        if (speed > maxPossibleSpeed) {
            speed = maxPossibleSpeed;
        }
        if (speed < minPossibleSpeed) {
            speed = minPossibleSpeed;
        }
        setSpeed(false, edgeId, edgeIntAccess, speed);
        if (avgSpeedEnc.isStoreTwoDirections()) {
            setSpeed(true, edgeId, edgeIntAccess, speed);
        }
    }

    /**
     * @param way needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed.
     */
    protected double applyMaxSpeed(ReaderWay way, double speed) {
        double maxSpeed = getMaxSpeed(way, false);
        if (!isValidSpeed(maxSpeed)) {
            maxSpeed = speed;
        }
        return maxSpeed * speedCorrectionFactor;
    }
}
