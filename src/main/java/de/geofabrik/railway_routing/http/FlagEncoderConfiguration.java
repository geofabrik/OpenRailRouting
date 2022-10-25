package de.geofabrik.railway_routing.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.RailFlagEncoder;

public class FlagEncoderConfiguration {

    @NotEmpty
    @JsonProperty
    private String name;

    @JsonProperty
    private String railway = "rail";

    @JsonProperty
    private String electrified = "";

    @JsonProperty
    private String voltages = "";

    @JsonProperty
    private String frequencies = "";

    @JsonProperty
    private String gauges = "";

    @JsonProperty
    @JsonAlias("speed_factor")
    private double speedFactor = 5;

    @JsonProperty
    private int maxspeed = 90;

    @JsonProperty
    @JsonAlias("yard_spur")
    private boolean yardSpur = true;

    public String getName() {
        return name;
    }

    public String getRailway() {
        return railway;
    }

    public String getElectrified() {
        return electrified;
    }

    public String getVoltages() {
        return voltages;
    }

    public String getFrequencies() {
        return frequencies;
    }

    public String getGauges() {
        return gauges;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }

    public int getMaxspeed() {
        return maxspeed;
    }

    public boolean getYardSpur() {
        return yardSpur;
    }

    public static Map<String, PMap> toPMaps(List<FlagEncoderConfiguration> configs) {
        Map<String, PMap> map = new LinkedHashMap<String, PMap>();
        for (FlagEncoderConfiguration config : configs) {
            PMap properties = config.toPMap();
            map.put(config.getName(), properties);
        }
        return map;
    }

    public static final String NAME = "name";
    public static final String RAILWAY = "railwayValues";
    public static final String ELECTRIFIED = "electrifiedValues";
    public static final String VOLATAGES = "acceptedVoltages";
    public static final String FREQUENCIES = "acceptedFrequencies";
    public static final String GAUGES = "acceptedGauges";
    public static final String MAXSPEED = "max_speed";
    public static final String SPEED_FACTOR = "speed_factor";
    public static final String ACCEPT_YARD_SPUR = "yardSpur";

    public PMap toPMap() {
        PMap properties = new PMap();
        properties.putObject(NAME, name);
        properties.putObject(RAILWAY, railway);
        properties.putObject(ELECTRIFIED, electrified);
        properties.putObject(VOLATAGES, voltages);
        properties.putObject(FREQUENCIES, frequencies);
        properties.putObject(GAUGES, gauges);
        properties.putObject(MAXSPEED, maxspeed);
        properties.putObject(SPEED_FACTOR, speedFactor);
        properties.putObject(ACCEPT_YARD_SPUR, yardSpur);
        return properties;
    }
}
