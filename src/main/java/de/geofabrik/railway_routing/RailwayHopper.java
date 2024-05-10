package de.geofabrik.railway_routing;

import java.util.List;

import com.graphhopper.GraphHopper;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.reader.OSMRailwayReader;
import de.geofabrik.railway_routing.reader.RailwayOSMParsers;

public class RailwayHopper extends GraphHopper {

    public RailwayHopper(List<FlagEncoderConfiguration> encoderConfigs) {
        super();
        setImportRegistry(new RailImportRegistry(encoderConfigs));
        setOsmParsersSupplier(RailwayOSMParsers::new);
        setOsmReaderSupplier((baseGraph, osmParsers, config) -> new OSMRailwayReader(baseGraph, osmParsers, config));
    }
}
