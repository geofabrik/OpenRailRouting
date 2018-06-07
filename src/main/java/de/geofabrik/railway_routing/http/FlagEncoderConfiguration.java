package de.geofabrik.railway_routing.http;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlagEncoderConfiguration {

    @NotEmpty
    private String name;

    private String electrified = "";
    private String voltages = "";
    private String frequencies = "";
    private String gauges = "";
    private String speedFactor = "5";
    private String maxspeed = "90";
    private String yardSpur = "true";

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getElectrified() {
        return electrified;
    }

    @JsonProperty
    public String getVoltages() {
        return voltages;
    }

    @JsonProperty
    public String getFrequencies() {
        return frequencies;
    }

    @JsonProperty
    public String getGauges() {
        return gauges;
    }

    @JsonProperty
    public String getSpeedFactor() {
        return speedFactor;
    }

    @JsonProperty
    public String getMaxspeed() {
        return maxspeed;
    }

    @JsonProperty
    public String getYardSpur() {
        return yardSpur;
    }


    @JsonProperty
    public void setName(String value) {
        name = value;
    }

    @JsonProperty
    public void setElectrified(String value) {
        electrified = value;
    }

    @JsonProperty
    public void setVoltages(String value) {
        voltages = value;
    }

    @JsonProperty
    public void setFrequencies(String value) {
        frequencies = value;
    }

    @JsonProperty
    public void setGauges(String value) {
        gauges = value;
    }

    @JsonProperty
    public void setSpeedFactor(String value) {
        speedFactor = value;
    }

    @JsonProperty
    public void setMaxspeed(String value) {
        maxspeed = value;
    }

    @JsonProperty
    public void setYardSpur(String value) {
        yardSpur = value;
    }
}
