package de.geofabrik.railway_routing.reader;

import static com.graphhopper.util.Helper.nf;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.storage.GraphHopperStorage;

import de.geofabrik.railway_routing.CrossingsSetHook;

public class OSMRailwayReader extends OSMReader {
    
    private CrossingsSetHook crossingsHandler;

    public OSMRailwayReader(GraphHopperStorage ghStorage, OSMReaderConfig config) {
        super(ghStorage, config);
    }

    public void setCrossingsHandler(CrossingsSetHook handler) {
        this.crossingsHandler = handler;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OSMRailwayReader.class);

    @Override
    public void readGraph() throws IOException {
        if (encodingManager == null)
            throw new IllegalStateException("Encoding manager was not set.");

        if (osmFile == null)
            throw new IllegalStateException("No OSM file specified");

        if (!osmFile.exists())
            throw new IllegalStateException("Your specified OSM file does not exist:" + osmFile.getAbsolutePath());

        RailwayWaySegmentParser.Builder builder = new RailwayWaySegmentParser.Builder(ghStorage.getNodeAccess());
        builder.setDirectory(ghStorage.getDirectory());
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
        ghStorage.create(100);
        waySegmentParser.readOSM(osmFile);
        osmDataDate = waySegmentParser.getTimeStamp();
        if (ghStorage.getNodes() == 0)
            throw new RuntimeException("Graph after reading OSM must not be empty");
        LOGGER.info("Finished reading OSM file: {}, nodes: {}, edges: {}, zero distance edges: {}",
                osmFile.getAbsolutePath(), nf(ghStorage.getNodes()), nf(ghStorage.getEdges()), nf(zeroCounter));
        finishedReading();
    }
}
