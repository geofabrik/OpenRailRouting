package de.geofabrik.railway_routing;

import java.util.List;
import java.util.Map;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParser;
import com.graphhopper.routing.util.VehicleTagParserFactory;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailFlagEncoderFactory implements VehicleTagParserFactory {

    private Map<String, PMap> flagEncoderProperties;

    public RailFlagEncoderFactory(List<FlagEncoderConfiguration> encoderConfigs) {
        flagEncoderProperties = FlagEncoderConfiguration.toPMaps(encoderConfigs);
    }

    @Override
    public VehicleTagParser createParser(EncodedValueLookup lookup, String name, PMap properties) {
        if (!properties.has(FlagEncoderConfiguration.NAME)) {
            properties.putObject(FlagEncoderConfiguration.NAME, name);
        }
        if (flagEncoderProperties.containsKey(name)) {
            properties.putAll(flagEncoderProperties.get(name));
        } else {
            throw new IllegalArgumentException("Encoder " + name + " not found. Available encoders: "
                    + flagEncoderProperties.values().toString());
        }
        return new RailFlagEncoder(lookup, properties);
    }

}
