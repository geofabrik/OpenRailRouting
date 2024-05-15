package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;

import de.geofabrik.railway_routing.ev.Electrified;

public class OSMElectrifiedParser implements TagParser {
    
    protected final EnumEncodedValue<Electrified> electrifiedEnc;

    public OSMElectrifiedParser(EnumEncodedValue<Electrified> roadClassEnc) {
        this.electrifiedEnc = roadClassEnc;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String value = way.getTag("electrified");
        if (value == null) {
            electrifiedEnc.setEnum(false, edgeId, edgeIntAccess, Electrified.UNSET);
            return;
        }
        if (value.contains(";")) {
            throw new IllegalArgumentException("way has electrified=* tag with multiple values. This should have been cleaned by OSMReader class or its children.");
        }
        Electrified el = Electrified.find(value);
        electrifiedEnc.setEnum(false, edgeId, edgeIntAccess, el);
    }

}
