package de.geofabrik.railway_routing;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;

public class CrossingsSetHook implements WaySegmentParser.ReaderElementHandler {

    /** set of internal node IDs which are tagged with railway=railway_crossing in OSM */
    private IntSet nodes;
    private AccessOSMNodeData nodeDataAccess;
    
    public CrossingsSetHook() {
        this.nodes = new IntScatterSet();
    }

    public IntSet getCrossingsSet() {
        return nodes;
    }

    /**
     * Set a function to access the internal ID of a OSM node ID.
     */
    public void setNodeDataAccess(AccessOSMNodeData nodeDataAccess) {
        this.nodeDataAccess = nodeDataAccess;
    }
    
    public void handleNode(ReaderNode node) {
        if (node.hasTag("railway", "railway_crossing")) {
            nodes.add(nodeDataAccess.run(node.getId()));
        }
    }

    public interface AccessOSMNodeData {
        int run(long osmNodeId);
    }
}
