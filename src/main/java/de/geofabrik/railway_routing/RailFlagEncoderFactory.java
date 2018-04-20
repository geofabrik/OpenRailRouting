package de.geofabrik.railway_routing;

import java.util.LinkedList;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.PMap;

public class RailFlagEncoderFactory {

    public static FlagEncoder createFlagEncoder(String name) {
        PMap properties = new PMap();
        properties.put("name", name);
        if (name.equals("freight_electric_15kvac_25kvac")) {
            properties.put("electrifiedValues", "contact_line");
            properties.put("acceptedVoltages", "15000;25000");
            properties.put("acceptedFrequencies", "16.7;16.67;50");
            properties.put("acceptedGauges", "1435");
            properties.put("max_speed", 90);
        } else if (name.equals("freight_diesel")) {
            properties.put("electrifiedValues", "");
            properties.put("acceptedGauges", "1435");
            properties.put("max_speed", 90);
        } else if (name.equals("tgv_15kvac25kvac1.5kvdc")) {
            properties.put("electrifiedValues", "contact_line");
            properties.put("acceptedVoltages", "15000;25000;1500");
            properties.put("acceptedFrequencies", "16.7;16.67;50;0");
            properties.put("acceptedGauges", "1435");
            properties.put("max_speed", 319);
            properties.put("speedFactor", 11);
        } else if (name.equals("tgv_25kvac1.5kvdc3kvdc")) {
            properties.put("electrifiedValues", "contact_line");
            properties.put("acceptedVoltages", "25000;3000;1500");
            properties.put("acceptedFrequencies", "0;50");
            properties.put("acceptedGauges", "1435");
            properties.put("max_speed", 319);
            properties.put("speedFactor", 11);
        } else if (name.equals("freight_25kvac1.5kvdc3kvdc")) {
            properties.put("electrifiedValues", "contact_line");
            properties.put("acceptedVoltages", "25000;3000;1500");
            properties.put("acceptedFrequencies", "0;50");
            properties.put("acceptedGauges", "1435");
            properties.put("max_speed", 90);
        } else {
            throw new IllegalArgumentException("Profile " + name + " not found.");
        }
        if (properties.isEmpty()) {
            return null;
        }
        return new RailFlagEncoder(properties);
    }
    
    public static FlagEncoder[] craeateEncoders(String[] names) {
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
