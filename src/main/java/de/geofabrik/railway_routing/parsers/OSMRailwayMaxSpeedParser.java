package de.geofabrik.railway_routing.parsers;

import static com.graphhopper.routing.ev.MaxSpeed.MAXSPEED_MISSING;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.util.parsers.OSMMaxSpeedParser;

public class OSMRailwayMaxSpeedParser extends OSMMaxSpeedParser {

    public OSMRailwayMaxSpeedParser(DecimalEncodedValue carMaxSpeedEnc) {
        super(carMaxSpeedEnc);
    }

    /**
     * Return false if maxspeed is unset or "none". The latter is considered invalid for railways.
     */
    private static boolean maxSpeedValid(double maxSpeed) {
        return maxSpeed != MAXSPEED_MISSING && maxSpeed != MAXSPEED_NONE;
    }

    /*
    * @return The maxspeed for the given way. It can be anything between 0 and {@link MaxSpeed.MAXSPEED_150},
    *         or {@link MaxSpeed.MAXSPEED_MISSING} in case there is no valid maxspeed tagged for this way in this direction.
    */
    public static double parseMaxSpeed(ReaderWay way, boolean reverse) {
        double directedMaxSpeed = parseMaxspeedString(way.getTag(reverse ? "maxspeed:backward" : "maxspeed:forward"));
        if (maxSpeedValid(directedMaxSpeed)) {
            return directedMaxSpeed;
        } else {
           double maxSpeed = parseMaxspeedString(way.getTag("maxspeed"));
           if (maxSpeedValid(maxSpeed)) {
               return maxSpeed;
           }
        }
        return MAXSPEED_MISSING;
    }
}
