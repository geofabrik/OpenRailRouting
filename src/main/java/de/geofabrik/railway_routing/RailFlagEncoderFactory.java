package de.geofabrik.railway_routing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FlagEncoderFactory;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailFlagEncoderFactory implements FlagEncoderFactory {

    private Map<String, PMap> flagEncoderProperties;

    public RailFlagEncoderFactory(List<FlagEncoderConfiguration> encoderConfigs) {
        flagEncoderProperties = new LinkedHashMap<String, PMap>();
        for (FlagEncoderConfiguration config : encoderConfigs) {
            PMap properties = new PMap();
            properties.putObject(RailFlagEncoder.NAME, config.getName());
            properties.putObject(RailFlagEncoder.RAILWAY, config.getRailway());
            properties.putObject(RailFlagEncoder.ELECTRIFIED, config.getElectrified());
            properties.putObject(RailFlagEncoder.VOLATAGES, config.getVoltages());
            properties.putObject(RailFlagEncoder.FREQUENCIES, config.getFrequencies());
            properties.putObject(RailFlagEncoder.GAUGES, config.getGauges());
            properties.putObject(RailFlagEncoder.MAXSPEED, config.getMaxspeed());
            properties.putObject(RailFlagEncoder.SPEED_FACTOR, config.getSpeedFactor());
            //TODO add yard/spur
            flagEncoderProperties.put(config.getName(), properties);
        }
    }

    private void putObjecIfAbsent(PMap map, String key, Object value) {
        if (!map.has(key)) {
            map.putObject(key, value);
        }
    }

    @Override
    public FlagEncoder createFlagEncoder(String name, PMap configuration) {
        PMap properties = new PMap();
        properties.putAll(configuration);
        if (!properties.has(RailFlagEncoder.NAME)) {
            properties.putObject(RailFlagEncoder.NAME, name);
        }
        if (name.equals("freight_electric_15kvac_25kvac")) {
            putObjecIfAbsent(properties, RailFlagEncoder.ELECTRIFIED, "contact_line");
            putObjecIfAbsent(properties, RailFlagEncoder.VOLATAGES, "15000;25000");
            putObjecIfAbsent(properties, RailFlagEncoder.FREQUENCIES, "16.7;16.67;50");
            putObjecIfAbsent(properties, RailFlagEncoder.GAUGES, "1435");
            putObjecIfAbsent(properties, RailFlagEncoder.MAXSPEED, 90);
        } else if (name.equals("freight_diesel")) {
            putObjecIfAbsent(properties, RailFlagEncoder.ELECTRIFIED, "");
            putObjecIfAbsent(properties, RailFlagEncoder.GAUGES, "1435");
            putObjecIfAbsent(properties, RailFlagEncoder.MAXSPEED, 90);
        } else if (name.equals("tgv_15kvac25kvac1.5kvdc")) {
            putObjecIfAbsent(properties, RailFlagEncoder.ELECTRIFIED, "contact_line");
            putObjecIfAbsent(properties, RailFlagEncoder.VOLATAGES, "15000;25000;1500");
            putObjecIfAbsent(properties, RailFlagEncoder.FREQUENCIES, "16.7;16.67;50;0");
            putObjecIfAbsent(properties, RailFlagEncoder.GAUGES, "1435");
            putObjecIfAbsent(properties, RailFlagEncoder.MAXSPEED, 319);
            putObjecIfAbsent(properties, RailFlagEncoder.SPEED_FACTOR, 11);
        } else if (name.equals("tgv_25kvac1.5kvdc3kvdc")) {
            putObjecIfAbsent(properties, RailFlagEncoder.ELECTRIFIED, "contact_line");
            putObjecIfAbsent(properties, RailFlagEncoder.VOLATAGES, "25000;3000;1500");
            putObjecIfAbsent(properties, RailFlagEncoder.FREQUENCIES, "0;50");
            putObjecIfAbsent(properties, RailFlagEncoder.GAUGES, "1435");
            putObjecIfAbsent(properties, RailFlagEncoder.MAXSPEED, 319);
            putObjecIfAbsent(properties, RailFlagEncoder.SPEED_FACTOR, 11);
        } else if (name.equals("freight_25kvac1.5kvdc3kvdc")) {
            putObjecIfAbsent(properties, RailFlagEncoder.ELECTRIFIED, "contact_line");
            putObjecIfAbsent(properties, RailFlagEncoder.VOLATAGES, "25000;3000;1500");
            putObjecIfAbsent(properties, RailFlagEncoder.FREQUENCIES, "0;50");
            putObjecIfAbsent(properties, RailFlagEncoder.GAUGES, "1435");
            putObjecIfAbsent(properties, RailFlagEncoder.MAXSPEED, 90);
        } else if (flagEncoderProperties.containsKey(name)) {
            properties.putAll(flagEncoderProperties.get(name));
        } else {
            throw new IllegalArgumentException("Encoder " + name + " not found. Available encoders: "
                    + flagEncoderProperties.values().toString());
        }
        return new RailFlagEncoder(properties);
    }

}
