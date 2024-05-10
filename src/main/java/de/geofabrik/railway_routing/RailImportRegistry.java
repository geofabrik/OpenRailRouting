package de.geofabrik.railway_routing;

import java.util.List;
import java.util.Map;

import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.ImportRegistry;
import com.graphhopper.routing.ev.ImportUnit;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.ev.OSMWayID;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.routing.util.parsers.OSMMaxSpeedParser;
import com.graphhopper.routing.util.parsers.OSMRoadEnvironmentParser;
import com.graphhopper.routing.util.parsers.OSMWayIDParser;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailImportRegistry implements ImportRegistry {

    private Map<String, PMap> flagEncoderProperties;

    public RailImportRegistry(List<FlagEncoderConfiguration> encoderConfigs) {
        flagEncoderProperties = FlagEncoderConfiguration.toPMaps(encoderConfigs);
    }

    @Override
    public ImportUnit createImportUnit(String name) {
        if (RoadEnvironment.KEY.equals(name))
            return ImportUnit.create(name, props -> RoadEnvironment.create(),
                    (lookup, props) -> new OSMRoadEnvironmentParser(
                            lookup.getEnumEncodedValue(RoadEnvironment.KEY, RoadEnvironment.class))
            );
        else if (MaxSpeed.KEY.equals(name))
            return ImportUnit.create(name, props -> MaxSpeed.create(),
                    (lookup, props) -> new OSMMaxSpeedParser(
                            lookup.getDecimalEncodedValue(MaxSpeed.KEY))
            );
        else if (OSMWayID.KEY.equals(name))
            return ImportUnit.create(name, props -> OSMWayID.create(),
                    (lookup, props) -> new OSMWayIDParser(
                            lookup.getIntEncodedValue(OSMWayID.KEY))
            );
        else if (FerrySpeed.KEY.equals(name))
            return ImportUnit.create(name, props -> FerrySpeed.create(),
                    (lookup, props) -> new FerrySpeedCalculator(
                            lookup.getDecimalEncodedValue(FerrySpeed.KEY)));
        else if (VehicleAccess.key("rail").equals(name))
            return ImportUnit.create(name, props -> VehicleAccess.create("rail"),
                    RailAccessParser::new
            );
        else if (VehicleSpeed.key("rail").equals(name))
            return ImportUnit.create(name, props -> new DecimalEncodedValueImpl(
                            name, props.getInt("speed_bits", 5), props.getDouble("speed_factor", 5), props.getBool("speed_two_directions", true)),
                    (lookup, props) -> new RailAverageSpeedParser(lookup, props),
                    "ferry_speed"
            );
        return null;
    }
}
