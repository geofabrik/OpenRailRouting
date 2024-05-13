package de.geofabrik.railway_routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.util.PMap;

import de.geofabrik.railway_routing.http.FlagEncoderConfiguration;
import de.geofabrik.railway_routing.util.MultiValueChecker;

public class RailAccessParser extends AbstractAccessParser {

    public static final String DEFAULT_NAME = "rail";

    private HashSet<String> railwayValues;
    private ArrayList<String> electrifiedValues;
    private ArrayList<Integer> acceptedVoltages;
    private ArrayList<Double> acceptedFrequencies;
    private ArrayList<Integer> acceptedGauges;
    private boolean acceptYardSpur;
    private PMap properties = new PMap();

    public RailAccessParser(BooleanEncodedValue accessEnc, PMap properties) {
        super(accessEnc, TransportationMode.TRAIN);
        this.properties = properties;
        initFromProperties(properties);
    }

    public RailAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", DEFAULT_NAME))),
                properties
        );
    }

    protected void initFromProperties(PMap properties) {
        if (this.properties == null) {
            this.properties = properties;
        } else {
            this.properties.putAll(properties);
        }
        // railway values
        String railwayProps = properties.getString(FlagEncoderConfiguration.RAILWAY, "");
        if (!railwayProps.equals("")) {
            this.railwayValues = new HashSet<String>(Arrays.asList(railwayProps.split(";")));
        } else {
            this.railwayValues = new HashSet<String>();
            this.railwayValues.add("rail");
        }

        // electrified values
        String electrifiedProps = properties.getString(FlagEncoderConfiguration.ELECTRIFIED, "");
        if (!electrifiedProps.equals("")) {
            this.electrifiedValues = new ArrayList<String>(Arrays.asList(electrifiedProps.split(";")));
        } else {
            this.electrifiedValues = new ArrayList<String>();
        }

        this.acceptedVoltages = new ArrayList<Integer>();
        for (String v : properties.getString(FlagEncoderConfiguration.VOLATAGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedVoltages.add(Integer.parseInt(v));
            }
        }

        this.acceptedFrequencies = new ArrayList<Double>();
        for (String v : properties.getString(FlagEncoderConfiguration.FREQUENCIES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedFrequencies.add(Double.parseDouble(v));
            }
        }

        this.acceptedGauges = new ArrayList<Integer>();
        for (String v : properties.getString(FlagEncoderConfiguration.GAUGES, "").split(";")) {
            if (!v.equals("")) {
                this.acceptedGauges.add(Integer.parseInt(v));
            }
        }

        this.acceptYardSpur = properties.getBool(FlagEncoderConfiguration.ACCEPT_YARD_SPUR, true);
    }

    public boolean hasCompatibleElectricity(ReaderWay way) {
        if (electrifiedValues.isEmpty()) {
            return true;
        }
        String electrified = way.getTag("electrified", null);
        if (electrified == null) {
            return true;
        }
        if (electrifiedValues.contains(electrified) || electrified.equals("yes")) {
            String voltage = way.getTag("voltage");
            String frequency = way.getTag("frequency");
            if (MultiValueChecker.tagContainsInt(voltage, acceptedVoltages, true)
                    && MultiValueChecker.tagContainsDouble(frequency, acceptedFrequencies, true)) {
                return true;
            }
            // Grounded sections of the overhead line are treated as compatible.
            if (voltage == null || voltage.equals("0")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCompatibleGauge(ReaderWay way) {
        return MultiValueChecker.tagContainsInt(way.getTag("gauge"), acceptedGauges, true);
    }

    public boolean isYardSpur(ReaderWay way) {
        return way.hasTag("service", "yard") || way.hasTag("service", "spur");
    }

    public WayAccess getAccess(ReaderWay way) {
        if (!way.hasTag("railway", railwayValues) || !hasCompatibleElectricity(way) || !hasCompatibleGauge(way)) {
            return WayAccess.CAN_SKIP;
        }
        if (!acceptYardSpur && isYardSpur(way)) {
            return WayAccess.CAN_SKIP;
        }
        return WayAccess.WAY;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        WayAccess access = getAccess(way);
        if (access.canSkip()) {
            return;
        }
        accessEnc.setBool(false, edgeId, edgeIntAccess, true);
        accessEnc.setBool(true, edgeId, edgeIntAccess, true);
    }
}
