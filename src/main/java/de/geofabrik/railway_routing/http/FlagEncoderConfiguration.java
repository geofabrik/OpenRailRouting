package de.geofabrik.railway_routing.http;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
