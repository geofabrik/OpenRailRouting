package de.geofabrik.railway_routing.reader;

import static com.graphhopper.util.Helper.nf;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PointList;

import de.geofabrik.railway_routing.CrossingsSetHook;
import de.geofabrik.railway_routing.SwitchTurnCostTask;
import de.geofabrik.railway_routing.util.MultiValueChecker;

public class OSMRailwayReader extends OSMReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSMRailwayReader.class);

    private CrossingsSetHook crossingsHandler;

    public OSMRailwayReader(BaseGraph baseGraph, OSMParsers osmParsers, OSMReaderConfig config) {
        super(baseGraph, osmParsers, config);
        this.crossingsHandler = new CrossingsSetHook();
    }

    private void applyTurnCostsAtSwitches() {
        SwitchTurnCostTask tct = new SwitchTurnCostTask(baseGraph, osmParsers, crossingsHandler.getCrossingsSet());
        tct.run();
    }

    @Override
    protected boolean isBarrierNode(ReaderNode node) {
        return false;
    }

    /**
     * In addition to upstream implementation, this method calls addEdge() multiple times if the way
     * supports multiple gauges.
     */
    @Override
    protected void addEdge(int fromIndex, int toIndex, PointList pointList, ReaderWay way, List<Map<String, Object>> nodeTags) {
        String gauge = way.getTag("gauge");
        List<Integer> gauges = MultiValueChecker.getNumbersFromTagValue(gauge, Integer::parseInt);
        for (Integer g : gauges) {
            ReaderWay dup = new ReaderWay(way);
            if (g != null) {
                dup.setTag("gauge", Integer.toString(g));
            } else {
                dup.setTag("gauge", null);
            }
            super.addEdge(fromIndex, toIndex, pointList, dup, nodeTags);
        }
    }

    @Override
    public void readGraph() throws IOException {
        if (osmParsers == null)
            throw new IllegalStateException("Tag parsers were not set.");

        if (osmFile == null)
            throw new IllegalStateException("No OSM file specified");

        if (!osmFile.exists())
            throw new IllegalStateException("Your specified OSM file does not exist:" + osmFile.getAbsolutePath());

        if (!baseGraph.isInitialized())
            throw new IllegalStateException("BaseGraph must be initialize before we can read OSM");

        WaySegmentParser waySegmentParser = new WaySegmentParser.Builder(baseGraph.getNodeAccess(), baseGraph.getDirectory())
        .setElevationProvider(eleProvider)
        .setWayFilter(this::acceptWay)
        .setSplitNodeFilter(this::isBarrierNode)
        .setWayPreprocessor(this::preprocessWay)
        .setRelationPreprocessor(this::preprocessRelations)
        .setRelationProcessor(this::processRelation)
        .setEdgeHandler(this::addEdge)
        .setWorkerThreads(config.getWorkerThreads())
        .registerPass2Handler(crossingsHandler)
        .build();
        waySegmentParser.readOSM(osmFile);
        osmDataDate = waySegmentParser.getTimeStamp();
        if (baseGraph.getNodes() == 0)
            throw new RuntimeException("Graph after reading OSM must not be empty");
        LOGGER.info("Finished reading OSM file: {}, nodes: {}, edges: {}, zero distance edges: {}",
                osmFile.getAbsolutePath(), nf(baseGraph.getNodes()), nf(baseGraph.getEdges()), nf(zeroCounter));
        releaseEverythingExceptRestrictionData();
        applyTurnCostsAtSwitches();
        addRestrictionsToGraph();
        releaseRestrictionData();
    }
}
