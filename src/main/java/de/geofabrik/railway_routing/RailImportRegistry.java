package de.geofabrik.railway_routing;

import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.ImportRegistry;
import com.graphhopper.routing.ev.ImportUnit;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.ev.OSMWayID;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.RoadClassLink;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.routing.ev.Roundabout;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.routing.util.parsers.OSMMaxSpeedParser;
import com.graphhopper.routing.util.parsers.OSMRoadClassLinkParser;
import com.graphhopper.routing.util.parsers.OSMRoadClassParser;
import com.graphhopper.routing.util.parsers.OSMRoadEnvironmentParser;
import com.graphhopper.routing.util.parsers.OSMRoundaboutParser;
import com.graphhopper.routing.util.parsers.OSMWayIDParser;

import de.geofabrik.railway_routing.ev.Electrified;
import de.geofabrik.railway_routing.ev.Frequency;
import de.geofabrik.railway_routing.ev.Gauge;
import de.geofabrik.railway_routing.ev.RailwayClass;
import de.geofabrik.railway_routing.ev.Voltage;
import de.geofabrik.railway_routing.parsers.OSMElectrifiedParser;
import de.geofabrik.railway_routing.parsers.OSMFrequencyParser;
import de.geofabrik.railway_routing.parsers.OSMGaugeParser;
import de.geofabrik.railway_routing.parsers.OSMRailwayClassParser;
import de.geofabrik.railway_routing.parsers.OSMVoltageParser;

public class RailImportRegistry implements ImportRegistry {

    public RailImportRegistry() {
    }

    @Override
    public ImportUnit createImportUnit(String name) {
        if (RoadEnvironment.KEY.equals(name))
            return ImportUnit.create(name, props -> RoadEnvironment.create(),
                    (lookup, props) -> new OSMRoadEnvironmentParser(
                            lookup.getEnumEncodedValue(RoadEnvironment.KEY, RoadEnvironment.class))
            );
        else if (Roundabout.KEY.equals(name))
            return ImportUnit.create(name, props -> Roundabout.create(),
                    (lookup, props) -> new OSMRoundaboutParser(
                            lookup.getBooleanEncodedValue(Roundabout.KEY))
            );
        else if (RoadClass.KEY.equals(name))
            return ImportUnit.create(name, props -> RoadClass.create(),
                    (lookup, props) -> new OSMRoadClassParser(
                            lookup.getEnumEncodedValue(RoadClass.KEY, RoadClass.class))
            );
        else if (RoadClassLink.KEY.equals(name))
            return ImportUnit.create(name, props -> RoadClassLink.create(),
                    (lookup, props) -> new OSMRoadClassLinkParser(
                            lookup.getBooleanEncodedValue(RoadClassLink.KEY))
            );
        else if (MaxSpeed.KEY.equals(name))
            return ImportUnit.create(name, props -> MaxSpeed.create(),
                    (lookup, props) -> new OSMMaxSpeedParser(
                            lookup.getDecimalEncodedValue(MaxSpeed.KEY))
            );
        else if (RailwayClass.KEY.equals(name))
            return ImportUnit.create(name, props -> RailwayClass.create(),
                    (lookup, props) -> new OSMRailwayClassParser(
                            lookup.getEnumEncodedValue(RailwayClass.KEY, RailwayClass.class))
            );
        else if (Gauge.KEY.equals(name))
            return ImportUnit.create(name, props -> Gauge.create(),
                    (lookup, props) -> new OSMGaugeParser(
                            lookup.getIntEncodedValue(Gauge.KEY))
            );
        else if (Electrified.KEY.equals(name))
            return ImportUnit.create(name, props -> Electrified.create(),
                    (lookup, props) -> new OSMElectrifiedParser(
                            lookup.getEnumEncodedValue(Electrified.KEY, Electrified.class))
            );
        else if (Voltage.KEY.equals(name))
            return ImportUnit.create(name, props -> Voltage.create(),
                    (lookup, props) -> new OSMVoltageParser(
                            lookup.getIntEncodedValue(Voltage.KEY))
            );
        else if (Frequency.KEY.equals(name))
            return ImportUnit.create(name, props -> Frequency.create(),
                    (lookup, props) -> new OSMFrequencyParser(
                            lookup.getIntEncodedValue(Frequency.KEY))
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
