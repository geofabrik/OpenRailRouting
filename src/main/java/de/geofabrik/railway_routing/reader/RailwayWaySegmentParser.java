package de.geofabrik.railway_routing.reader;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.osm.SkipOptions;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.graphhopper.storage.Directory;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointAccess;
import com.graphhopper.util.StopWatch;

import de.geofabrik.railway_routing.CrossingsSetHook;

public class RailwayWaySegmentParser extends WaySegmentParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaySegmentParser.class);
    private CrossingsSetHook crossingsHandler = null;

    protected RailwayWaySegmentParser(PointAccess nodeAccess, Directory directory, ElevationProvider eleProvider,
            Predicate<ReaderWay> wayFilter, Predicate<ReaderNode> splitNodeFilter, WayPreprocessor wayPreprocessor,
            Consumer<ReaderRelation> relationPreprocessor, RelationProcessor relationProcessor,
            EdgeHandler edgeHandler, int workerThreads) {
        super(nodeAccess, directory, eleProvider, wayFilter, splitNodeFilter, wayPreprocessor,
                relationPreprocessor, relationProcessor, edgeHandler, workerThreads);
    }

    public void setAndInitCrossingsHandler(CrossingsSetHook handler) {
        crossingsHandler = handler;
    }

    /**
     * @param osmFile the OSM file to parse, supported formats include .osm.xml, .osm.gz and .xml.pbf
     */
    @Override
    public void readOSM(File osmFile) {
        if (crossingsHandler == null) {
            throw new IllegalStateException("crossingsHandler was not set.");
        }
        if (nodeData.getNodeCount() > 0)
            throw new IllegalStateException("You can only run way segment parser once");

        LOGGER.info("Start reading OSM file: '" + osmFile + "'");
        LOGGER.info("pass1 - start");
        StopWatch sw1 = StopWatch.started();
        readOSM(osmFile, new SkipOptions(true, false, false), new Pass1Handler());
        LOGGER.info("pass1 - finished, took: {}", sw1.stop().getTimeString());

        long nodes = nodeData.getNodeCount();

        LOGGER.info("Creating graph. Node count (pillar+tower): " + nodes + ", " + Helper.getMemInfo());

        LOGGER.info("pass2 - start");
        StopWatch sw2 = new StopWatch().start();
        Pass2Handler pass2Handler = new Pass2Handler();
        crossingsHandler.setNodeDataAccess(nodeId -> pass2Handler.getInternalNodeIdOfOSMNode(nodeId));
        readOSM(osmFile, SkipOptions.none(), pass2Handler, crossingsHandler);
        LOGGER.info("pass2 - finished, took: {}", sw2.stop().getTimeString());

        nodeData.release();

        LOGGER.info("Finished reading OSM file." +
                " pass1: " + (int) sw1.getSeconds() + "s, " +
                " pass2: " + (int) sw2.getSeconds() + "s, " +
                " total: " + (int) (sw1.getSeconds() + sw2.getSeconds()) + "s");
    }

    public static class Builder extends WaySegmentParser.Builder {
        /**
         * @param nodeAccess used to store tower node coordinates while parsing the ways
         */
        public Builder(PointAccess nodeAccess) {
            super(nodeAccess);
        }
        
        public RailwayWaySegmentParser build() {
            return new RailwayWaySegmentParser(
                    nodeAccess, directory, elevationProvider, wayFilter, splitNodeFilter, wayPreprocessor, relationPreprocessor, relationProcessor,
                    edgeHandler, workerThreads
            );
        }
    }
}
