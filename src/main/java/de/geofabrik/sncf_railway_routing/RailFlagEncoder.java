package de.geofabrik.sncf_railway_routing;

import java.util.ArrayList;
import java.util.Arrays;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.util.PMap;

import de.geofabrik.sncf_railway_routing.util.MultiValueChecker;

public class RailFlagEncoder extends AbstractFlagEncoder {

	protected final Integer defaultSpeed = 25;
	private int tk;
	private String name;
	private ArrayList<String> electrifiedValues;
	private ArrayList<Integer> acceptedVoltages;
	private ArrayList<Double> acceptedFrequencies;
	private ArrayList<Integer> acceptedGauges;
	private double speedFactor;

	public RailFlagEncoder() {
		this(5, 5, 0, "rail");
	}

    public RailFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 5),
                properties.getDouble("speedFactor", 10),
        properties.getInt("max_turn_costs", 3),
        properties.get("name", ""));
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
        String electrifiedProps = properties.get("electrifiedValues", "");
        if (!electrifiedProps.equals("")) {
            this.electrifiedValues = new ArrayList<String>(Arrays.asList(properties.get("electrifiedValues", "").split(";")));
        } else {
            this.electrifiedValues = new ArrayList<String>();
        }

        this.acceptedVoltages = new ArrayList<Integer>();
        for (String v : properties.get("acceptedVoltages", "").split(";")) {
            if (!v.equals("")) {
                this.acceptedVoltages.add(Integer.parseInt(v));
            }
        }

        this.acceptedFrequencies = new ArrayList<Double>();
        for (String v : properties.get("acceptedFrequencies", "").split(";")) {
            if (!v.equals("")) {
                this.acceptedFrequencies.add(Double.parseDouble(v));
            }
        }

        this.acceptedGauges = new ArrayList<Integer>();
        for (String v : properties.get("acceptedGauges", "").split(";")) {
            if (!v.equals("")) {
                this.acceptedGauges.add(Integer.parseInt(v));
            }
        }

        this.maxPossibleSpeed = properties.getInt("max_speed", 100);
        this.speedFactor = properties.getDouble("speedFactor", 0.9);
    }

    public int getMaxTurnCosts() {
        return tk;
    }

    public void setSpeedFactor(double factor) {
        speedFactor = factor;
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
        String electrified = way.getTag("electrified");
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

    @Override
    public long acceptWay(ReaderWay way) {
        if (way.hasTag("railway", "rail") && hasCompatibleElectricity(way) && hasCompatibleGauge(way)) {
        	return acceptBit;
        }
        return 0;
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
        return maxSpeed * speedFactor;
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
