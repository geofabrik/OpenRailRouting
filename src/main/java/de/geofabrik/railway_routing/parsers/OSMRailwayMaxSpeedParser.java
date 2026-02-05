/*
 *  This file contains code from GraphHopper published under Apache License,
 *  version 2.0.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package de.geofabrik.railway_routing.parsers;

import static com.graphhopper.routing.ev.MaxSpeed.MAXSPEED_MISSING;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.OSMMaxSpeedParser;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

/**
 * Maxspeed parser for railways.
 *
 * This is a modified copy of upstream OSMMaxSpeedParser class but with following changes:
 *
 *   - speed is not capped at 150 km/h
 *   - maxspeed=none is not supported
 *
 * This class does not inherit from OSMMaxSpeedParser because the most important methods to
 * override are declared as static.
 */
public class OSMRailwayMaxSpeedParser implements TagParser {

    /**
     * Special value to represent `maxspeed=none` internally, not exposed via the maxspeed encoded value
     */
    public static final double MAXSPEED_NONE = -1;
    private final DecimalEncodedValue maxSpeedEnc;

    public OSMRailwayMaxSpeedParser(DecimalEncodedValue maxSpeedEnc) {
        if (!maxSpeedEnc.isStoreTwoDirections())
            throw new IllegalArgumentException("EncodedValue for maxSpeed must be able to store two directions");

        this.maxSpeedEnc = maxSpeedEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        maxSpeedEnc.setDecimal(false, edgeId, edgeIntAccess, parseMaxSpeed(way, false));
        maxSpeedEnc.setDecimal(true, edgeId, edgeIntAccess, parseMaxSpeed(way, true));
    }

    /**
     * Return false if maxspeed is unset or "none". The latter is considered invalid for railways.
     */
    private static boolean maxSpeedValid(double maxSpeed) {
        return maxSpeed != MAXSPEED_MISSING && maxSpeed != MAXSPEED_NONE;
    }

    /*
    * @return The maxspeed for the given way. It can be anything between 0 and infinity,
    *         or {@link MaxSpeed.MAXSPEED_MISSING} in case there is no valid maxspeed tagged for this way in this direction.
    */
    public static double parseMaxSpeed(ReaderWay way, boolean reverse) {
        double directedMaxSpeed = OSMMaxSpeedParser.parseMaxspeedString(way.getTag(reverse ? "maxspeed:backward" : "maxspeed:forward"));
        if (maxSpeedValid(directedMaxSpeed)) {
            return directedMaxSpeed;
        } else {
           double maxSpeed = OSMMaxSpeedParser.parseMaxspeedString(way.getTag("maxspeed"));
           if (maxSpeedValid(maxSpeed)) {
               return maxSpeed;
           }
        }
        return MAXSPEED_MISSING;
    }
}
