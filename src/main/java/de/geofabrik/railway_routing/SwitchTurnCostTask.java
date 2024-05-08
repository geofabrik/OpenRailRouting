package de.geofabrik.railway_routing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.LongSet;
import com.graphhopper.reader.osm.OSMNodeData;
import com.graphhopper.reader.osm.RestrictionTagParser;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;


public class SwitchTurnCostTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchTurnCostTask.class);

    AngleCalc angleCalc = new AngleCalc();
    BaseGraph baseGraph;
    OSMParsers osmParsers;
    private LongSet crossingsSet;

    public SwitchTurnCostTask(BaseGraph baseGraph, OSMParsers osmParsers, LongSet crossingsSet) {
        this.baseGraph = baseGraph;
        this.osmParsers = osmParsers;
        this.crossingsSet = crossingsSet;
    }

    private double getAngle(double or1, double or2) {
        return Math.abs(or1 - or2);
    }

    private void addTurnCosts(int fromEdge, int viaNode, int toEdge, double angleDiff, boolean crossing) {
        TurnCostStorage tcs = baseGraph.getTurnCostStorage();
        boolean forbidden = (crossing && (angleDiff < 0.92 * Math.PI || angleDiff > 1.08 * Math.PI));
        forbidden |= (angleDiff < 0.75 * Math.PI || angleDiff > 1.25 * Math.PI);
        List<RestrictionTagParser> restrictionTagParsers = osmParsers.getRestrictionTagParsers();
        for (RestrictionTagParser parser : restrictionTagParsers) {
            BooleanEncodedValue turnCostEnc = parser.getTurnRestrictionEnc();
            if (forbidden) {
                tcs.set(turnCostEnc, fromEdge, viaNode, toEdge, true);
                tcs.set(turnCostEnc, toEdge, viaNode, fromEdge, true);
            }
        }
    }

    private void handleSwitch(EdgeIterator iter, int node) {
        // get list of IDs of edges
        ArrayList<Integer> edges = new ArrayList<Integer>();
        ArrayList<Integer> adjNodes = new ArrayList<Integer>();
        // check if it is a railway crossing
        // We only get tower nodes here.
        long id = OSMNodeData.towerNodeToId(node);
        boolean crossing = crossingsSet.contains(id);
        while (iter.next()) {
            edges.add(iter.getEdge());
            adjNodes.add(iter.getAdjNode());
        }
        for (int i = 0; i < adjNodes.size(); ++i) {
            EdgeIteratorState fromEdge = baseGraph.getEdgeIteratorState(edges.get(i), adjNodes.get(i));
            PointList fromPoints = fromEdge.fetchWayGeometry(FetchMode.ALL);
            double fromLon = fromPoints.getLon(1);
            double fromLat = fromPoints.getLat(1);
            double centreLon = fromPoints.getLon(0);
            double centreLat = fromPoints.getLat(0);
            double fromOrientation = angleCalc.calcOrientation(centreLat, centreLon, fromLat, fromLon);
            for (int j = i + 1; j < adjNodes.size(); ++j) {
                EdgeIteratorState toEdge = baseGraph.getEdgeIteratorState(edges.get(j), adjNodes.get(j));
                PointList toPoints = toEdge.fetchWayGeometry(FetchMode.ALL);
                double toLon = toPoints.getLon(1);
                double toLat = toPoints.getLat(1);
                double toOrientation = angleCalc.calcOrientation(centreLat, centreLon, toLat, toLon);
                double diff = getAngle(fromOrientation, toOrientation);
                addTurnCosts(fromEdge.getEdge(), node, toEdge.getEdge(), diff, crossing);
            }
        }
    }

    public void run() {
        LOGGER.info("Assigning turn costs to impossible turns");
        EdgeExplorer explorer = baseGraph.createEdgeExplorer();

        // iterate over all nodes
        int nodes = baseGraph.getNodes();
        for (int start = 0; start < nodes; start++) {
            EdgeIterator iter = explorer.setBaseNode(start);
            handleSwitch(iter, start);
        }
    }
}
