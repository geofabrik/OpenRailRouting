package de.geofabrik.railway_routing.http;

import javax.inject.Inject;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;
import de.geofabrik.railway_routing.RailwayHopper;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RailwayRoutingManaged implements Managed {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RailwayHopper graphHopper;
    
    @Inject
    public RailwayRoutingManaged(CmdArgs configuration) {
        graphHopper = (RailwayHopper) new RailwayHopper(configuration).forServer();
        graphHopper.setGraphHopperLocation(configuration.get("graph.location", "./graph-cache"));
//        graphHopper.init(configuration);
    }
    
    @Override
    public void start() throws Exception {
        graphHopper.importOrLoad();
        logger.info("loaded graph at:" + graphHopper.getGraphHopperLocation()
                + ", data_reader_file:" + graphHopper.getDataReaderFile()
                + ", flag_encoders:" + graphHopper.getEncodingManager()
                + ", " + graphHopper.getGraphHopperStorage().toDetailsString());
    }

    RailwayHopper getGraphHopper() {
        return graphHopper;
    }


    @Override
    public void stop() throws Exception {
        graphHopper.close();
    }

}
