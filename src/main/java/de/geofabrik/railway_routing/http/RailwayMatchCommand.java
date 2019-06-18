package de.geofabrik.railway_routing.http;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.graphhopper.PathWrapper;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.gpx.Gpx;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Constants;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;

import de.geofabrik.railway_routing.RailwayHopper;
import de.geofabrik.railway_routing.util.PatternMatching;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class RailwayMatchCommand extends ConfiguredCommand<RailwayRoutingServerConfiguration> {
    public RailwayMatchCommand() {
        super("match", "matches GPX tracks to the railway network");
    }
    
    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-V", "--vehicle")
                .dest("vehicle")
                .type(String.class)
                .required(true)
                .help("profile to use");
        subparser.addArgument("-a", "--gps-accuracy")
                .dest("gps-accuracy")
                .type(Double.class)
                .required(false)
                .setDefault(40.0)
                .help("GPS measurement accuracy");
        subparser.addArgument("--max-nodes")
                .dest("max_nodes_to_visit")
                .type(Integer.class)
                .required(false)
                .setDefault(10000)
                .help("maximum number of nodes to visit between two trackpoints");
        subparser.addArgument("--gpx-location")
                .dest("gpx_location")
                .type(String.class)
                .required(true)
                .help("GPX input file(s). The argument may be a glob pattern.");
    }

    @Override
    protected void run(Bootstrap<RailwayRoutingServerConfiguration> bootstrap, Namespace namespace, RailwayRoutingServerConfiguration configuration) throws Exception {
        configuration.getGraphHopperConfiguration().merge(CmdArgs.readFromSystemProperties());
        CmdArgs commandline_args = configuration.getGraphHopperConfiguration();
        RailwayHopper hopper = new RailwayHopper(configuration.getGraphHopperConfiguration(),
                configuration.getFlagEncoderConfigurations());
        hopper.setGraphHopperLocation(commandline_args.get("graph.location", "./graph-cache"));
//        graphHopper.start();
//        graphHopper.stop();
        

        final Logger logger = LogManager.getLogger(RailwayMatchCommand.class);
        logger.info("Loading graph from cache at {}", hopper.getGraphHopperLocation());
        hopper.load(hopper.getGraphHopperLocation());
        List<FlagEncoder> flagEncoders = hopper.getEncodingManager().fetchEdgeEncoders();
        FlagEncoder selectedEncoder = null;
        String profile = namespace.get("vehicle");
        for (FlagEncoder encoder : flagEncoders) {
            if (encoder.toString().equals(profile)) {
                selectedEncoder = encoder;
            }
        }
        if (selectedEncoder == null) {
            throw new IllegalArgumentException("No valid encoding manager selected. Please use the 'vehicle' parameter.");
        }
        double gpsAccuracy = namespace.getDouble("gps-accuracy");

        FastestWeighting fastestWeighting = new FastestWeighting(selectedEncoder);
        Weighting turnWeighting = hopper.createTurnWeighting(hopper.getGraphHopperStorage(),
                fastestWeighting, hopper.getTraversalMode());
        AlgorithmOptions opts = AlgorithmOptions.start()
                .traversalMode(hopper.getTraversalMode())
                .maxVisitedNodes(namespace.getInt("max_nodes_to_visit"))
                .weighting(turnWeighting)
                .hints(new HintsMap().put("vehicle", profile))
                .build();

        MapMatching mapMatching = new MapMatching(hopper, opts);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);

        String inputPath = namespace.getString("gpx_location");
        if (inputPath.equals("")) {
            throw new IllegalArgumentException("No input file was given. Please use the option gpx.location=*.");
        }
        int lastSeparator = PatternMatching.patternSplitDirFile(inputPath);
        String localeStr = namespace.getString("instructions");
        if (localeStr == null) {
            localeStr = "";
        }
        final boolean withRoute = !localeStr.isEmpty();
        Translation tr = new TranslationMap().doImport().getWithFallBack(Helper.getLocale(localeStr));
        LinkedList<Path> files = PatternMatching.getFileList(inputPath, lastSeparator);
        XmlMapper xmlMapper = new XmlMapper();

        for (Path f : files) {
            try {
                logger.info("Matching GPX track {} on the graph.", f.toString());
                Gpx gpx = xmlMapper.readValue(f.toFile(), Gpx.class);
                List<GPXEntry> inputGPXEntries = gpx.trk.get(0).getEntries();
                MatchResult mr = mapMatching.doWork(inputGPXEntries, false);
                logger.debug("\tmatches: {}", mr.getEdgeMatches().size());
                logger.debug("\tgpx length: {}", mr.getGpxEntriesLength(), mr.getMatchLength());
                logger.debug("\tgpx time: {} vs {}", mr.getGpxEntriesMillis() / 1000f, mr.getMatchMillis() / 1000f);

                String outFile = f.toString() + ".res.gpx";
                System.out.println("\texport results to:" + outFile);

                PathWrapper pathWrapper = new PathWrapper();
                new PathMerger().doWork(pathWrapper, Collections.singletonList(mr.getMergedPath()), tr);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
                    long time = System.currentTimeMillis();
                    if (!inputGPXEntries.isEmpty()) {
                        time = inputGPXEntries.get(0).getTime();
                    }
                    if (pathWrapper.hasErrors()) {
                        logger.error("Failed to match {} to the graph.", f);
                        System.exit(1);
                    }
                    writer.append(pathWrapper.getInstructions().createGPX(gpx.trk.get(0).name != null ? gpx.trk.get(0).name : "", time, hopper.hasElevation(), withRoute, true, false, Constants.VERSION));
                }
            } catch (IOException e) {
                logger.error("Received IOException while reading GPX file {} from input stream: {}",
                        f.toString(), e.toString());
            }
        }
        hopper.close();
    }
}
