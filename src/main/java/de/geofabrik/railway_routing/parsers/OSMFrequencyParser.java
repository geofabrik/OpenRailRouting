package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

public class OSMFrequencyParser implements TagParser {

    private final DecimalEncodedValue frequencyEnc;

    public OSMFrequencyParser(DecimalEncodedValue frequencyEnc) {
        this.frequencyEnc = frequencyEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String frequency = way.getTag("frequency");
        if (frequency == null) {
            return;
        }
        if (frequency.contains(";")) {
            throw new IllegalArgumentException("way has frequency=* tag with multiple values. This should have been cleaned by OSMReader class or its children.");
        }
        try {
            Double value = Double.parseDouble(frequency);
            if (value <= frequencyEnc.getMaxStorableDecimal() && value >= frequencyEnc.getMinStorableDecimal()) {
                frequencyEnc.setDecimal(false, edgeId, edgeIntAccess, value);
            }
        } catch (NumberFormatException ex) {
            // ignore failures
        }
    }
}
