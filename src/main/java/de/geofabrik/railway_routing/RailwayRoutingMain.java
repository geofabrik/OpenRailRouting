package de.geofabrik.railway_routing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

import de.geofabrik.railway_routing.util.PatternMatching;

/**
 * Hello world!
 *
 */
public class RailwayRoutingMain {
    private RailwayHopper hopper;
    private CmdArgs commandline_args;
    private static final Logger logger = LogManager.getLogger(RailwayRoutingMain.class);
    
    public static void main( String[] args ) {
        new RailwayRoutingMain(CmdArgs.read(args));
    }
    
    private RailwayRoutingMain(CmdArgs args) {
        commandline_args = args;
        String action = commandline_args.get("action", "");
        hopper = new RailwayHopper(args);
        hopper.setGraphHopperLocation(commandline_args.get("graph.location", "./graph-cache"));
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
        logger.info("Loading graph from cache at {}", hopper.getGraphHopperLocation());
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
            throw new IllegalArgumentException("No encoding manager selected. Please use the 'profile' parameter.");
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
            throw new IllegalArgumentException("No input file was given. Please use the option gpx.location=*.");
        }
        int lastSeparator = PatternMatching.patternSplitDirFile(inputPath);
        LinkedList<Path> files = PatternMatching.getFileList(inputPath, lastSeparator);

        for (Path f : files) {
            InputStream inputStream;
            try {
                logger.info("Matching GPX track {} on the graph.", f.toString());
                inputStream = Files.newInputStream(f);
                List<GPXEntry> inputGPXEntries = new GPXFile().doImport(inputStream, 50).getEntries();
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
            } catch (IOException e) {
                logger.error("Received IOException while reading GPX file {} from input stream: {}",
                        f.toString(), e.toString());
            }
        }
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
