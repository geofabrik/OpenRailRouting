package de.geofabrik.railway_routing.reader;

import java.util.List;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.RestrictionTagParser;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.parsers.RelationTagParser;
import com.graphhopper.routing.util.parsers.TagParser;

public class RailwayOSMParsers extends OSMParsers {

    public RailwayOSMParsers() {
        super();
    }

    public RailwayOSMParsers(List<String> ignoredHighways, List<TagParser> wayTagParsers,
            List<RelationTagParser> relationTagParsers, List<RestrictionTagParser> restrictionTagParsers) {
        super(ignoredHighways, wayTagParsers, relationTagParsers, restrictionTagParsers);
    }

    public boolean acceptWay(ReaderWay way) {
        return way.getTag("railway") != null;
    }
}
