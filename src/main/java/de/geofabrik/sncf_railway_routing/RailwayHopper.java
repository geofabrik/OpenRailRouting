package de.geofabrik.sncf_railway_routing;


import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.TurnWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

public class RailwayHopper extends GraphHopperOSM {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RailwayHopper(final CmdArgs args) {
        if (args.get("datareader.file", "").equals("")) {
            logger.error("Missing argument datareader.file=<OSM file>");
            System.exit(1);
        }
        setTraversalMode(TraversalMode.EDGE_BASED_2DIR);
        String[] encoderNames = {"freight_electric_15kvac", "freight_diesel", "tgv_15kvac25kvac1.5kvdc", "tgv_25kvac1.5kvdc3kvdc"};
        setEncodingManager(new EncodingManager(RailFlagEncoderFactory.craeateEncoders(encoderNames)));
        super.init(args);
    }

    private double getAngle(double or1, double or2) {
        return Math.abs(or1 - or2);
    }

    public Weighting createTurnWeighting(Graph graph, Weighting weighting, TraversalMode tMode) {
        FlagEncoder encoder = weighting.getFlagEncoder();
        if (encoder.supports(TurnWeighting.class) && !tMode.equals(TraversalMode.NODE_BASED)) {
            RailTurnWeighting tw = new RailTurnWeighting(weighting, (TurnCostExtension) graph.getExtension());
            tw.setDefaultUTurnCost(60 * 30);
            return tw;
        }
        return weighting;
    }

    private void addTurnCosts(TurnCostExtension tcs, int fromEdge, int viaNode, int toEdge, double angleDiff) {
        long flags = 0;
        for (FlagEncoder encoder : getEncodingManager().fetchEdgeEncoders()) {
            if (!encoder.supports(TurnWeighting.class)) {
                continue;
            }
            if (angleDiff < 0.3 * Math.PI || angleDiff > 1.7 * Math.PI) {
                // avoid this turn because it requires a change of direction
                flags = flags | encoder.getTurnFlags(false, ((RailFlagEncoder) encoder).getMaxTurnCosts() - 1);
            } else if (angleDiff < 0.75 * Math.PI || angleDiff > 1.25 * Math.PI) {
                // this turn is forbidden
                flags = flags | encoder.getTurnFlags(true, 0);
            }
        }
        tcs.addTurnInfo(fromEdge, viaNode, toEdge, flags);
        tcs.addTurnInfo(toEdge, viaNode, fromEdge, flags);
    }

    protected void cleanUp() {
        super.cleanUp();
        AngleCalc angleCalc = new AngleCalc();
        GraphHopperStorage ghs = getGraphHopperStorage();
        TurnCostExtension tcs = (TurnCostExtension) ghs.getExtension();
        EdgeExplorer explorer = ghs.createEdgeExplorer();
        int nodes = ghs.getNodes();
        for (int start = 0; start < nodes; start++) {
            if (ghs.isNodeRemoved(start)) {
                continue;
            }
            EdgeIterator iter = explorer.setBaseNode(start);
            // get list of IDs of edges
            ArrayList<Integer> edges = new ArrayList<Integer>();
            ArrayList<Integer> adjNodes = new ArrayList<Integer>();
            while (iter.next()) {
                edges.add(iter.getEdge());
                adjNodes.add(iter.getAdjNode());
            }
            for (int i = 0; i < adjNodes.size(); ++i) {
                EdgeIteratorState fromEdge = ghs.getEdgeIteratorState(edges.get(i), adjNodes.get(i));
                PointList fromPoints = fromEdge.fetchWayGeometry(3);
                double fromLon = fromPoints.getLon(1);
                double fromLat = fromPoints.getLat(1);
                double centreLon = fromPoints.getLon(0);
                double centreLat = fromPoints.getLat(0);
                double fromOrientation = angleCalc.calcOrientation(centreLat, centreLon, fromLat, fromLon);
                for (int j = i + 1; j < adjNodes.size(); ++j) {
                    EdgeIteratorState toEdge = ghs.getEdgeIteratorState(edges.get(j), adjNodes.get(j));
                    PointList toPoints = toEdge.fetchWayGeometry(3);
                    double toLon = toPoints.getLon(1);
                    double toLat = toPoints.getLat(1);
                    double toOrientation = angleCalc.calcOrientation(centreLat, centreLon, toLat, toLon);
                    double diff = getAngle(fromOrientation, toOrientation);
                    addTurnCosts(tcs, fromEdge.getEdge(), start, toEdge.getEdge(), diff);
                }
            }
        }
    }
}
