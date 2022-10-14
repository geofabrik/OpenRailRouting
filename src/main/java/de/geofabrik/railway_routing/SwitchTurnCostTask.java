package de.geofabrik.railway_routing;

import java.util.ArrayList;
import java.util.Map;

import com.carrotsearch.hppc.IntSet;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.PostProcessingTask;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.parsers.TurnCostParser;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;

public class SwitchTurnCostTask implements PostProcessingTask {

    AngleCalc angleCalc = new AngleCalc();
    GraphHopperStorage ghStorage;
    EncodingManager encodingManager;
    private IntSet crossingsSet;

    public SwitchTurnCostTask(GraphHopperStorage storage, EncodingManager em, IntSet crossingsSet) {
        ghStorage = storage;
        encodingManager = em;
        this.crossingsSet = crossingsSet;
    }

    private double getAngle(double or1, double or2) {
        return Math.abs(or1 - or2);
    }

    private void addTurnCosts(int fromEdge, int viaNode, int toEdge, double angleDiff, boolean crossing) {
        TurnCostStorage tcs = ghStorage.getTurnCostStorage();
        boolean forbidden = false;
        boolean avoid = false;
        if (crossing && (angleDiff < 0.92 * Math.PI || angleDiff > 1.08 * Math.PI)) {
            forbidden = true;
        }
        else if (angleDiff < 0.3 * Math.PI || angleDiff > 1.7 * Math.PI) {
            // Avoid this turn because it requires a change of direction.
            // The TurnCostStorage implementation ORs a potentially existing value and the new value.
            // Therefore, if we set a value lower than the encoders maximum, we will not overwrite
            // an already set maximum. And the code does not set values lower than maximum except here.
            avoid = true;
        } else if (angleDiff < 0.75 * Math.PI || angleDiff > 1.25 * Math.PI) {
            forbidden = true;
        }
        Map<String, TurnCostParser> turnCostParsers = encodingManager.getTurnCostParsers();
        for (TurnCostParser parser : turnCostParsers.values()) {
            DecimalEncodedValue turnCostEnc = parser.getTurnCostEnc();
            if (avoid) {
                tcs.set(turnCostEnc, fromEdge, viaNode, toEdge, turnCostEnc.getMaxDecimal() - 1);
                tcs.set(turnCostEnc, toEdge, viaNode, fromEdge, turnCostEnc.getMaxDecimal() - 1);
            } else if (forbidden) {
                tcs.set(turnCostEnc, fromEdge, viaNode, toEdge, Double.POSITIVE_INFINITY);
                tcs.set(turnCostEnc, toEdge, viaNode, fromEdge, Double.POSITIVE_INFINITY);
            }
        }
    }

    private void handleSwitch(EdgeIterator iter, int node) {
        // get list of IDs of edges
        ArrayList<Integer> edges = new ArrayList<Integer>();
        ArrayList<Integer> adjNodes = new ArrayList<Integer>();
        // check if it is a railway crossing
        // We only get tower nodes here.
        int id = OSMReader.towerIdToMapId(node);
        boolean crossing = crossingsSet.contains(id);
        while (iter.next()) {
            edges.add(iter.getEdge());
            adjNodes.add(iter.getAdjNode());
        }
        for (int i = 0; i < adjNodes.size(); ++i) {
            EdgeIteratorState fromEdge = ghStorage.getEdgeIteratorState(edges.get(i), adjNodes.get(i));
            PointList fromPoints = fromEdge.fetchWayGeometry(FetchMode.ALL);
            double fromLon = fromPoints.getLon(1);
            double fromLat = fromPoints.getLat(1);
            double centreLon = fromPoints.getLon(0);
            double centreLat = fromPoints.getLat(0);
            double fromOrientation = angleCalc.calcOrientation(centreLat, centreLon, fromLat, fromLon);
            for (int j = i + 1; j < adjNodes.size(); ++j) {
                EdgeIteratorState toEdge = ghStorage.getEdgeIteratorState(edges.get(j), adjNodes.get(j));
                PointList toPoints = toEdge.fetchWayGeometry(FetchMode.ALL);
                double toLon = toPoints.getLon(1);
                double toLat = toPoints.getLat(1);
                double toOrientation = angleCalc.calcOrientation(centreLat, centreLon, toLat, toLon);
                double diff = getAngle(fromOrientation, toOrientation);
                addTurnCosts(fromEdge.getEdge(), node, toEdge.getEdge(), diff, crossing);
            }
        }
    }

    @Override
    public void run() {
        EdgeExplorer explorer = ghStorage.createEdgeExplorer();

        // iterate over all nodes
        int nodes = ghStorage.getNodes();
        for (int start = 0; start < nodes; start++) {
            EdgeIterator iter = explorer.setBaseNode(start);
            handleSwitch(iter, start);
        }
    }
}
