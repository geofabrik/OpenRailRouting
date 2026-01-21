package de.geofabrik.railway_routing;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.ElseIf;
import static com.graphhopper.json.Statement.Else;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;
import static com.graphhopper.util.Parameters.Algorithms.ASTAR;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters.CH;

class RailwayRoutingTest {
    public static final String DIR = "files";
    private static final String COLOGNE = DIR + "/cologne-railway.osm.pbf";

    // when creating GH instances make sure to use this as the GH location such that it will be cleaned between tests
    private static final String GH_LOCATION = "target/graphhopper-test-gh";

    @BeforeEach
    @AfterEach
    public void setup() {
        Helper.removeDir(new File(GH_LOCATION));
    }

    private static Profile intercityProfile() {
        Profile profile = new Profile("intercity");
        CustomModel customModel = new CustomModel().
                addToPriority(If("!rail_access || railway_class != RAIL", MULTIPLY, "0")).
                addToPriority(If("!preferred_direction", MULTIPLY, "0.7")).
                addToSpeed(If("true", LIMIT, "rail_average_speed")).
                addToSpeed(If("railway_service == YARD || railway_service == SPUR", MULTIPLY, "0.0")).
                addToSpeed(ElseIf("!(electrified == CONTACT_LINE || electrified == UNSET)", MULTIPLY, "0.0")).
                addToSpeed(ElseIf("voltage >= 14000.0 && voltage <= 16000.0 && frequency >= 15.0 && frequency <= 17.5", LIMIT, "160")).
                addToSpeed(ElseIf("voltage == 0.0 && frequency == 0.0", LIMIT, "160")).
                addToSpeed(Else(MULTIPLY, "0.0"));
        profile.setCustomModel(customModel);
        return profile;
    }

	@Test
	public void test() {
		GraphHopper hopper = new RailwayHopper().
                setGraphHopperLocation(GH_LOCATION).
                setOSMFile(COLOGNE).
                setEncodedValuesString("gauge,voltage,electrified,frequency,road_environment,max_speed,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(intercityProfile()).
                setStoreOnFlush(true);
//        hopper.getCHPreparationHandler()
//                .setCHProfiles(new CHProfile("profile"));
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
        GHRequest req = new GHRequest(50.8513934, 6.9088983, 50.9419345, 6.9600105)
                .setAlgorithm(ASTAR)
                .setProfile("intercity");
        req.putHint(CH.DISABLE, true);
        GHResponse rsp = hopper.route(req);
        assertFalse(rsp.hasErrors(), rsp.getErrors().toString());
        assertEquals(768, rsp.getHints().getLong("visited_nodes.sum", 0));

        ResponsePath res = rsp.getBest();
        assertEquals(14000.0, res.getDistance(), .1);
        assertEquals(600000, res.getTime(), 10);
        assertEquals(250, res.getPoints().size());

        assertEquals(50.8513929, res.getWaypoints().getLat(0), 1e-7);
        assertEquals(50.9419607, res.getWaypoints().getLat(1), 1e-7);
    }

}
