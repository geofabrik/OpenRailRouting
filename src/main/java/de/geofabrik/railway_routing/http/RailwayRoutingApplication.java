/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import java.util.EnumSet;
import jakarta.servlet.DispatcherType;

import com.graphhopper.application.resources.RootResource;
import com.graphhopper.http.CORSFilter;

import io.dropwizard.core.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

public final class RailwayRoutingApplication extends Application<RailwayRoutingServerConfiguration> {

    public static void main(String[] args) throws Exception {
        new RailwayRoutingApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<RailwayRoutingServerConfiguration> bootstrap) {
        bootstrap.addBundle(new RailwayRoutingBundle());
        bootstrap.addCommand(new RailwayImportCommand());
        bootstrap.addCommand(new RailwayMatchCommand());
        bootstrap.addBundle(new AssetsBundle("/map-matching-frontend/", "/map-matching/", "index.html", "map-matching-frontend"));
        bootstrap.addBundle(new AssetsBundle("/de/geofabrik/openrailrouting/maps/", "/maps/", "index.html", "openrailrouting-frontend"));
    }

    @Override
    public void run(RailwayRoutingServerConfiguration configuration, Environment environment) throws Exception {

        environment.jersey().register(new RootResource());
        environment.servlets().addFilter("cors", CORSFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "*");

    }
}
