package de.geofabrik.railway_routing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;

import de.geofabrik.railway_routing.http.MatchResource;
import de.geofabrik.railway_routing.util.RailwayProfiles;

class RailwayMapMatchingTest {
    private static final String DIR = "files";
    private static final String VDE82_PBF = DIR + "/vde8.2-rail.osm.pbf";
    private static final String EF_NMB_HAL_GPX = DIR + "/erfurt-naumburg-halle.gpx";
    private static final String VDE82_GPX = DIR + "/vde8.2.gpx";

    // when creating GH instances make sure to use this as the GH location such that it will be cleaned between tests
    private static final String GH_LOCATION = "target/graphhopper-test-gh";
    
    private static RailwayHopper hopper;
    
    @BeforeAll
    public static void setUp() {
        Helper.removeDir(new File(GH_LOCATION));
        hopper = new RailwayHopper();
        hopper.setGraphHopperLocation(GH_LOCATION).
                setOSMFile(VDE82_PBF).
                setEncodedValuesString("voltage,electrified,frequency,rail_access,rail_average_speed,railway_class,railway_service,preferred_direction").
                setProfiles(RailwayProfiles.mapMatchingcreateProfiles()).
                setStoreOnFlush(true);
        hopper.setMinNetworkSize(0);
        hopper.importOrLoad();
    }
    
    @AfterAll
    public static void tearDown() {
        hopper.close();
        Helper.removeDir(new File(GH_LOCATION));
    }

    @ParameterizedTest
    @CsvSource({
        EF_NMB_HAL_GPX + ",intercity,3513512,108402,701",
        EF_NMB_HAL_GPX + ",ice,3513512,108402,701",
        VDE82_GPX + ",intercity,2231659,92019,306",
        VDE82_GPX + ",ice,2231659,92019,306",
    })
    public void test(String gpxPath, String profile, double time, double distance, int pointCount) throws FileNotFoundException {
        PMap hints = new PMap().putObject("profile", profile).putObject(Parameters.CH.DISABLE, true);
        File f = new File(gpxPath);
        List<Observation> measurements;
        FileInputStream stream = new FileInputStream(f);
        measurements = MatchResource.importGpx(stream);
        MapMatching mapMatching = MapMatching.fromGraphHopper(hopper, hints);
        MatchResult mr = mapMatching.match(measurements);
        Translation tr = new TranslationMap().doImport().getWithFallBack(Helper.getLocale("en"));
        ResponsePath p = new PathMerger(mr.getGraph(), mr.getWeighting()).
                doWork(PointList.EMPTY, Collections.singletonList(mr.getMergedPath()), hopper.getEncodingManager(), tr);
        assertEquals(distance, p.getDistance(), 1.0);
        assertEquals(time, p.getTime(), 10.0);
        assertEquals(pointCount, p.getPoints().size());
    }

}

