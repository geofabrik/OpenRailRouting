/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.http.GraphHopperBundleConfiguration;

import io.dropwizard.Configuration;

public class RailwayRoutingServerConfiguration extends Configuration implements GraphHopperBundleConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RailwayRoutingServerConfiguration.class);

    @NotNull
    @JsonProperty
    private final GraphHopperConfig graphhopper = new GraphHopperConfig();

    public RailwayRoutingServerConfiguration() {
    }

    @Override
    public GraphHopperConfig getGraphHopperConfiguration() {
        return graphhopper;
    }

    /**
     * Update GraphHopper configuration with certain values set in the Java system properties (-Ddw.key.subkey=value).
     */
    public void updateFromSystemProperties() {
        for (Object k : System.getProperties().keySet()) {
            if (k instanceof String) {
                String key = (String) k;
                if (key.startsWith("graphhopper.")) {
                    throw new IllegalArgumentException("You need to prefix system parameters with '-Ddw.graphhopper.' instead of '-Dgraphhopper.' see #1879 and #1897");
                } else if (key.startsWith("dw.graphhopper.datareader.file")) {
                    graphhopper.putObject("datareader.file", System.getProperty(key));
                } else if (key.startsWith("dw.graphhopper.graph.location")) {
                    graphhopper.putObject("graph.location", System.getProperty(key));
                } else if (key.startsWith("dw.graphhopper")) {
                    logger.warn("The key " + key + " cannot be supplied using Java system properties.");
                }
            }
        }
    }
}
