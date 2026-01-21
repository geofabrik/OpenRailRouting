package de.geofabrik.railway_routing.http;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.graphhopper.ResponsePath;
import com.graphhopper.gpx.GpxConversions;
import com.graphhopper.jackson.Gpx;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;

import de.geofabrik.railway_routing.RailwayHopper;
import de.geofabrik.railway_routing.util.PatternMatching;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RailwayMatchCommand extends ConfiguredCommand<RailwayRoutingServerConfiguration> {
    public RailwayMatchCommand() {
        super("match", "matches GPX tracks to the railway network");
    }
    
    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-P", "--profile")
                .dest("profile")
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
        configuration.updateFromSystemProperties();
        RailwayHopper hopper = new RailwayHopper();
        hopper.setGraphHopperLocation(configuration.getGraphHopperConfiguration().getString("graph.location", "./graph-cache"));
        hopper.init(configuration.getGraphHopperConfiguration());
        

        final Logger logger = LogManager.getLogger(RailwayMatchCommand.class);
        logger.info("Loading graph from cache at {}", hopper.getGraphHopperLocation());
        hopper.load();
        String profile = namespace.get("profile");
        double gpsAccuracy = namespace.getDouble("gps-accuracy");

        PMap hints = new PMap()
                .putObject("profile", profile)
                .putObject(Parameters.Routing.MAX_VISITED_NODES, namespace.getInt("max_nodes_to_visit"));

        MapMatching mapMatching = MapMatching.fromGraphHopper(hopper, hints);
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
                if (gpx.trk == null) {
                    throw new IllegalArgumentException("No tracks found in GPX document. Are you using waypoints or routes instead?");
                }
                if (gpx.trk.size() > 1) {
                    throw new IllegalArgumentException("GPX documents with multiple tracks not supported yet.");
                }
                List<Observation> measurements = GpxConversions.getEntries(gpx.trk.get(0));
                MatchResult mr = mapMatching.match(measurements/*, 0*/);
                logger.debug("\tmatches: {}", mr.getEdgeMatches().size());
                logger.debug("\tgpx length: {}", mr.getGpxEntriesLength(), mr.getMatchLength());

                String outFile = f.toString() + ".res.gpx";
                System.out.println("\texport results to:" + outFile);

                ResponsePath responsePath = new PathMerger(mr.getGraph(), mr.getWeighting()).
                        doWork(PointList.EMPTY, Collections.singletonList(mr.getMergedPath()), hopper.getEncodingManager(), tr);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
                    // The GPX output is not exactly the same as upstream GraphHopper.
                    // Upstream GraphHopper writes the timestamp of the first trackpoint of the input
                    // file to the metadata section of the output file. We don't do this because this
                    // is special to GPX. The same applies tothe name field of the metadata section.
                    //TODO If elevation support is added, remove hardcoded false here.
                    long time = System.currentTimeMillis();
                    if (responsePath.hasErrors()) {
                        logger.error("Failed to match {} to the graph.", f);
                        System.exit(1);
                    }
                    String trackName = gpx.trk.get(0).name != null ? gpx.trk.get(0).name : "";
                    writer.append(GpxConversions.createGPX(responsePath.getInstructions(),
                            trackName, time, hopper.hasElevation(), withRoute, true, false,
                            Constants.VERSION, tr));
                }
            } catch (IOException e) {
                logger.error("Received IOException while reading GPX file {} from input stream: {}",
                        f.toString(), e.toString());
            }
        }
        hopper.close();
    }
}
