package de.geofabrik.railway_routing;


import static com.graphhopper.util.Helper.createFormatter;
import static com.graphhopper.util.Helper.getMemInfo;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GraphHopper;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.reader.OSMRailwayReader;

public class RailwayHopper extends GraphHopper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RailwayHopper(List<FlagEncoderConfiguration> encoderConfigs) {
        super();
        setVehicleTagParserFactory(new RailFlagEncoderFactory(encoderConfigs));
        setVehicleEncodedValuesFactory(new RailEncodedValuesFactory(encoderConfigs));
    }

    @Override
    protected void importOSM() {
        if (getOSMFile() == null)
            throw new IllegalStateException("Couldn't load from existing folder: " + getGraphHopperLocation()
                    + " but also cannot use file for DataReader as it wasn't specified!");

        logger.info("start creating graph from " + getOSMFile());
        OSMRailwayReader reader = new OSMRailwayReader(getBaseGraph(), getEncodingManager(), getOSMParsers(), getReaderConfig());
        reader.setFile(_getOSMFile());
        reader.setElevationProvider(getElevationProvider());
        logger.info("using " + getBaseGraph().toString() + ", memory:" + getMemInfo());
        createBaseGraphAndProperties();
        try {
            reader.readGraph();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read file " + getOSMFile(), ex);
        }
        DateFormat f = createFormatter();
        getProperties().put("datareader.import.date", f.format(new Date()));
        if (reader.getDataDate() != null) {
            getProperties().put("datareader.data.date", f.format(reader.getDataDate()));
        }
        writeEncodingManagerToProperties();
    }

}
