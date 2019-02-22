/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import java.util.List;
import java.util.stream.Collectors;
import java.util.EnumSet;
import javax.servlet.DispatcherType;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import com.graphhopper.http.resources.RootResource;
import com.graphhopper.http.CORSFilter;

import io.dropwizard.Application;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

public final class RailwayRoutingApplication extends Application<RailwayRoutingServerConfiguration> {

    public static void main(String[] args) throws Exception {
        new RailwayRoutingApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<RailwayRoutingServerConfiguration> bootstrap) {
        bootstrap.addBundle(new RailwayRoutingBundle());
        bootstrap.addBundle(
            new ConfiguredAssetsBundle(
                    ImmutableMap.<String, String>builder()
                    .put("/assets/", "/maps/")
                    .put("/map-matching-frontend/", "/map-matching/")
                    .build(),
            "index.html"
            )
        );
        bootstrap.addCommand(new RailwayImportCommand());
        bootstrap.addCommand(new RailwayMatchCommand());
    }

    @Override
    public void run(RailwayRoutingServerConfiguration configuration, Environment environment) throws Exception {

        environment.jersey().register(new RootResource());
        environment.servlets().addFilter("cors", CORSFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "*");

    }
}
