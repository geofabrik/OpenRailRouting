package de.geofabrik.railway_routing;


import static com.graphhopper.util.Helper.createFormatter;
import static com.graphhopper.util.Helper.getMemInfo;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.RestrictionTagParser;
import com.graphhopper.reader.osm.conditional.DateRangeParser;
import com.graphhopper.routing.ev.AverageSlope;
import com.graphhopper.routing.ev.BikeNetwork;
import com.graphhopper.routing.ev.Curvature;
import com.graphhopper.routing.ev.FootNetwork;
import com.graphhopper.routing.ev.GetOffBike;
import com.graphhopper.routing.ev.MaxSlope;
import com.graphhopper.routing.ev.MaxSpeed;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.RoadClassLink;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.routing.ev.Roundabout;
import com.graphhopper.routing.ev.RouteNetwork;
import com.graphhopper.routing.ev.Smoothness;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.util.CurvatureCalculator;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.SlopeCalculator;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.util.parsers.BikeCommonAccessParser;
import com.graphhopper.routing.util.parsers.FootAccessParser;
import com.graphhopper.routing.util.parsers.OSMBikeNetworkTagParser;
import com.graphhopper.routing.util.parsers.OSMFootNetworkTagParser;
import com.graphhopper.routing.util.parsers.OSMGetOffBikeParser;
import com.graphhopper.routing.util.parsers.OSMMaxSpeedParser;
import com.graphhopper.routing.util.parsers.OSMRoadAccessParser;
import com.graphhopper.routing.util.parsers.OSMRoadClassLinkParser;
import com.graphhopper.routing.util.parsers.OSMRoadClassParser;
import com.graphhopper.routing.util.parsers.OSMRoadEnvironmentParser;
import com.graphhopper.routing.util.parsers.OSMRoundaboutParser;
import com.graphhopper.routing.util.parsers.OSMSmoothnessParser;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.reader.OSMRailwayReader;
import de.geofabrik.railway_routing.reader.RailwayOSMParsers;

public class RailwayHopper extends GraphHopper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RailwayHopper(List<FlagEncoderConfiguration> encoderConfigs) {
        super();
        setVehicleTagParserFactory(new RailFlagEncoderFactory(encoderConfigs));
        setVehicleEncodedValuesFactory(new RailEncodedValuesFactory(encoderConfigs));
    }

    @Override
    protected void importOSM() {
        if (getOSMFile() == null)
            throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
                    + " but also cannot use file for DataReader as it wasn't specified!");

        logger.info("start creating graph from " + getOSMFile());
        OSMRailwayReader reader = new OSMRailwayReader(getBaseGraph(), getEncodingManager(), getOSMParsers(), getReaderConfig());
        reader.setFile(_getOSMFile());
        reader.setElevationProvider(getElevationProvider());
        logger.info("using " + getBaseGraph().toString() + ", memory:" + getMemInfo());
        createBaseGraphAndProperties();
        try {
            reader.readGraph();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read file " + getOSMFile(), ex);
        }
        DateFormat f = createFormatter();
        getProperties().put("datareader.import.date", f.format(new Date()));
        if (reader.getDataDate() != null) {
            getProperties().put("datareader.data.date", f.format(reader.getDataDate()));
        }
        writeEncodingManagerToProperties();
    }

    protected OSMParsers buildOSMParsers(Map<String, String> vehiclesByName, List<String> encodedValueStrings,
                                         List<String> ignoredHighways, String dateRangeParserString) {
        OSMParsers osmParsers = new RailwayOSMParsers();
        ignoredHighways.forEach(osmParsers::addIgnoredHighway);
        for (String s : encodedValueStrings) {
            TagParser tagParser = getTagParserFactory().create(getEncodingManager(), s, new PMap());
            if (tagParser != null)
                osmParsers.addWayTagParser(tagParser);
        }

        // this needs to be in sync with the default EVs added in EncodingManager.Builder#build. ideally I would like to remove
        // all these defaults and just use the config as the single source of truth
        if (!encodedValueStrings.contains(Roundabout.KEY))
            osmParsers.addWayTagParser(new OSMRoundaboutParser(getEncodingManager().getBooleanEncodedValue(Roundabout.KEY)));
        if (!encodedValueStrings.contains(RoadClass.KEY))
            osmParsers.addWayTagParser(new OSMRoadClassParser(getEncodingManager().getEnumEncodedValue(RoadClass.KEY, RoadClass.class)));
        if (!encodedValueStrings.contains(RoadClassLink.KEY))
            osmParsers.addWayTagParser(new OSMRoadClassLinkParser(getEncodingManager().getBooleanEncodedValue(RoadClassLink.KEY)));
        if (!encodedValueStrings.contains(RoadEnvironment.KEY))
            osmParsers.addWayTagParser(new OSMRoadEnvironmentParser(getEncodingManager().getEnumEncodedValue(RoadEnvironment.KEY, RoadEnvironment.class)));
        if (!encodedValueStrings.contains(MaxSpeed.KEY))
            osmParsers.addWayTagParser(new OSMMaxSpeedParser(getEncodingManager().getDecimalEncodedValue(MaxSpeed.KEY)));
        if (!encodedValueStrings.contains(RoadAccess.KEY))
            osmParsers.addWayTagParser(new OSMRoadAccessParser(getEncodingManager().getEnumEncodedValue(RoadAccess.KEY, RoadAccess.class), OSMRoadAccessParser.toOSMRestrictions(TransportationMode.CAR)));
        if (getEncodingManager().hasEncodedValue(AverageSlope.KEY) || getEncodingManager().hasEncodedValue(MaxSlope.KEY)) {
            if (!getEncodingManager().hasEncodedValue(AverageSlope.KEY) || !getEncodingManager().hasEncodedValue(MaxSlope.KEY))
                throw new IllegalArgumentException("Enable both, average_slope and max_slope");
            osmParsers.addWayTagParser(new SlopeCalculator(getEncodingManager().getDecimalEncodedValue(MaxSlope.KEY),
                    getEncodingManager().getDecimalEncodedValue(AverageSlope.KEY)));
        }
        if (getEncodingManager().hasEncodedValue(Curvature.KEY))
            osmParsers.addWayTagParser(new CurvatureCalculator(getEncodingManager().getDecimalEncodedValue(Curvature.KEY)));

        DateRangeParser dateRangeParser = DateRangeParser.createInstance(dateRangeParserString);
        Set<String> added = new HashSet<>();
        vehiclesByName.forEach((name, vehicleStr) -> {
            VehicleTagParsers vehicleTagParsers = getVehicleTagParserFactory().createParsers(getEncodingManager(), name,
                    new PMap(vehicleStr).putObject("date_range_parser", dateRangeParser));
            if (vehicleTagParsers == null)
                return;
            vehicleTagParsers.getTagParsers().forEach(tagParser -> {
                if (tagParser == null) return;
                if (tagParser instanceof BikeCommonAccessParser) {
                    if (getEncodingManager().hasEncodedValue(BikeNetwork.KEY) && added.add(BikeNetwork.KEY))
                        osmParsers.addRelationTagParser(relConfig -> new OSMBikeNetworkTagParser(getEncodingManager().getEnumEncodedValue(BikeNetwork.KEY, RouteNetwork.class), relConfig));
                    if (getEncodingManager().hasEncodedValue(GetOffBike.KEY) && added.add(GetOffBike.KEY))
                        osmParsers.addWayTagParser(new OSMGetOffBikeParser(getEncodingManager().getBooleanEncodedValue(GetOffBike.KEY)));
                    if (getEncodingManager().hasEncodedValue(Smoothness.KEY) && added.add(Smoothness.KEY))
                        osmParsers.addWayTagParser(new OSMSmoothnessParser(getEncodingManager().getEnumEncodedValue(Smoothness.KEY, Smoothness.class)));
                } else if (tagParser instanceof FootAccessParser) {
                    if (getEncodingManager().hasEncodedValue(FootNetwork.KEY) && added.add(FootNetwork.KEY))
                        osmParsers.addRelationTagParser(relConfig -> new OSMFootNetworkTagParser(getEncodingManager().getEnumEncodedValue(FootNetwork.KEY, RouteNetwork.class), relConfig));
                }
                String turnCostKey = TurnCost.key(new PMap(vehicleStr).getString("name", name));
                if (getEncodingManager().hasEncodedValue(turnCostKey)
                        // need to make sure we do not add the same restriction parsers multiple times
                        && osmParsers.getRestrictionTagParsers().stream().noneMatch(r -> r.getTurnCostEnc().getName().equals(turnCostKey))) {
                    List<String> restrictions = tagParser instanceof AbstractAccessParser
                            ? ((AbstractAccessParser) tagParser).getRestrictions()
                            : OSMRoadAccessParser.toOSMRestrictions(TransportationMode.valueOf(new PMap(vehicleStr).getString("transportation_mode", "VEHICLE")));
                    osmParsers.addRestrictionTagParser(new RestrictionTagParser(restrictions, getEncodingManager().getDecimalEncodedValue(turnCostKey)));
                }
            });
            vehicleTagParsers.getTagParsers().forEach(tagParser -> {
                if (tagParser == null) return;
                osmParsers.addWayTagParser(tagParser);
            });
        });
        return osmParsers;
    }

}
