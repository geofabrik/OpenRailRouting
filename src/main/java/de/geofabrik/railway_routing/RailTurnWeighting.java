package de.geofabrik.railway_routing;

import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.TurnCostExtension;

public class RailTurnWeighting extends com.graphhopper.routing.weighting.TurnWeighting {

    private final TurnCostEncoder turnCostEncoder;
    private final TurnCostExtension turnCostExt;
    private double directionChangeTime = 60 * 4;


    public RailTurnWeighting(Weighting superWeighting, TurnCostExtension turnCostExt, double uTurnCosts) {
        super(superWeighting, turnCostExt);
        this.turnCostEncoder = (TurnCostEncoder) superWeighting.getFlagEncoder();
        this.turnCostExt = turnCostExt;

        if (turnCostExt == null)
            throw new RuntimeException("No storage set to calculate turn weight");
    }

    public double calcTurnWeight(int edgeFrom, int nodeVia, int edgeTo) {
        long turnFlags = turnCostExt.getTurnCostFlags(edgeFrom, nodeVia, edgeTo);
        if (turnCostEncoder.isTurnRestricted(turnFlags)) {
            return Double.POSITIVE_INFINITY;
        }
        return turnCostEncoder.getTurnCost(turnFlags) * directionChangeTime;
    }
}
