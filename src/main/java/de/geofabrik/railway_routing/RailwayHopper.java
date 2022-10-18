package de.geofabrik.railway_routing;


import static com.graphhopper.util.Helper.createFormatter;
import static com.graphhopper.util.Helper.getMemInfo;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;
import com.graphhopper.GraphHopper;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.reader.OSMRailwayReader;

public class RailwayHopper extends GraphHopper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** set of internal node IDs which are tagged with railway=railway_crossing in OSM */
    private IntSet crossingsSet = null;

    public RailwayHopper(List<FlagEncoderConfiguration> encoderConfigs) {
        super();
        setFlagEncoderFactory(new RailFlagEncoderFactory(encoderConfigs));
    }

    @Override
    protected void importOSM() {
        if (getOSMFile() == null)
            throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
                    + " but also cannot use file for DataReader as it wasn't specified!");

        logger.info("start creating graph from " + getOSMFile());
        OSMRailwayReader reader = new OSMRailwayReader(getGraphHopperStorage(), getReaderConfig());
        reader.setFile(_getOSMFile());
        reader.setElevationProvider(getElevationProvider());
        crossingsSet = new IntScatterSet();
        CrossingsSetHook hook = new CrossingsSetHook(crossingsSet);
        reader.setCrossingsHandler(hook);
        reader.register(new SwitchTurnCostTask(getGraphHopperStorage(), getEncodingManager(), crossingsSet));
        logger.info("using " + getGraphHopperStorage().toString() + ", memory:" + getMemInfo());
        try {
            reader.readGraph();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read file " + getOSMFile(), ex);
        }
        DateFormat f = createFormatter();
        getGraphHopperStorage().getProperties().put("datareader.import.date", f.format(new Date()));
        if (reader.getDataDate() != null)
            getGraphHopperStorage().getProperties().put("datareader.data.date", f.format(reader.getDataDate()));
    }

}
