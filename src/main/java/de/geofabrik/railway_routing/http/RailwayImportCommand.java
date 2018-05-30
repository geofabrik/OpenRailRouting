/*
 *  This file contains code of the GraphHopper project. See the
 *  THIRD_PARTY.md file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package de.geofabrik.railway_routing.http;

import com.graphhopper.util.CmdArgs;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class RailwayImportCommand extends ConfiguredCommand<RailwayRoutingServerConfiguration> {

    public RailwayImportCommand() {
        super("import", "creates the graphhopper files used for later (faster) starts");
    }

    @Override
    protected void run(Bootstrap<RailwayRoutingServerConfiguration> bootstrap, Namespace namespace,
            RailwayRoutingServerConfiguration configuration) throws Exception {
        configuration.getGraphHopperConfiguration().merge(CmdArgs.readFromSystemProperties());
        final RailwayRoutingManaged graphHopper = new RailwayRoutingManaged(configuration.getGraphHopperConfiguration(),
                configuration.getFlagEncoderConfigurations());
        graphHopper.start();
        graphHopper.stop();
    }
}
