/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.http.GHPointConverterProvider;
//import com.graphhopper.http.GraphHopperBundle;
import com.graphhopper.http.GraphHopperBundleConfiguration;
import com.graphhopper.http.IllegalArgumentExceptionMapper;
import com.graphhopper.http.MultiExceptionGPXMessageBodyWriter;
import com.graphhopper.http.MultiExceptionMapper;
import com.graphhopper.http.TypeGPXFilter;
import com.graphhopper.http.health.GraphHopperHealthCheck;
import com.graphhopper.jackson.GraphHopperModule;
import com.graphhopper.resources.I18NResource;
import com.graphhopper.resources.InfoResource;
import com.graphhopper.resources.NearestResource;
import com.graphhopper.resources.RouteResource;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetail;

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

    static class GraphHopperStorageFactory implements Factory<GraphHopperStorage> {

        @Inject
        GraphHopper graphHopper;

        @Override
        public GraphHopperStorage provide() {
            return graphHopper.getGraphHopperStorage();
        }

        @Override
        public void dispose(GraphHopperStorage instance) {

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
        bootstrap.getObjectMapper().setDateFormat(new ISO8601DateFormat());
        bootstrap.getObjectMapper().registerModule(new JtsModule());
        bootstrap.getObjectMapper().registerModule(new GraphHopperModule());
        bootstrap.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Because VirtualEdgeIteratorState has getters which throw Exceptions.
        // http://stackoverflow.com/questions/35359430/how-to-make-jackson-ignore-properties-if-the-getters-throw-exceptions
        bootstrap.getObjectMapper().registerModule(new SimpleModule().setSerializerModifier(new BeanSerializerModifier() {
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
    }

    public void run(RailwayRoutingServerConfiguration configuration, Environment environment) throws Exception {
        configuration.getGraphHopperConfiguration().merge(CmdArgs.readFromSystemProperties());

        // If the "?type=gpx" parameter is present, sets a corresponding media type header
        environment.jersey().register(new TypeGPXFilter());

        // Take care that IllegalArgumentException and MultiExceptions thrown from the resources
        // come out as JSON or GPX, depending on the media type
        environment.jersey().register(new MultiExceptionMapper());
        environment.jersey().register(new MultiExceptionGPXMessageBodyWriter());

        environment.jersey().register(new IllegalArgumentExceptionMapper());

        runRailwayRouting(configuration.getGraphHopperConfiguration(), configuration.getFlagEncoderConfigurations(),
                environment);
    }

    private void runRailwayRouting(CmdArgs configuration, List<FlagEncoderConfiguration> encoderConfig, Environment environment) {
        final RailwayRoutingManaged graphHopperManaged = new RailwayRoutingManaged(configuration, encoderConfig);
        graphHopperManaged.getGraphHopper().setGraphHopperLocation(configuration.get("graph.location", "./graph-cache"));
        environment.lifecycle().manage(graphHopperManaged);
        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(configuration).to(CmdArgs.class);
                bind(graphHopperManaged).to(RailwayRoutingManaged.class);
                bind(graphHopperManaged.getGraphHopper()).to(GraphHopper.class);
                bind(graphHopperManaged.getGraphHopper()).to(GraphHopperAPI.class);

                bind(false).to(Boolean.class).named("hasElevation");
                bindFactory(LocationIndexFactory.class).to(LocationIndex.class);
                bindFactory(TranslationMapFactory.class).to(TranslationMap.class);
                bindFactory(EncodingManagerFactory.class).to(EncodingManager.class);
                bindFactory(GraphHopperStorageFactory.class).to(GraphHopperStorage.class);
            }
        });

        environment.jersey().register(NearestResource.class);
        environment.jersey().register(RouteResource.class);
        environment.jersey().register(MatchResource.class);
        environment.jersey().register(I18NResource.class);
        environment.jersey().register(InfoResource.class);

        SimpleModule pathDetailModule = new SimpleModule();
        pathDetailModule.addSerializer(PathDetail.class, new PathDetailSerializer());
        pathDetailModule.addDeserializer(PathDetail.class, new PathDetailDeserializer());
        environment.getObjectMapper().registerModule(pathDetailModule);
        environment.healthChecks().register("graphhopper", new GraphHopperHealthCheck(graphHopperManaged.getGraphHopper()));
    }

    public static class PathDetailSerializer extends JsonSerializer<PathDetail> {

        @Override
        public void serialize(PathDetail value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();

            gen.writeNumber(value.getFirst());
            gen.writeNumber(value.getLast());

            if (value.getValue() instanceof Double)
                gen.writeNumber((Double) value.getValue());
            else if (value.getValue() instanceof Long)
                gen.writeNumber((Long) value.getValue());
            else if (value.getValue() instanceof Integer)
                gen.writeNumber((Integer) value.getValue());
            else if (value.getValue() instanceof Boolean)
                gen.writeBoolean((Boolean) value.getValue());
            else if (value.getValue() instanceof String)
                gen.writeString((String) value.getValue());
            else
                throw new JsonGenerationException("Unsupported type for PathDetail.value" + value.getValue().getClass(), gen);

            gen.writeEndArray();
        }
    }

    public static class PathDetailDeserializer extends JsonDeserializer<PathDetail> {

        @Override
        public PathDetail deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode pathDetail = jp.readValueAsTree();
            if (pathDetail.size() != 3)
                throw new JsonParseException(jp, "PathDetail array must have exactly 3 entries but was " + pathDetail.size());

            JsonNode from = pathDetail.get(0);
            JsonNode to = pathDetail.get(1);
            JsonNode val = pathDetail.get(2);

            PathDetail pd;
            if (val.isBoolean())
                pd = new PathDetail(val.asBoolean());
            else if (val.isLong())
                pd = new PathDetail(val.asLong());
            else if (val.isDouble())
                pd = new PathDetail(val.asDouble());
            else if (val.isTextual())
                pd = new PathDetail(val.asText());
            else
                throw new JsonParseException(jp, "Unsupported type of PathDetail value " + pathDetail.getNodeType().name());

            pd.setFirst(from.asInt());
            pd.setLast(to.asInt());
            return pd;
        }
    }
}
