package de.geofabrik.sncf_railway_routing;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.util.Modules;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHServer;
import com.graphhopper.http.GraphHopperModule;
import com.graphhopper.http.GraphHopperServletModule;
import com.graphhopper.matching.GPXFile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.InstructionList;

/**
 * Hello world!
 *
 */
public class RailwayRoutingMain {
    private RailwayHopper hopper;
    private CmdArgs commandline_args;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public static void main( String[] args ) {
        new RailwayRoutingMain(CmdArgs.read(args));
    }
    
    private RailwayRoutingMain(CmdArgs args) {
        commandline_args = args;
        String action = commandline_args.get("action", "");
        hopper = new RailwayHopper(args);
        hopper.setGraphHopperLocation("./graph-cache");
        if (action.equals("import")) {
            importOSM();
        } else if (action.equals("web")) {
            web();
        } else if (action.equals("match")) {
            match();
        }
    }
    
    private void importOSM() {
        hopper.importOrLoad();
        hopper.close();
    }

    private void match() {
        logger.info("loading graph from cache");
        hopper.load(hopper.getGraphHopperLocation());
        List<FlagEncoder> flagEncoders = hopper.getEncodingManager().fetchEdgeEncoders();
        FlagEncoder selectedEncoder = null;
        String profile = commandline_args.get("profile", "");
        for (FlagEncoder encoder : flagEncoders) {
            if (encoder.toString().equals(profile)) {
                selectedEncoder = encoder;
            }
        }
        if (selectedEncoder == null) {
            //TODO throw exception
            System.err.println("No encoding manager selected. Please use the 'vehicle' parameter.");
            logger.error("No encoding manager selected. Please use the 'vehicle' parameter.");
            System.exit(1);
        }
        int gpsAccuracy = commandline_args.getInt("gps_accuracy", 40);

        FastestWeighting fastestWeighting = new FastestWeighting(selectedEncoder);
        Weighting turnWeighting = hopper.createTurnWeighting(hopper.getGraphHopperStorage(),
                fastestWeighting, hopper.getTraversalMode());
        AlgorithmOptions opts = AlgorithmOptions.start()
                .traversalMode(hopper.getTraversalMode())
                .maxVisitedNodes(commandline_args.getInt("max_nodes_to_visit", 10000))
                .weighting(turnWeighting)
                .hints(new HintsMap().put("vehicle", profile))
                .build();

        MapMatching mapMatching = new MapMatching(hopper, opts);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);

        String inputPath = commandline_args.get("gpx.location", "");
        if (inputPath.equals("")) {
            //TODO throw exception
            System.err.println("No input file given.");
            System.exit(1);
        }

        List<GPXEntry> inputGPXEntries = new GPXFile().doImport(inputPath).getEntries();
        MatchResult mr = mapMatching.doWork(inputGPXEntries);
        System.out.println(inputPath);
        System.out.println("\tmatches:\t" + mr.getEdgeMatches().size());
        System.out.println("\tgpx length:\t" + mr.getGpxEntriesLength() + " vs " + mr.getMatchLength());
        System.out.println("\tgpx time:\t" + mr.getGpxEntriesMillis() / 1000f + " vs " + mr.getMatchMillis() / 1000f);

        String outFile = inputPath + ".res.gpx";
        System.out.println("\texport results to:" + outFile);

        //TODO find a way without an instruction list
        InstructionList il = null;
        new GPXFile(mr, il).doExport(outFile);
        hopper.close();
}
    
    private void web() {
        try {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    binder().requireExplicitBindings();

                    install(Modules.override(new GraphHopperModule(commandline_args)).with(new AbstractModule() {

                        @Override
                        protected void configure() {
                            bind(RailwayHopper.class).toInstance(hopper);
                        }

                        @Singleton
                        @Provides
                        protected GraphHopper createGraphHopper(CmdArgs args) {
                            return hopper;
                        }
                    }));

                    install(new GraphHopperServletModule(commandline_args));

                    bind(GuiceFilter.class);
                }
            });
            new GHServer(commandline_args).start(injector);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        // do not close as server is running in its own thread and needs open graphhopper
        // hopper.close();
    }
}
