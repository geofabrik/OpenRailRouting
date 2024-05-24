package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class OSMVoltageParser implements TagParser {

    private final DecimalEncodedValue voltageEnc;

    public OSMVoltageParser(DecimalEncodedValue voltageEnc) {
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
            Double value = Double.parseDouble(voltage);
            if (value <= voltageEnc.getMaxStorableDecimal() && value >= voltageEnc.getMinStorableDecimal()) {
                voltageEnc.setDecimal(false, edgeId, edgeIntAccess, value);
            }
        } catch (NumberFormatException ex) {
            // ignore failures
        }
    }
}
