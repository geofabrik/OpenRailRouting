package de.geofabrik.railway_routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.VehicleTagParser;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.util.MultiValueChecker;

public class RailFlagEncoder extends VehicleTagParser {

    public static final String DEFAULT_NAME = "rail";

    protected final Integer defaultSpeed = 25;
    private HashSet<String> railwayValues;
    private ArrayList<String> electrifiedValues;
    private ArrayList<Integer> acceptedVoltages;
    private ArrayList<Double> acceptedFrequencies;
    private ArrayList<Integer> acceptedGauges;
    private double speedCorrectionFactor;
    private boolean acceptYardSpur;
    private PMap properties = new PMap();

    public RailFlagEncoder(BooleanEncodedValue accessEnc, DecimalEncodedValue speedEnc,
            String name, DecimalEncodedValue turnCostEnc, double maxPossibleSpeed, PMap properties) {
        super(accessEnc, speedEnc, name, null, turnCostEnc, TransportationMode.TRAIN, maxPossibleSpeed);
        this.properties = properties;
        initFromProperties(properties);
    }

    public RailFlagEncoder(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", DEFAULT_NAME))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", DEFAULT_NAME))),
                properties.getString("name", DEFAULT_NAME),
                lookup.hasEncodedValue(TurnCost.key(properties.getString("name", DEFAULT_NAME))) ? lookup.getDecimalEncodedValue(TurnCost.key(properties.getString("name", DEFAULT_NAME))) : null,
                properties.getInt(FlagEncoderConfiguration.MAXSPEED, 100),
                properties
        );
    }

    protected void initFromProperties(PMap properties) {
        if (this.properties == null) {
            this.properties = properties;
        } else {
            this.properties.putAll(properties);
        }
        // railway values
        String railwayProps = properties.getString(FlagEncoderConfiguration.RAILWAY, "");
        if (!railwayProps.equals("")) {
            this.railwayValues = new HashSet<String>(Arrays.asList(railwayProps.split(";")));
        } else {
            this.railwayValues = new HashSet<String>();
            this.railwayValues.add("rail");
        }

        // electrified values
        String electrifiedProps = properties.getString(FlagEncoderConfiguration.ELECTRIFIED, "");
        if (!electrifiedProps.equals("")) {
            this.electrifiedValues = new ArrayList<String>(Arrays.asList(electrifiedProps.split(";")));
        } else {
            this.electrifiedValues = new ArrayList<String>();
        }

        this.acceptedVoltages = new ArrayList<Integer>();
        for (String v : properties.getString(FlagEncoderConfiguration.VOLATAGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedVoltages.add(Integer.parseInt(v));
            }
        }

        this.acceptedFrequencies = new ArrayList<Double>();
        for (String v : properties.getString(FlagEncoderConfiguration.FREQUENCIES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedFrequencies.add(Double.parseDouble(v));
            }
        }

        this.acceptedGauges = new ArrayList<Integer>();
        for (String v : properties.getString(FlagEncoderConfiguration.GAUGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedGauges.add(Integer.parseInt(v));
            }
        }

        this.speedCorrectionFactor = properties.getDouble("speedCorrectionFactor", 0.9);
        this.acceptYardSpur = properties.getBool(FlagEncoderConfiguration.ACCEPT_YARD_SPUR, true);
    }

    public void setSpeedCorrectionFactor(double factor) {
        speedCorrectionFactor = factor;
    }

    public boolean hasCompatibleElectricity(ReaderWay way) {
        if (electrifiedValues.isEmpty()) {
            return true;
        }
        String electrified = way.getTag("electrified", null);
        if (electrified == null) {
            return true;
        }
        if (electrifiedValues.contains(electrified) || electrified.equals("yes")) {
            String voltage = way.getTag("voltage");
            String frequency = way.getTag("frequency");
            if (MultiValueChecker.tagContainsInt(voltage, acceptedVoltages, true)
                    && MultiValueChecker.tagContainsDouble(frequency, acceptedFrequencies, true)) {
                return true;
            }
            // Grounded sections of the overhead line are treated as compatible.
            if (voltage == null || voltage.equals("0")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCompatibleGauge(ReaderWay way) {
        return MultiValueChecker.tagContainsInt(way.getTag("gauge"), acceptedGauges, true);
    }

    public boolean isYardSpur(ReaderWay way) {
        return way.hasTag("service", "yard") || way.hasTag("service", "spur");
    }


    @Override
    public WayAccess getAccess(ReaderWay way) {
        if (!way.hasTag("railway", railwayValues) || !hasCompatibleElectricity(way) || !hasCompatibleGauge(way)) {
            return WayAccess.CAN_SKIP;
        }
        if (!acceptYardSpur && isYardSpur(way)) {
            return WayAccess.CAN_SKIP;
        }
        return WayAccess.WAY;
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
    public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way) {
        WayAccess access = getAccess(way);
        if (access.canSkip()) {
            return edgeFlags;
        }
        // get assumed speed from railway type
        double speed = getSpeed(way);
        speed = applyMaxSpeed(way, speed);
        setSpeed(false, edgeFlags, speed);
        if (avgSpeedEnc.isStoreTwoDirections()) {
            setSpeed(true, edgeFlags, speed);
        }
        accessEnc.setBool(false, edgeFlags, true);
        accessEnc.setBool(true, edgeFlags, true);
        return edgeFlags;
    }

    /**
     * @param way needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed.
     */
    protected double applyMaxSpeed(ReaderWay way, double speed) {
        double maxSpeed = getMaxSpeed(way);
        if (!isValidSpeed(maxSpeed)) {
            maxSpeed = speed;
        } else if (isValidSpeed(maxSpeed) && maxSpeed > maxPossibleSpeed) {
            maxSpeed = maxPossibleSpeed;
        }
        return maxSpeed * speedCorrectionFactor;
    }

    protected int handlePriority(ReaderWay way, int priorityFromRelation) {
        if (way.hasTag("usage", "main")) {
            return 140;
        }
        if (way.hasTag("usage", "branch")) {
            return 70;
        }
        if (way.hasTag("service", "siding")) {
            return 40;
        }
        if (way.hasTag("service", "crossover")) {
            return 20;
        }
        if (way.hasTag("service", "yard")) {
            return 5;
        }
        if (way.hasTag("service", "spur")) {
            return 1;
        }
        return 15;
    }
}
