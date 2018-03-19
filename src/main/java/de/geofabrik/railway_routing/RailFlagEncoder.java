package de.geofabrik.railway_routing;

import static com.graphhopper.util.Helper.keepIn;

import java.util.ArrayList;
import java.util.Arrays;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;

import de.geofabrik.railway_routing.util.MultiValueChecker;

public class RailFlagEncoder extends AbstractFlagEncoder {

	protected final Integer defaultSpeed = 25;
	private int tk;
	private String name;
	private ArrayList<String> electrifiedValues;
	private ArrayList<Integer> acceptedVoltages;
	private ArrayList<Double> acceptedFrequencies;
	private ArrayList<Integer> acceptedGauges;
	private double speedCorrectionFactor;
	private boolean enableSRTM;

	public RailFlagEncoder() {
		this(5, 5, 0, "rail");
	}

	public RailFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
	    this(speedBits, speedFactor, maxTurnCosts, "rail");
	}

    public RailFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 5),
                properties.getDouble("speedFactor", 5),
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
        this.speedCorrectionFactor = properties.getDouble("speedCorrectionFactor", 0.9);
        this.enableSRTM = properties.getBool("enableSRTM", false);
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
    
    public void applyWayTags(ReaderWay way, EdgeIteratorState edge) {
        if (!enableSRTM) {
            return;
        }
        PointList pl = edge.fetchWayGeometry(3);
        if (!pl.is3D())
            throw new IllegalStateException("To support speed calculation based on elevation data it is necessary to enable import of it.");

        long flags = edge.getFlags();

        if (way.hasTag("tunnel", "yes") || way.hasTag("bridge", "yes") /*|| way.hasTag("highway", "steps")*/) {
            // do not change speed
            // note: although tunnel can have a difference in elevation it is very unlikely that the elevation data is correct for a tunnel
        } else {
            // Decrease the speed for ele increase (incline), and decrease the speed for ele decrease (decline). The speed-decrease 
            // has to be bigger (compared to the speed-increase) for the same elevation difference to simulate loosing energy and avoiding hills.
            // For the reverse speed this has to be the opposite but again keeping in mind that up+down difference.
            double incEleSum = 0, incDist2DSum = 0;
            double decEleSum = 0, decDist2DSum = 0;
            // double prevLat = pl.getLatitude(0), prevLon = pl.getLongitude(0);
            double prevEle = pl.getElevation(0);
            double fullDist2D = edge.getDistance();

            if (Double.isInfinite(fullDist2D))
                throw new IllegalStateException("Infinite distance should not happen due to #435. way ID=" + way.getId());

            // for short edges an incline makes no sense and for 0 distances could lead to NaN values for speed, see #432
            if (fullDist2D < 1)
                return;

            double eleDelta = pl.getElevation(pl.size() - 1) - prevEle;
            if (eleDelta > 0.1) {
                incEleSum = eleDelta;
                incDist2DSum = fullDist2D;
            } else if (eleDelta < -0.1) {
                decEleSum = -eleDelta;
                decDist2DSum = fullDist2D;
            }

//            // get a more detailed elevation information, but due to bad SRTM data this does not make sense now.
//            for (int i = 1; i < pl.size(); i++)
//            {
//                double lat = pl.getLatitude(i);
//                double lon = pl.getLongitude(i);
//                double ele = pl.getElevation(i);
//                double eleDelta = ele - prevEle;
//                double dist2D = distCalc.calcDist(prevLat, prevLon, lat, lon);
//                if (eleDelta > 0.1)
//                {
//                    incEleSum += eleDelta;
//                    incDist2DSum += dist2D;
//                } else if (eleDelta < -0.1)
//                {
//                    decEleSum += -eleDelta;
//                    decDist2DSum += dist2D;
//                }
//                fullDist2D += dist2D;
//                prevLat = lat;
//                prevLon = lon;
//                prevEle = ele;
//            }
            // Calculate slop via tan(asin(height/distance)) but for rather smallish angles where we can assume tan a=a and sin a=a.
            // Then calculate a factor which decreases or increases the speed.
            // Do this via a simple quadratic equation where y(0)=1 and y(0.3)=1/4 for incline and y(0.3)=2 for decline        
            double fwdIncline = incDist2DSum > 1 ? incEleSum / incDist2DSum : 0;
            double fwdDecline = decDist2DSum > 1 ? decEleSum / decDist2DSum : 0;
//            double restDist2D = fullDist2D - incDist2DSum - decDist2DSum;
//            double maxSpeed = getHighwaySpeed("cycleway");
            double maxSpeed = applyMaxSpeed(way, getSpeed(way));
            // m * x + c = f(x); m = -5.0/3.0; c = 90
            double m = -5.0/3.0;
            double md = -4.0/3.0;
            double c = 90;
            if (isForward(flags)) {
                double speed = getSpeed(flags);
                if (fwdIncline > 0.05) {
                    speed = 15;
                } else if (fwdIncline > 0) {
                    speed = keepIn(m * fwdIncline*1000 + c, 15, maxSpeed);
                } else if (fwdDecline > 0.01) {
                    speed = 15;
                } else if (fwdDecline > 0) {
                    speed = keepIn(md * fwdDecline*1000 + c, 15, maxSpeed);
                }
                flags = this.setSpeed(flags, keepIn(speed, 15, maxSpeed));
            }
            if (isBackward(flags)) {
                double speedReverse = getReverseSpeed(flags);
                if (fwdIncline > 0.05) {
                    speedReverse = 15;
                } else if (fwdIncline > 0) {
                    speedReverse = keepIn(m * fwdDecline*1000 + c, 15, maxSpeed);
                } else if (fwdDecline > 0.01) {
                    speedReverse = 15;
                } else if (fwdDecline > 0) {
                    speedReverse = keepIn(md * fwdIncline*1000 + c, 15, maxSpeed);
                }
                flags = this.setReverseSpeed(flags, keepIn(speedReverse, 15, maxSpeed));
            }
//            if (isForward(flags)) {
//                // use weighted mean so that longer incline influences speed more than shorter
//                double speed = getSpeed(flags);
//                double fwdFaster = 1 + 2 * keepIn(fwdDecline, 0, 0.2);
//                fwdFaster = fwdFaster * fwdFaster;
//                double fwdSlower = 1 - 5 * keepIn(fwdIncline, 0, 0.2);
//                fwdSlower = fwdSlower * fwdSlower;
//                speed = speed * (fwdSlower * incDist2DSum + fwdFaster * decDist2DSum + 1 * restDist2D) / fullDist2D;
//                flags = this.setSpeed(flags, keepIn(speed, 15, maxSpeed));
//            }
//
//            if (isBackward(flags)) {
//                double speedReverse = getReverseSpeed(flags);
//                double bwFaster = 1 + 2 * keepIn(fwdIncline, 0, 0.2);
//                bwFaster = bwFaster * bwFaster;
//                double bwSlower = 1 - 5 * keepIn(fwdDecline, 0, 0.2);
//                bwSlower = bwSlower * bwSlower;
//                speedReverse = speedReverse * (bwFaster * incDist2DSum + bwSlower * decDist2DSum + 1 * restDist2D) / fullDist2D;
//                flags = this.setReverseSpeed(flags, keepIn(speedReverse, 15, maxSpeed));
//            }
        }
        edge.setFlags(flags);
    }

}
