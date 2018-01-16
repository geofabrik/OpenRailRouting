package de.geofabrik.sncf_railway_routing;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.weighting.PriorityWeighting;
import com.graphhopper.util.PMap;

import java.util.Arrays;
import java.util.TreeMap;

import static com.graphhopper.routing.util.PriorityCode.*;

public class RailFlagEncoder extends AbstractFlagEncoder {
	
	protected final Integer defaultSpeed = 25;
	
	public RailFlagEncoder() {
		this(5, 5, 0);
	}

    public RailFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 5),
                properties.getDouble("speedFactor", 10),
        properties.getBool("turn_costs", false) ? 1 : 0);
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", true));
        this.setBlockByDefault(properties.getBool("block_barriers", false));
	}

    public RailFlagEncoder(String propertiesStr) {
        this(new PMap(propertiesStr));
    }
    
    public RailFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
        super(speedBits, speedFactor, maxTurnCosts);        
        maxPossibleSpeed = 150;
        init();
    }

	@Override
    public long handleRelationTags(ReaderRelation relation, long oldRelationFlags) {
        return oldRelationFlags;
    }
    
    @Override
    public long acceptWay(ReaderWay way) {
        if (way.hasTag("railway", "rail")) {
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
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public String toString() {
        return "rail";
    }

}
