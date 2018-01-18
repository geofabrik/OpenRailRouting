package de.geofabrik.sncf_railway_routing;


import static com.graphhopper.util.GHUtility.getEdge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.TurnWeighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.PointList;

public class RailwayHopper extends GraphHopperOSM {
    public RailwayHopper(final CmdArgs args) {
        setEncodingManager(new EncodingManager(new RailFlagEncoder(5, 5, 0), new RailFlagEncoder(5, 5, 1)));
        super.init(args);
    }

    private double getAngle(double or1, double or2) {
        return Math.abs(or1 -or2);
    }

    /**
     * Internal method to clean up the graph.
     */
    protected void cleanUp() {
        super.cleanUp();
        AngleCalc angleCalc = new AngleCalc();
        GraphHopperStorage ghs = getGraphHopperStorage();
        TurnCostExtension tcs = (TurnCostExtension) ghs.getExtension();
        for (FlagEncoder encoder : getEncodingManager().fetchEdgeEncoders()) {
            if (!encoder.supports(TurnWeighting.class)) {
                continue;
            }
            long tflags = encoder.getTurnFlags(true, 0);
            EdgeExplorer explorer = ghs.createEdgeExplorer();
            int nodes = ghs.getNodes();
            for (int start = 0; start < nodes; start++) {
                if (ghs.isNodeRemoved(start)) {
                    continue;
                }
                EdgeIterator iter = explorer.setBaseNode(start);
                double prevOrientation = Double.MAX_VALUE;
                int prevEdgeId = Integer.MAX_VALUE;
                // We have to cache the first edge to have it available when we get the last edge.
                double firstOrientation = Double.MAX_VALUE;
                int firstEdgeId = Integer.MAX_VALUE;
                while (iter.next()) {
                    PointList points = null;
                    // get geometry
                    points = iter.fetchWayGeometry(3);
                    // get orientation
                    double lon1 = points.getLon(0);
                    double lat1 = points.getLat(0);
                    double lon2 = points.getLon(1);
                    double lat2 = points.getLat(1);
                    double orientation = angleCalc.calcOrientation(lat1, lon1, lat2, lon2);
                    if (prevOrientation == Double.MAX_VALUE
                            || prevEdgeId == Integer.MAX_VALUE) {
                        prevOrientation = orientation;
                        firstOrientation = orientation;
                        prevEdgeId = iter.getEdge();
                        firstEdgeId = iter.getEdge();
                        continue;
                    }
                    // get difference
                    double diff = getAngle(orientation, prevOrientation);
                    if (diff < 0.75 * Math.PI || diff > 1.25 * Math.PI) {
                        // We have to forbid this turn in both directions.
                        tcs.addTurnInfo(prevEdgeId, iter.getBaseNode(), iter.getEdge(), tflags);
                        tcs.addTurnInfo(iter.getEdge(), iter.getBaseNode(), prevEdgeId, tflags);
                    }
                    prevEdgeId = iter.getEdge();
                    prevOrientation = orientation;
                }
                // compare last and first edge
                double diff = getAngle(prevOrientation, firstOrientation);
                if (diff < 0.75 * Math.PI || diff > 1.25 * Math.PI) {
                    tcs.addTurnInfo(prevEdgeId, iter.getBaseNode(), firstEdgeId, tflags);
                    tcs.addTurnInfo(firstEdgeId, iter.getBaseNode(), prevEdgeId, tflags);
                }
            }
        }
    }
}
