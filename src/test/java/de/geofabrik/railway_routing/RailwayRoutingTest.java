package de.geofabrik.railway_routing;

import static com.graphhopper.util.Parameters.Algorithms.ASTAR;
import static com.graphhopper.util.Parameters.Algorithms.ASTAR_BI;
import static com.graphhopper.util.Parameters.Algorithms.DIJKSTRA;
import static com.graphhopper.util.Parameters.Algorithms.DIJKSTRA_BI;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.config.TurnCostsConfig;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters.CH;

import de.geofabrik.railway_routing.util.RailwayProfiles;

class RailwayRoutingTest {
    public static final String DIR = "files";
    private static final String COLOGNE = DIR + "/cologne-railway.osm.pbf";
    private static final String VDE82 = DIR + "/vde8.2-rail.osm.pbf";

    // when creating GH instances make sure to use this as the GH location such that it will be cleaned between tests
    private static final String GH_LOCATION = "target/graphhopper-test-gh";

    @BeforeEach
    @AfterEach
    public void setup() {
        Helper.removeDir(new File(GH_LOCATION));
    }

    void assertRoute(GHResponse response, double distance, int milliseconds, int points,
        double lat1, double lat2) {
        assertFalse(response.hasErrors(), response.getErrors().toString());

        ResponsePath res = response.getBest();
        assertEquals(distance, res.getDistance(), .1);
        assertEquals(milliseconds, res.getTime(), 10);
        assertEquals(points, res.getPoints().size());

        assertEquals(lat1, res.getWaypoints().getLat(0), 1e-6);
        assertEquals(lat2, res.getWaypoints().getLat(1), 1e-6);
    }

    void assertRoute(GHResponse response, int nodes, double distance, int milliseconds, int points,
        double lat1, double lat2) {
        assertRoute(response, distance, milliseconds, points, lat1, lat2);
        assertEquals(nodes, response.getHints().getLong("visited_nodes.sum", 0));
    }

    @ParameterizedTest
    @CsvSource({
            DIJKSTRA + ",false,1044",
            ASTAR + ",false,768",
            DIJKSTRA_BI + ",false,958",
            ASTAR_BI + ",false,848",
            DIJKSTRA_BI + ",true,85",
            ASTAR_BI + ",true,98",
    })
    public void test(String algo, boolean withCH, int expectedVisitedNodes) {
        String profileName = "intercity";
        GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(COLOGNE).
                setEncodedValuesString("gauge,voltage,electrified,frequency,road_environment,max_speed,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(RailwayProfiles.create(profileName, true, 160, true)).
                setStoreOnFlush(true);
        hopper.getCHPreparationHandler()
                .setCHProfiles(new CHProfile(profileName));
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.8513934, 6.9088983, 50.9419345, 6.9600105)
                .setAlgorithm(algo)
                .setProfile("intercity");
        req.putHint(CH.DISABLE, !withCH);
        GHResponse rsp = hopper.route(req);
        assertFalse(rsp.hasErrors(), rsp.getErrors().toString());
        assertEquals(expectedVisitedNodes, rsp.getHints().getLong("visited_nodes.sum", 0));

        ResponsePath res = rsp.getBest();
        assertEquals(13707.0, res.getDistance(), .1);
        assertEquals(549193, res.getTime(), 10);
        assertEquals(141, res.getPoints().size());

        assertEquals(50.8513932, res.getWaypoints().getLat(0), 1e-7);
        assertEquals(50.9419637, res.getWaypoints().getLat(1), 1e-7);
    }

    @Test
    public void testHighspeed() {
        GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(VDE82).
                setEncodedValuesString("voltage,electrified,frequency,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(RailwayProfiles.create("ice", true, 300, true)).
                setStoreOnFlush(true);
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.9721, 11.0388, 51.4764, 11.9878)
                .setAlgorithm(ASTAR)
                .setProfile("ice");
        req.putHint(CH.DISABLE, true);
        GHResponse rsp = hopper.route(req);
        // TODO The encoder for rail_average_speed cannot hold values larger than 155 km/h.
        // Therefore, this test passes and needs to be adapted when the bug is fixed.
        assertRoute(rsp, 578, 91844.0, 2220559, 406, 50.972153, 51.476413);
    }

    @Test
    public void testElectricDieselNoTurnCosts() {
        GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(COLOGNE).
                setEncodedValuesString("gauge,voltage,electrified,frequency,road_environment,max_speed,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(RailwayProfiles.create("electric_freight", true, 80, false), RailwayProfiles.create("diesel_freight", false, 80, false)).
                setStoreOnFlush(true);
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.939893, 7.016251, 50.950945, 7.012517)
                .setAlgorithm(ASTAR)
                .setProfile("diesel_freight");
        req.putHint(CH.DISABLE, true);
        GHResponse rsp = hopper.route(req);
        assertRoute(rsp, 162, 1329.2, 190315, 36, 50.939894, 50.950945);
        req.setProfile("electric_freight");
        GHResponse rsp2 = hopper.route(req);
        assertRoute(rsp2, 195, 3061.7, 217015, 57, 50.939894, 50.950945);
    }

    @Test
    public void testElectricWithTurnCosts() {
        GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(COLOGNE).
                setEncodedValuesString("gauge,voltage,electrified,frequency,road_environment,max_speed,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(
                        RailwayProfiles.create("electric_freight_tr", true, 80, false).
                        setTurnCostsConfig(new TurnCostsConfig(List.of("train"), 300))
                    ).
                setStoreOnFlush(true);
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.939893, 7.016251, 50.950945, 7.012517)
                .setAlgorithm(ASTAR)
                .setProfile("electric_freight_tr");
        req.putHint(CH.DISABLE, true);
        GHResponse rsp = hopper.route(req);
        assertRoute(rsp, 483, 3084.8, 518053, 58, 50.939894, 50.950945);
    }

    @Test
    public void testZigZagTurnCosts() {
        GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(COLOGNE).
                setEncodedValuesString("gauge,rail_access,rail_average_speed,railway_class,preferred_direction").
                setProfiles(
                    RailwayProfiles.create("diesel_freight_tr", false, 80, false).
                        setTurnCostsConfig(new TurnCostsConfig(List.of("train"), 300)),
                    RailwayProfiles.create("diesel_freight", false, 80, false)
                    ).
                setStoreOnFlush(true);
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.918903,6.718054, 50.918551,6.719792)
                .setAlgorithm(ASTAR)
                .setProfile("diesel_freight_tr");
        req.putHint(CH.DISABLE, true);
        GHResponse rsp = hopper.route(req);
        assertRoute(rsp, 2966.3, 753766, 29, 50.918903, 50.918549);
        req.setProfile("diesel_freight");
        GHResponse rsp2 = hopper.route(req);
        assertRoute(rsp2, 2026.8, 111486, 23, 50.918903, 50.918549);
    }

}
