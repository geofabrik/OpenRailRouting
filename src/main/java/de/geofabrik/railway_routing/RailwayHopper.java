package de.geofabrik.railway_routing;


import static com.graphhopper.util.Helper.getMemInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;
import com.graphhopper.coll.GHLongLongHashMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.OSMReaderHook;
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

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailwayHopper extends GraphHopperOSM {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** set of internal node IDs which are tagged with railway=railway_crossing in OSM */
    private IntSet crossingsSet = null;

    public RailwayHopper(final CmdArgs args, final List<FlagEncoderConfiguration> encoderConfigs) {
        if (args.get("datareader.file", "").equals("")) {
            logger.error("Missing argument graphhopper.datareader.file=<OSM file>");
            System.exit(1);
        }
        setTraversalMode(TraversalMode.EDGE_BASED_2DIR);
        String[] knownEncoderNames = RailFlagEncoderFactory.getKnownEncoderNames();
        HashSet<String> knownEncoders = new HashSet<String>(Arrays.asList(knownEncoderNames));
        String[] encoderNames = args.get("profiles", "").split(",");
        FlagEncoder[] encoders = new FlagEncoder[encoderNames.length];
        for (int i = 0; i < encoderNames.length; ++i) {
            String encoderName = encoderNames[i];
            if (knownEncoders.contains(encoderName)) {
                encoders[i] = RailFlagEncoderFactory.createFlagEncoder(encoderName);
            } else {
                for (FlagEncoderConfiguration c : encoderConfigs) {
                    if (c.getName().equals(encoderName)) {
                        encoders[i] = RailFlagEncoderFactory.createFlagEncoder(c);
                        break;
                    }
                }
                if (encoders[i] == null) {
                    throw new IllegalArgumentException("Could not find properties for flag encoder '" + encoderName + "'");
                }
            }
        }
        setEncodingManager(new EncodingManager(encoders));
        super.init(args);
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        crossingsSet = new IntScatterSet();
        OSMReader reader = new OSMReader(ghStorage);
        OSMReaderHook hook = new CrossingsSetHook(reader, crossingsSet);
        reader.register(hook);
        return initDataReader(reader);
    }

    protected DataReader importData() throws IOException {
        ensureWriteAccess();
        if (getGraphHopperStorage() == null)
            throw new IllegalStateException("Load graph before importing OSM data");

        if (getDataReaderFile() == null)
            throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
                    + " but also cannot use file for DataReader as it wasn't specified!");

        getEncodingManager().setEnableInstructions(isEnableInstructions());
        getEncodingManager().setPreferredLanguage(getPreferredLanguage());
        DataReader reader = createReader(getGraphHopperStorage());
        logger.info("using " + getGraphHopperStorage().toString() + ", memory:" + getMemInfo());
        reader.readGraph();
        return reader;
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

    private void addTurnCosts(TurnCostExtension tcs, int fromEdge, int viaNode, int toEdge, double angleDiff, boolean crossing) {
        long flags = 0;
        for (FlagEncoder encoder : getEncodingManager().fetchEdgeEncoders()) {
            if (!encoder.supports(TurnWeighting.class)) {
                continue;
            }
            if (crossing && (angleDiff < 0.92 * Math.PI || angleDiff > 1.08 * Math.PI)) {
                flags = flags | encoder.getTurnFlags(true, 0);
            } else if (crossing) {
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

    private void handleSwitch(EdgeIterator iter, AngleCalc angleCalc, GraphHopperStorage ghs, int node) {
        TurnCostExtension tcs = (TurnCostExtension) ghs.getExtension();
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
                addTurnCosts(tcs, fromEdge.getEdge(), node, toEdge.getEdge(), diff, crossing);
            }
        }
    }

    protected void cleanUp() {
        super.cleanUp();
        AngleCalc angleCalc = new AngleCalc();
        GraphHopperStorage ghs = getGraphHopperStorage();
        EdgeExplorer explorer = ghs.createEdgeExplorer();

        // iterate over all nodes
        int nodes = ghs.getNodes();
        for (int start = 0; start < nodes; start++) {
            if (ghs.isNodeRemoved(start)) {
                continue;
            }
            EdgeIterator iter = explorer.setBaseNode(start);
            handleSwitch(iter, angleCalc, ghs, start);
        }
    }
}
