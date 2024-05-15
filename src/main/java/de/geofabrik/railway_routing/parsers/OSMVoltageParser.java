package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class OSMVoltageParser implements TagParser {

    private final IntEncodedValue voltageEnc;

    public OSMVoltageParser(IntEncodedValue voltageEnc) {
        this.voltageEnc = voltageEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String voltage = way.getTag("voltage");
        if (voltage == null) {
            return;
        }
        if (voltage.contains(";")) {
            throw new IllegalArgumentException("way has voltage=* tag with multiple values. This should have been cleaned by OSMReader class or its children.");
        }
        try {
            Integer value = Integer.parseInt(voltage);
            if (value <= voltageEnc.getMaxStorableInt() && value >= voltageEnc.getMinStorableInt()) {
                voltageEnc.setInt(false, edgeId, edgeIntAccess, value);
            }
        } catch (NumberFormatException ex) {
            // ignore failures
        }
    }
}
