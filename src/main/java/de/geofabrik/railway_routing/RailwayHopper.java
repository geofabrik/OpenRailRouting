package de.geofabrik.railway_routing;

import java.util.List;
import java.util.Map;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.ev.ImportUnit;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.ev.TrainRoute;
import de.geofabrik.railway_routing.parsers.OSMTrainRouteParser;
import de.geofabrik.railway_routing.reader.OSMRailwayReader;
import de.geofabrik.railway_routing.reader.RailwayOSMParsers;

public class RailwayHopper extends GraphHopper {

    public RailwayHopper() {
        super();
        setImportRegistry(new RailImportRegistry());
        setOsmParsersSupplier(RailwayOSMParsers::new);
        setOsmReaderSupplier((baseGraph, osmParsers, config) -> new OSMRailwayReader(baseGraph, osmParsers, config));
    }

    @Override
    protected OSMParsers buildOSMParsers(Map<String, PMap> encodedValuesWithProps,
                                         Map<String, ImportUnit> activeImportUnits,
                                         Map<String, List<String>> restrictionVehicleTypesByProfile,
                                         List<String> ignoredHighways) {
        OSMParsers osmParsers = super.buildOSMParsers(encodedValuesWithProps, activeImportUnits,
                restrictionVehicleTypesByProfile, ignoredHighways);
        if (encodingManager.hasEncodedValue(TrainRoute.KEY))
            osmParsers.addRelationTagParser(relConfig -> new OSMTrainRouteParser(
                    encodingManager.getBooleanEncodedValue(TrainRoute.KEY), relConfig));
        return osmParsers;
    }
}
