package de.geofabrik.railway_routing.parsers;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntsRefEdgeIntAccess;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import com.graphhopper.routing.util.parsers.RelationTagParser;
import com.graphhopper.storage.IntsRef;

/**
 * Sets {@link de.geofabrik.railway_routing.ev.TrainRoute} on ways that are
 * members of a {@code route=train} relation. Same pattern as
 * GraphHopper's bike network parsers: the relation flag is stored in a
 * transfer encoded value during relation preprocessing and copied to the edge
 * when the way is read.
 */
public class OSMTrainRouteParser implements RelationTagParser {

    private final BooleanEncodedValue trainRouteEnc;
    // used only for the transfer from relations into edge flags
    private final BooleanEncodedValue transferEnc = new SimpleBooleanEncodedValue("train_route_relation");

    public OSMTrainRouteParser(BooleanEncodedValue trainRouteEnc, EncodedValue.InitializerConfig relConfig) {
        this.trainRouteEnc = trainRouteEnc;
        this.transferEnc.init(relConfig);
    }

    @Override
    public void handleRelationTags(IntsRef relFlags, ReaderRelation relation) {
        if (relation.hasTag("route", "train"))
            transferEnc.setBool(false, -1, new IntsRefEdgeIntAccess(relFlags), true);
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        boolean member = transferEnc.getBool(false, -1, new IntsRefEdgeIntAccess(relationFlags));
        trainRouteEnc.setBool(false, edgeId, edgeIntAccess, member);
    }
}
