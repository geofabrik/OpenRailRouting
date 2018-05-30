/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import java.util.List;
import java.util.stream.Collectors;

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
        environment.getObjectMapper().setDateFormat(new ISO8601DateFormat());
        environment.getObjectMapper().registerModule(new JtsModule());
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Because VirtualEdgeIteratorState has getters which throw Exceptions.
        // http://stackoverflow.com/questions/35359430/how-to-make-jackson-ignore-properties-if-the-getters-throw-exceptions
        environment.getObjectMapper().registerModule(new SimpleModule().setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                return beanProperties.stream().map(bpw -> new BeanPropertyWriter(bpw) {
                    @Override
                    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
                        try {
                            super.serializeAsField(bean, gen, prov);
                        } catch (Exception e) {
                            // Ignoring expected exception, see above.
                        }
                    }
                }).collect(Collectors.toList());
            }
        }));

        environment.jersey().register(new RootResource());
    }
}
