package de.geofabrik.railway_routing;

import java.util.LinkedList;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

public class RailFlagEncoderFactory {

    public static FlagEncoder createFlagEncoder(FlagEncoderConfiguration config) {
        PMap properties = new PMap();
        properties.put(RailFlagEncoder.NAME, config.getName());
        properties.put(RailFlagEncoder.RAILWAY, config.getRailway());
        properties.put(RailFlagEncoder.ELECTRIFIED, config.getElectrified());
        properties.put(RailFlagEncoder.VOLATAGES, config.getVoltages());
        properties.put(RailFlagEncoder.FREQUENCIES, config.getFrequencies());
        properties.put(RailFlagEncoder.GAUGES, config.getGauges());
        properties.put(RailFlagEncoder.MAXSPEED, config.getMaxspeed());
        properties.put(RailFlagEncoder.SPEED_FACTOR, config.getSpeedFactor());
        return new RailFlagEncoder(properties);
    }

    public static String[] getKnownEncoderNames() {
        String[] names ={"freight_electric_15kvac_25kvac", "freight_diesel",
            "tgv_15kvac25kvac1.5kvdc", "tgv_25kvac1.5kvdc3kvdc", "freight_25kvac1.5kvdc3kvdc"};
        return names;
    }

    public static FlagEncoder createFlagEncoder(String name) {
        PMap properties = new PMap();
        properties.put(RailFlagEncoder.NAME, name);
        if (name.equals("freight_electric_15kvac_25kvac")) {
            properties.put(RailFlagEncoder.ELECTRIFIED, "contact_line");
            properties.put(RailFlagEncoder.VOLATAGES, "15000;25000");
            properties.put(RailFlagEncoder.FREQUENCIES, "16.7;16.67;50");
            properties.put(RailFlagEncoder.GAUGES, "1435");
            properties.put(RailFlagEncoder.MAXSPEED, 90);
        } else if (name.equals("freight_diesel")) {
            properties.put(RailFlagEncoder.ELECTRIFIED, "");
            properties.put(RailFlagEncoder.GAUGES, "1435");
            properties.put(RailFlagEncoder.MAXSPEED, 90);
        } else if (name.equals("tgv_15kvac25kvac1.5kvdc")) {
            properties.put(RailFlagEncoder.ELECTRIFIED, "contact_line");
            properties.put(RailFlagEncoder.VOLATAGES, "15000;25000;1500");
            properties.put(RailFlagEncoder.FREQUENCIES, "16.7;16.67;50;0");
            properties.put(RailFlagEncoder.GAUGES, "1435");
            properties.put(RailFlagEncoder.MAXSPEED, 319);
            properties.put(RailFlagEncoder.SPEED_FACTOR, 11);
        } else if (name.equals("tgv_25kvac1.5kvdc3kvdc")) {
            properties.put(RailFlagEncoder.ELECTRIFIED, "contact_line");
            properties.put(RailFlagEncoder.VOLATAGES, "25000;3000;1500");
            properties.put(RailFlagEncoder.FREQUENCIES, "0;50");
            properties.put(RailFlagEncoder.GAUGES, "1435");
            properties.put(RailFlagEncoder.MAXSPEED, 319);
            properties.put(RailFlagEncoder.SPEED_FACTOR, 11);
        } else if (name.equals("freight_25kvac1.5kvdc3kvdc")) {
            properties.put(RailFlagEncoder.ELECTRIFIED, "contact_line");
            properties.put(RailFlagEncoder.VOLATAGES, "25000;3000;1500");
            properties.put(RailFlagEncoder.FREQUENCIES, "0;50");
            properties.put(RailFlagEncoder.GAUGES, "1435");
            properties.put(RailFlagEncoder.MAXSPEED, 90);
        } else {
            throw new IllegalArgumentException("Profile " + name + " not found.");
        }
        if (properties.isEmpty()) {
            return null;
        }
        return new RailFlagEncoder(properties);
    }

    public static FlagEncoder[] createEncoders(String[] names) {
        LinkedList<FlagEncoder> encoders = new LinkedList<FlagEncoder>();
        for (String s : names) {
            encoders.add(createFlagEncoder(s));
        }
        if (encoders.size() > 0) {
            return encoders.toArray(new FlagEncoder[encoders.size()]);
        }
        return null;
    }

}
