package de.geofabrik.railway_routing;

import java.util.List;
import java.util.Map;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.TurnCost;
import com.graphhopper.routing.ev.TurnRestriction;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.routing.util.VehicleEncodedValuesFactory;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailEncodedValuesFactory implements VehicleEncodedValuesFactory {

    private Map<String, PMap> flagEncoderProperties;

    public RailEncodedValuesFactory(List<FlagEncoderConfiguration> encoderConfigs) {
        flagEncoderProperties = FlagEncoderConfiguration.toPMaps(encoderConfigs);
    }

    @Override
    public VehicleEncodedValues createVehicleEncodedValues(String name, PMap configuration) {
        if (name == null) {
            return createRailwayEncodedValues(configuration.getString("name", "rail"), configuration);
        }
        return createRailwayEncodedValues(name, configuration);
    }

    private VehicleEncodedValues createRailwayEncodedValues(String name, PMap encoderConfiguration) {
        PMap properties = flagEncoderProperties.get(name);
        if (properties == null) {
            properties = new PMap();
        }
        properties.putAll(encoderConfiguration);
        int speedBits = properties.getInt("speed_bits", 5);
        double speedFactor = properties.getDouble("speed_factor", 5);
        boolean speedTwoDirections = properties.getBool("speed_two_directions", true);
        BooleanEncodedValue accessEnc = VehicleAccess.create(name);
        DecimalEncodedValue speedEnc = VehicleSpeed.create(name, speedBits, speedFactor, speedTwoDirections);
        BooleanEncodedValue turnRestrictionEnc = TurnRestriction.create(name);
        return new VehicleEncodedValues(name, accessEnc, speedEnc, null, turnRestrictionEnc);
    }
}
