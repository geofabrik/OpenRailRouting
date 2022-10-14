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
import com.graphhopper.reader.osm.OSMReader;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;

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
        OSMReader reader = new OSMReader(getGraphHopperStorage()).setFile(_getOSMFile()).
                setElevationProvider(getElevationProvider()).
                setWorkerThreads(getWorkerThreads()).
                setWayPointMaxDistance(getWayPointMaxDistance()).
                setWayPointElevationMaxDistance(getRouterConfig().getElevationWayPointMaxDistance()).
                setSmoothElevation(getSmoothElevation()).
                setLongEdgeSamplingDistance(getLongEdgeSamplingDistance());
        crossingsSet = new IntScatterSet();
        CrossingsSetHook hook = new CrossingsSetHook(reader, crossingsSet);
        reader.register(hook);
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
