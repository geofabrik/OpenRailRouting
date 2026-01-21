package de.geofabrik.railway_routing.reader;

import static com.graphhopper.util.Helper.nf;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.PointList;

import de.geofabrik.railway_routing.CrossingsSetHook;
import de.geofabrik.railway_routing.SwitchTurnCostTask;

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

    private ReaderWay duplicateWay(ReaderWay source, String gauge, String electrified, String voltage, String frequency) {
        ReaderWay dup = new ReaderWay(source);
        if (gauge != null) {
            dup.setTag("gauge", gauge);
        } else {
            dup.removeTag("gauge");
        }
        if (electrified != null) {
            dup.setTag("electrified", electrified);
        } else {
            dup.removeTag("electrified");
        }
        if (voltage != null) {
            dup.setTag("voltage", voltage);
        } else {
            dup.removeTag("voltage");
        }
        if (frequency != null) {
            dup.setTag("frequency", frequency);
        } else {
            dup.removeTag("frequency");
        }
        return dup;
    }

    /**
     * In addition to upstream implementation, this method calls addEdge() multiple times if the way
     * supports multiple gauges.
     */
    protected void addEdge(int fromIndex, int toIndex, PointList pointList, ReaderWay way,
            List<Map<String, Object>> nodeTags) {
        duplicateAndAddEdge(fromIndex, toIndex, pointList, way, nodeTags, super::addEdge);
    }

    /**
     * Read multiple string values from a OSM tag value separated by semicolons.
     * @param tagValue OSM tag value
     * @return List of read values or an empty list if nothing could be read.
     */
    static List<String> tagValueToList(String tagValue) {
        List<String> l;
        if (tagValue == null) {
            l = new ArrayList<String>(1);
            l.add(null);
            return l;
        }
        return Arrays.asList(tagValue.split(";"));
    }

    protected void duplicateAndAddEdge(int fromIndex, int toIndex, PointList pointList, ReaderWay way,
            List<Map<String, Object>> nodeTags, AddEdgeCallback callback) {
        String gauge = way.getTag("gauge");
        List<String> gauges = tagValueToList(gauge);
        String electrified = way.getTag("electrified");
        List<String> eleList = tagValueToList(electrified);
        String voltage = way.getTag("voltage");
        List<String> voltages = tagValueToList(voltage);
        String frequency = way.getTag("frequency");
        List<String> frequencies = tagValueToList(frequency);
        // Duplicating ways with multiple power systems is a bit tricky.#
        for (String g : gauges) {
            // Note that electrified=null results in a list of size 1 with a null member.
            if (eleList.size() == 1) {
                if (voltages.size() == frequencies.size()) {
                    for (int i = 0; i < voltages.size(); ++i) {
                        callback.apply(fromIndex, toIndex, pointList.clone(false),
                                duplicateWay(way, g, electrified, voltages.get(i), frequencies.get(i)),
                                nodeTags);
                    }
                } else if (voltages.size() > 1 && frequencies.size() == 1) {
                    for (String v : voltages) {
                        callback.apply(fromIndex, toIndex, pointList.clone(false),
                                duplicateWay(way, g, electrified, v, frequencies.get(0)),
                                nodeTags);
                    }
                } else if (voltages.size() == 1 && frequencies.size() > 1) {
                    for (String f : frequencies) {
                        callback.apply(fromIndex, toIndex, pointList.clone(false),
                                duplicateWay(way, g, electrified, voltages.get(0), f),
                                nodeTags);
                    }
                } else {
                    // If voltage or frequency is missing or the lists differ in size, the other tag is ignored as well.
                    callback.apply(fromIndex, toIndex, pointList.clone(false),
                            duplicateWay(way, g, electrified, null, null),
                            nodeTags);
                }
            } else {
                if (voltages.size() == eleList.size() && frequencies.size() == eleList.size()) {
                    for (int i = 0; i < voltages.size(); ++i) {
                        callback.apply(fromIndex, toIndex, pointList.clone(false),
                            duplicateWay(way, g, eleList.get(i), voltages.get(i), frequencies.get(i)),
                            nodeTags);
                    }
                } else {
                    super.addEdge(fromIndex, toIndex, pointList.clone(false), duplicateWay(way, g, null, null, null), nodeTags);
                }
            }
        }
    }

    @FunctionalInterface
    protected interface AddEdgeCallback {
        void apply(int fromIndex, int toIndex, PointList pointList, ReaderWay way, List<Map<String, Object>> nodeTags);
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
        .setElevationProvider(this::getElevation)
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
        osmDataDate = waySegmentParser.getTimestamp();
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
