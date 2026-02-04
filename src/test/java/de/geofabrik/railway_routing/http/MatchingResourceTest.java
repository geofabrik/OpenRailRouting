/*
 *  This file contains code from GraphHopper published under Apache License,
 *  version 2.0.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package de.geofabrik.railway_routing.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphhopper.config.CHProfile;
import com.graphhopper.jackson.ResponsePathDeserializerHelper;
import com.graphhopper.routing.TestProfiles;
import com.graphhopper.util.Helper;

import de.geofabrik.railway_routing.util.RailwayProfiles;
import de.geofabrik.railway_routing.util.RailwayRoutingServerTestConfiguration;
import de.geofabrik.railway_routing.http.RailwayRoutingApplication;
import de.geofabrik.railway_routing.http.RailwayRoutingServerConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@ExtendWith(DropwizardExtensionsSupport.class)
class MatchingResourceTest {

    private static final String DIR = "files";
    private static final String VDE82_PBF = DIR + "/vde8.2-rail.osm.pbf";
    private static final String SAALEBRUECKEN = "/saalebruecken.gpx";
    private static final String GH_LOCATION = "target/graphhopper-test-gh";
    private static final DropwizardAppExtension<RailwayRoutingServerConfiguration> app = new DropwizardAppExtension<>(RailwayRoutingApplication.class, createConfig());

    private static RailwayRoutingServerConfiguration createConfig() {
        RailwayRoutingServerConfiguration config = new RailwayRoutingServerTestConfiguration();
        config.getGraphHopperConfiguration().
                putObject("prepare.min_network_size", 0).
                putObject("datareader.file", VDE82_PBF).
                putObject("graph.encoded_values", "voltage,electrified,frequency,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                putObject("import.osm.ignored_highways", "").
                putObject("graph.location", GH_LOCATION).
                putObject("graph.encoded_values", "voltage,electrified,frequency,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(RailwayProfiles.mapMatchingcreateProfiles());
        return config;
    }

    public static WebTarget clientTarget(DropwizardAppExtension<? extends Configuration> app, String path) {
        path = prefixPathWithSlash(path);
        return app.client().target("http://localhost:" + app.getLocalPort() + path);
    }

    private static String prefixPathWithSlash(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    @BeforeAll
    @AfterAll
    public static void cleanUp() {
        Helper.removeDir(new File(GH_LOCATION));
    }

    @Test
    public void test() {
        final Response response = clientTarget(app, "/match?profile=intercity")
                .request()
                .buildPost(Entity.xml(getClass().getResourceAsStream(SAALEBRUECKEN)))
                .invoke();
        assertEquals(200, response.getStatus());
        JsonNode json = response.readEntity(JsonNode.class);
        JsonNode path = json.get("paths").get(0);

        LineString expectedGeometry = readWktLineString("LINESTRING (11.827171 51.168214,11.835505 51.169041,11.837182 51.169237,11.839895 51.169629,11.842694 51.170135,11.844409 51.1705,11.875694 51.1776)");
        LineString actualGeometry = ResponsePathDeserializerHelper.decodePolyline(path.get("points").asText(), 10, false, 1e5).toLineString(false);
        assertEquals(DiscreteHausdorffDistance.distance(expectedGeometry, actualGeometry), 0.0, 1E-4);
        assertEquals(89, path.get("time").asLong() / 1000f, 1);
        assertEquals(89, json.get("map_matching").get("time").asLong() / 1000f, 1);
        assertEquals(3550, path.get("distance").asDouble(), 1);
        assertEquals(3550, json.get("map_matching").get("distance").asDouble(), 1);
    }


    private LineString readWktLineString(String wkt) {
        WKTReader wktReader = new WKTReader();
        LineString expectedGeometry = null;
        try {
            expectedGeometry = (LineString) wktReader.read(wkt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return expectedGeometry;
    }
}
