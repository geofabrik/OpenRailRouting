package de.geofabrik.railway_routing;

import static com.graphhopper.routing.util.EncodingManager.getKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.EncodingManager.Access;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.routing.profiles.EncodedValue;
import com.graphhopper.routing.profiles.UnsignedDecimalEncodedValue;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.util.MultiValueChecker;

public class RailFlagEncoder extends AbstractFlagEncoder {

    public static final String NAME = "name";
    public static final String RAILWAY = "railwayValues";
    public static final String ELECTRIFIED = "electrifiedValues";
    public static final String VOLATAGES = "acceptedVoltages";
    public static final String FREQUENCIES = "acceptedFrequencies";
    public static final String GAUGES = "acceptedGauges";
    public static final String MAXSPEED = "max_speed";
    public static final String SPEED_FACTOR = "speedFactor";
    public static final String ACCEPT_YARD_SPUR = "yardSpur";

    protected boolean speedTwoDirections = false;
    protected final Integer defaultSpeed = 25;
    private int tk;
    private String name;
    private HashSet<String> railwayValues;
    private ArrayList<String> electrifiedValues;
    private ArrayList<Integer> acceptedVoltages;
    private ArrayList<Double> acceptedFrequencies;
    private ArrayList<Integer> acceptedGauges;
    private double speedCorrectionFactor;
    private boolean acceptYardSpur;

    public RailFlagEncoder() {
        this(5, 5, 0, "rail");
    }

    public RailFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      this(speedBits, speedFactor, maxTurnCosts, "rail");
      //TODO change to true if we support
    }

    public RailFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 5),
                properties.getDouble(SPEED_FACTOR, 5),
        properties.getInt("max_turn_costs", 3),
        properties.get(NAME, ""));
        this.speedTwoDirections = properties.getBool("speed_two_directions", false);
        this.properties = properties;
        initFromProperties(properties);
    }

    public RailFlagEncoder(String propertiesStr) {
        this(new PMap(propertiesStr));
    }

    public RailFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts, String name) {
        super(speedBits, speedFactor, maxTurnCosts);
        if (name.equals("")) {
            throw new IllegalArgumentException("The name of an encoder must not be an empty string.");
        }
        this.name = name;
        tk = maxTurnCosts;
        init();
    }

    protected void initFromProperties(PMap properties) {
        super.init();
        if (this.properties == null) {
            this.properties = properties;
        } else {
            this.properties.put(properties);
        }
        // railway values
        String railwayProps = properties.get(RAILWAY, "");
        if (!railwayProps.equals("")) {
            this.railwayValues = new HashSet<String>(Arrays.asList(railwayProps.split(";")));
        } else {
            this.railwayValues = new HashSet<String>();
            this.railwayValues.add("rail");
        }

        // electrified values
        String electrifiedProps = properties.get(ELECTRIFIED, "");
        if (!electrifiedProps.equals("")) {
            this.electrifiedValues = new ArrayList<String>(Arrays.asList(electrifiedProps.split(";")));
        } else {
            this.electrifiedValues = new ArrayList<String>();
        }

        this.acceptedVoltages = new ArrayList<Integer>();
        for (String v : properties.get(VOLATAGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedVoltages.add(Integer.parseInt(v));
            }
        }

        this.acceptedFrequencies = new ArrayList<Double>();
        for (String v : properties.get(FREQUENCIES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedFrequencies.add(Double.parseDouble(v));
            }
        }

        this.acceptedGauges = new ArrayList<Integer>();
        for (String v : properties.get(GAUGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedGauges.add(Integer.parseInt(v));
            }
        }

        this.maxPossibleSpeed = properties.getInt(MAXSPEED, 100);
        this.speedCorrectionFactor = properties.getDouble("speedCorrectionFactor", 0.9);
        this.acceptYardSpur = properties.getBool(ACCEPT_YARD_SPUR, true);
    }

    public int getMaxTurnCosts() {
        return tk;
    }

    public void setSpeedCorrectionFactor(double factor) {
        speedCorrectionFactor = factor;
    }

    public void setMaxPossibleSpeed(int speed) {
        maxPossibleSpeed = speed;
    }

    @Override
    public long handleRelationTags(long oldRelation, ReaderRelation relation) {
        return oldRelation;
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
            if (voltage.equals("0")) {
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
    public EncodingManager.Access getAccess(ReaderWay way) {
        if (!way.hasTag("railway", railwayValues) || !hasCompatibleElectricity(way) || !hasCompatibleGauge(way)) {
            return EncodingManager.Access.CAN_SKIP;
        }
        if (!acceptYardSpur && isYardSpur(way)) {
            return EncodingManager.Access.CAN_SKIP;
        }
        return EncodingManager.Access.WAY;
    }

    @Override
    public void createEncodedValues(List<EncodedValue> registerNewEncodedValue, String prefix, int index) {
        // first two bits are reserved for route handling in superclass
        super.createEncodedValues(registerNewEncodedValue, prefix, index);
        registerNewEncodedValue.add(speedEncoder = new UnsignedDecimalEncodedValue(getKey(prefix, "average_speed"), speedBits, speedFactor, defaultSpeed, speedTwoDirections));
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
    public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, EncodingManager.Access access, long relationFlags) {
        if (access.canSkip()) {
            return edgeFlags;
        }
        // get assumed speed from railway type
        double speed = getSpeed(way);
        speed = applyMaxSpeed(way, speed);
        setSpeed(false, edgeFlags, speed);
        if (speedTwoDirections) {
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
        if (maxSpeed <= 0) {
            maxSpeed = speed;
        } else if (maxSpeed > this.maxPossibleSpeed) {
            maxSpeed = this.maxPossibleSpeed;
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

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
