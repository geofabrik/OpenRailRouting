package de.geofabrik.railway_routing;

import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.osm.OSMReader;
import com.graphhopper.reader.osm.OSMReaderHook;
import com.carrotsearch.hppc.IntSet;

public class CrossingsSetHook implements OSMReaderHook {
    private IntSet nodes;
    private OSMReader reader;
    
    public CrossingsSetHook(OSMReader reader, IntSet crossingsSet) {
        this.nodes = crossingsSet;
        this.reader = reader;
    }
    
    public void processNode(ReaderNode node) {
        if (node.hasTag("railway", "railway_crossing")) {
            nodes.add(reader.getNodeMap().get(node.getId()));
        }
    }
}
