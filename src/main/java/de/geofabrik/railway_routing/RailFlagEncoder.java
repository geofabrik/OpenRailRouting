package de.geofabrik.railway_routing;

import java.util.ArrayList;
import java.util.Arrays;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.util.MultiValueChecker;

public class RailFlagEncoder extends AbstractFlagEncoder {

    public static final String NAME = "name";
    public static final String ELECTRIFIED = "electrifiedValues";
    public static final String VOLATAGES = "acceptedVoltages";
    public static final String FREQUENCIES = "acceptedFrequencies";
    public static final String GAUGES = "acceptedGauges";
    public static final String MAXSPEED = "max_speed";
    public static final String SPEED_FACTOR = "speedFactor";
    public static final String ACCEPT_YARD_SPUR = "yardSpur";

	protected final Integer defaultSpeed = 25;
	private int tk;
	private String name;
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
	}

    public RailFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 5),
                properties.getDouble(SPEED_FACTOR, 5),
        properties.getInt("max_turn_costs", 3),
        properties.get(NAME, ""));
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
        // electrified values
        String electrifiedProps = properties.get(ELECTRIFIED, "");
        if (!electrifiedProps.equals("")) {
            this.electrifiedValues = new ArrayList<String>(Arrays.asList(properties.get("electrifiedValues", "").split(";")));
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
    public long handleRelationTags(ReaderRelation relation, long oldRelationFlags) {
        return oldRelationFlags;
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
    public long acceptWay(ReaderWay way) {
        if (!way.hasTag("railway", "rail") || !hasCompatibleElectricity(way) || !hasCompatibleGauge(way)) {
            return 0;
        }
        if (!acceptYardSpur && isYardSpur(way)) {
            return 0;
        }
        return acceptBit;
    }

    @Override
    public int defineWayBits(int index, int shift) {
        // first two bits are reserved for route handling in superclass
        shift = super.defineWayBits(index, shift);
        speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, defaultSpeed,
        		maxPossibleSpeed);
        return shift + speedEncoder.getBits();
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
    public long handleWayTags(ReaderWay way, long allowed, long relationFlags) {
        if (!isAccept(allowed)) {
            return 0;
        }
        long flags = 0;
        // get assumed speed from railway type
        double speed = getSpeed(way);
        speed = applyMaxSpeed(way, speed);
        flags = setSpeed(flags, speed);
        flags |= directionBitMask;
        return flags;
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
