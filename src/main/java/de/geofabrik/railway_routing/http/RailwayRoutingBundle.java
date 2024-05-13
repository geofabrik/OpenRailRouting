/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.List;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.http.GHJerseyViolationExceptionMapper;
import com.graphhopper.http.GHRequestTransformer;
import com.graphhopper.http.IllegalArgumentExceptionMapper;
//import com.graphhopper.http.LegacyProfileResolver;
import com.graphhopper.http.MultiExceptionGPXMessageBodyWriter;
import com.graphhopper.http.MultiExceptionMapper;
import com.graphhopper.http.TypeGPXFilter;
import com.graphhopper.http.health.GraphHopperHealthCheck;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.resources.HealthCheckResource;
import com.graphhopper.resources.I18NResource;
import com.graphhopper.resources.InfoResource;
import com.graphhopper.resources.IsochroneResource;
import com.graphhopper.resources.MVTResource;
import com.graphhopper.resources.MapMatchingResource;
import com.graphhopper.resources.NearestResource;
import com.graphhopper.resources.RouteResource;
import com.graphhopper.resources.SPTResource;
import com.graphhopper.http.ProfileResolver;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetailsBuilderFactory;

import de.geofabrik.railway_routing.RailwayHopper;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;


public class RailwayRoutingBundle implements ConfiguredBundle<RailwayRoutingServerConfiguration> {

    static class TranslationMapFactory implements Factory<TranslationMap> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public TranslationMap provide() {
            return graphHopper.getTranslationMap();
        }

        @Override
        public void dispose(TranslationMap instance) {

        }
    }

    static class BaseGraphFactory implements Factory<BaseGraph> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public BaseGraph provide() {
            return graphHopper.getBaseGraph();
        }

        @Override
        public void dispose(BaseGraph instance) {

        }
    }

    static class EncodingManagerFactory implements Factory<EncodingManager> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public EncodingManager provide() {
            return graphHopper.getEncodingManager();
        }

        @Override
        public void dispose(EncodingManager instance) {

        }
    }

    static class LocationIndexFactory implements Factory<LocationIndex> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public LocationIndex provide() {
            return graphHopper.getLocationIndex();
        }

        @Override
        public void dispose(LocationIndex instance) {

        }
    }

    static class ProfileResolverFactory implements Factory<ProfileResolver> {
        @Inject
        GraphHopper graphHopper;

        @Override
        public ProfileResolver provide() {
            return new ProfileResolver(graphHopper.getProfiles());
        }

        @Override
        public void dispose(ProfileResolver instance) {

        }
    }

    static class GHRequestTransformerFactory implements Factory<GHRequestTransformer> {
        @Override
        public GHRequestTransformer provide() {
            return req -> req;
        }

        @Override
        public void dispose(GHRequestTransformer instance) {
        }
    }

    static class PathDetailsBuilderFactoryFactory implements Factory<PathDetailsBuilderFactory> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public PathDetailsBuilderFactory provide() {
            return graphHopper.getPathDetailsBuilderFactory();
        }

        @Override
        public void dispose(PathDetailsBuilderFactory profileResolver) {

        }
    }

    static class HasElevation implements Factory<Boolean> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public Boolean provide() {
            return graphHopper.hasElevation();
        }

        @Override
        public void dispose(Boolean instance) {

        }
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // See #1440: avoids warning regarding com.fasterxml.jackson.module.afterburner.util.MyClassLoader
        bootstrap.setObjectMapper(io.dropwizard.jackson.Jackson.newMinimalObjectMapper());
        // avoids warning regarding com.fasterxml.jackson.databind.util.ClassUtil
        bootstrap.getObjectMapper().registerModule(new Jdk8Module());

        Jackson.initObjectMapper(bootstrap.getObjectMapper());
        bootstrap.getObjectMapper().setDateFormat(new StdDateFormat());
        // See https://github.com/dropwizard/dropwizard/issues/1558
        bootstrap.getObjectMapper().enable(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING);
    }

    @Override
    public void run(RailwayRoutingServerConfiguration configuration, Environment environment) throws Exception {
        configuration.updateFromSystemProperties();

        // When Dropwizard's Hibernate Validation misvalidates a query parameter,
        // a JerseyViolationException is thrown.
        // With this mapper, we use our custom format for that (backwards compatibility),
        // and also coerce the media type of the response to JSON, so we can return JSON error
        // messages from methods that normally have a different return type.
        // That's questionable, but on the other hand, Dropwizard itself does the same thing,
        // not here, but in a different place (the custom parameter parsers).
        // So for the moment we have to assume that both mechanisms
        // a) always return JSON error messages, and
        // b) there's no need to annotate the method with media type JSON for that.
        //
        // However, for places that throw IllegalArgumentException or MultiException,
        // we DO need to use the media type JSON annotation, because
        // those are agnostic to the media type (could be GPX!), so the server needs to know
        // that a JSON error response is supported. (See below.)
        environment.jersey().register(new GHJerseyViolationExceptionMapper());

        // If the "?type=gpx" parameter is present, sets a corresponding media type header
        environment.jersey().register(new TypeGPXFilter());

        // Take care that IllegalArgumentException and MultiExceptions thrown from the resources
        // come out as JSON or GPX, depending on the media type
        environment.jersey().register(new MultiExceptionMapper());
        environment.jersey().register(new MultiExceptionGPXMessageBodyWriter());

        environment.jersey().register(new IllegalArgumentExceptionMapper());

        runRailwayRouting(configuration.getGraphHopperConfiguration(),
                environment);
    }

    private void runRailwayRouting(GraphHopperConfig configuration, Environment environment) {
        final RailwayRoutingManaged graphHopperManaged = new RailwayRoutingManaged(configuration);
        RailwayHopper hopper = graphHopperManaged.getGraphHopper();
        hopper.getRouterConfig().setNonChMaxWaypointDistance(Integer.parseInt(configuration.getString(
                Parameters.NON_CH.MAX_NON_CH_POINT_DISTANCE, "4000000")
        ));
        environment.lifecycle().manage(graphHopperManaged);
        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(configuration).to(GraphHopperConfig.class);
                //bind(graphHopperManaged).to(RailwayRoutingManaged.class);
                bind(graphHopperManaged.getGraphHopper()).to(GraphHopper.class);

                bindFactory(PathDetailsBuilderFactoryFactory.class).to(PathDetailsBuilderFactory.class);
                bindFactory(ProfileResolverFactory.class).to(ProfileResolver.class);
                bindFactory(GHRequestTransformerFactory.class).to(GHRequestTransformer.class);
                bind(false).to(Boolean.class).named("hasElevation");
                bindFactory(LocationIndexFactory.class).to(LocationIndex.class);
                bindFactory(TranslationMapFactory.class).to(TranslationMap.class);
                bindFactory(EncodingManagerFactory.class).to(EncodingManager.class);
                bindFactory(BaseGraphFactory.class).to(BaseGraph.class);
            }
        });

        environment.jersey().register(MVTResource.class);
        environment.jersey().register(NearestResource.class);
        environment.jersey().register(RouteResource.class);
        environment.jersey().register(IsochroneResource.class);
        environment.jersey().register(MatchResource.class);
        environment.jersey().register(SPTResource.class);
        environment.jersey().register(I18NResource.class);
        environment.jersey().register(InfoResource.class);
        environment.healthChecks().register("graphhopper", new GraphHopperHealthCheck(hopper));
        environment.jersey().register(environment.healthChecks());
        environment.jersey().register(HealthCheckResource.class);
    }
}
