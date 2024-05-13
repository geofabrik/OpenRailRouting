package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class OSMGaugeParser implements TagParser {

    private final IntEncodedValue gaugeEnc;

    public OSMGaugeParser(IntEncodedValue gaugeEnc) {
        this.gaugeEnc = gaugeEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String gauge = way.getTag("gauge");
        if (gauge == null) {
            return;
        }
        if (gauge.contains(";")) {
            throw new IllegalArgumentException("way has gauge=* tag with multiple values. This should have been cleaned by OSMReader class or its children.");
        }
        try {
            Integer value = Integer.parseInt(gauge);
            if (value <= gaugeEnc.getMaxStorableInt() && value >= gaugeEnc.getMinStorableInt()) {
                gaugeEnc.setInt(false, edgeId, edgeIntAccess, value);
            }
        } catch (NumberFormatException ex) {
            // ignore failures
        }
    }
}