/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class RailwayImportCommand extends ConfiguredCommand<RailwayRoutingServerConfiguration> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RailwayImportCommand() {
        super("import", "creates the graphhopper files used for later (faster) starts");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-o", "--output")
                .dest("output")
                .type(String.class)
                .help("Path to graph directory");
        subparser.addArgument("-i", "--input")
                .dest("input")
                .type(String.class)
                .help("Path to input file (.osm.pbf)");
    }

    @Override
    protected void run(Bootstrap<RailwayRoutingServerConfiguration> bootstrap, Namespace namespace,
            RailwayRoutingServerConfiguration configuration) {
        String input = namespace.get("input");
        String output = namespace.get("output");
        configuration.updateFromSystemProperties();
        if (configuration.getGraphHopperConfiguration().getString("datareader.file", null) != null && input != null) {
            logger.warn("Input file path is specified by system property (-Ddw.graphhopper…) "+
                    "or configuration file. Overriding it by the value provided on command line.");
        }
        if (input != null) {
            configuration.getGraphHopperConfiguration().putObject("datareader.file", input);
        }
        if (configuration.getGraphHopperConfiguration().getString("graph.location", null) != null && output != null) {
            logger.warn("Output directory for graph is specified by system property (-Ddw.graphhopper…) "+
                    "or configuration file. Overriding it by the value provided on command line.");
        }
        if (output != null) {
            configuration.getGraphHopperConfiguration().putObject("graph.location", output);
        }
        final RailwayRoutingManaged graphHopper = new RailwayRoutingManaged(configuration.getGraphHopperConfiguration());
        graphHopper.getGraphHopper().importAndClose();
    }
}
