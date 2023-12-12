package de.geofabrik.railway_routing.reader;

import static com.graphhopper.util.Helper.nf;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.storage.BaseGraph;

import de.geofabrik.railway_routing.CrossingsSetHook;
import de.geofabrik.railway_routing.SwitchTurnCostTask;

public class OSMRailwayReader extends OSMReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSMRailwayReader.class);

    private CrossingsSetHook crossingsHandler;

    public OSMRailwayReader(BaseGraph baseGraph, EncodingManager encodingManager, OSMParsers osmParsers, OSMReaderConfig config) {
        super(baseGraph, encodingManager, osmParsers, config);
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

        RailwayWaySegmentParser.Builder builder = new RailwayWaySegmentParser.Builder(baseGraph.getNodeAccess());
        builder.setDirectory(baseGraph.getDirectory());
        builder.setElevationProvider(eleProvider);
        builder.setWayFilter(this::acceptWay);
        builder.setSplitNodeFilter(this::isBarrierNode);
        builder.setWayPreprocessor(this::preprocessWay);
        builder.setRelationPreprocessor(this::preprocessRelations);
        builder.setRelationProcessor(this::processRelation);
        builder.setEdgeHandler(this::addEdge);
        builder.setWorkerThreads(config.getWorkerThreads());
        RailwayWaySegmentParser waySegmentParser = builder.build();
        waySegmentParser.setAndInitCrossingsHandler(crossingsHandler);
        waySegmentParser.readOSM(osmFile);
        osmDataDate = waySegmentParser.getTimeStamp();
        if (baseGraph.getNodes() == 0)
            throw new RuntimeException("Graph after reading OSM must not be empty");
        LOGGER.info("Finished reading OSM file: {}, nodes: {}, edges: {}, zero distance edges: {}",
                osmFile.getAbsolutePath(), nf(baseGraph.getNodes()), nf(baseGraph.getEdges()), nf(zeroCounter));
        applyTurnCostsAtSwitches();
        finishedReading();
    }
}
