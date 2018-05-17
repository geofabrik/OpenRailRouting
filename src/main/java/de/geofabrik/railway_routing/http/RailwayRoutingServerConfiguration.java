package de.geofabrik.railway_routing.http;

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
}
