package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

/**
 * Read value of railway:preferred_direction=*. Defaults to `both`.
 * Any value other than `forward` or `backward` is treated like `both`. 
 */
public class OSMPreferredDirectionParser implements TagParser {

    private final BooleanEncodedValue prefDirEnc;

    public OSMPreferredDirectionParser(BooleanEncodedValue enc) {
        this.prefDirEnc = enc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String value = way.getTag("railway:preferred_direction");
        boolean forward = true;
        boolean backward = true;
        if (value != null) {
            if (value.equals("forward")) {
                backward = false;
            } else if (value.equals("backward")) {
                forward = false;
            }
        }
        prefDirEnc.setBool(false, edgeId, edgeIntAccess, forward);
        prefDirEnc.setBool(true, edgeId, edgeIntAccess, backward);
    }
}