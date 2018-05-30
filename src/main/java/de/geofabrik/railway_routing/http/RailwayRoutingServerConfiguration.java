/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.http.GraphHopperBundleConfiguration;
import com.graphhopper.util.CmdArgs;

import io.dropwizard.Configuration;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.dropwizard.bundles.assets.AssetsConfiguration;

public class RailwayRoutingServerConfiguration extends Configuration implements GraphHopperBundleConfiguration, AssetsBundleConfiguration {

    @NotNull
    @JsonProperty
    private final CmdArgs graphhopper = new CmdArgs();

    @Valid
    @JsonProperty
    private final AssetsConfiguration assets = AssetsConfiguration.builder().build();

    @JsonProperty
    private final List<FlagEncoderConfiguration> flagEncoderProperties = new ArrayList<FlagEncoderConfiguration>();

    public RailwayRoutingServerConfiguration() {
    }

    @Override
    public CmdArgs getGraphHopperConfiguration() {
        return graphhopper;
    }

    @Override
    public AssetsConfiguration getAssetsConfiguration() {
        return assets;
    }

    public List<FlagEncoderConfiguration> getFlagEncoderConfigurations() {
        return flagEncoderProperties;
    }
}
