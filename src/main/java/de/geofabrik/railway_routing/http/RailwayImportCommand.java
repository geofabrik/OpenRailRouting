/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class RailwayImportCommand extends ConfiguredCommand<RailwayRoutingServerConfiguration> {

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
        configuration.getGraphHopperConfiguration().putObject("datareader.file", input);
        configuration.getGraphHopperConfiguration().putObject("graph.location", output);
        final RailwayRoutingManaged graphHopper = new RailwayRoutingManaged(configuration.getGraphHopperConfiguration(),
                configuration.getFlagEncoderConfigurations());
        graphHopper.getGraphHopper().importAndClose();
    }
}
