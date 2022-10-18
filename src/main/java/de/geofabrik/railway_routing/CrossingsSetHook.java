package de.geofabrik.railway_routing;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.osm.WaySegmentParser;
import com.carrotsearch.hppc.IntSet;

public class CrossingsSetHook implements WaySegmentParser.ReaderElementHandler {
    private IntSet nodes;
    private AccessOSMNodeData nodeDataAccess;
    
    public CrossingsSetHook(IntSet crossingsSet) {
        this.nodes = crossingsSet;
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
