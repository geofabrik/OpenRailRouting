package de.geofabrik.railway_routing.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.http.WebHelper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXFile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.Constants;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
import com.opencsv.bean.CsvToBeanBuilder;

import de.geofabrik.railway_routing.InputCSVEntry;

import static com.graphhopper.util.Parameters.Routing.*;

@javax.ws.rs.Path("match")
public class MatchResource {
    private static final Logger logger = LoggerFactory.getLogger(MatchResource.class);

    private final GraphHopper hopper;
    private final EncodingManager encodingManager;
    private final TranslationMap trMap;

    @Inject
    public MatchResource(GraphHopper graphHopper, EncodingManager encodingManager,
            TranslationMap trMap) {
        this.hopper = graphHopper;
        this.encodingManager = encodingManager;
        this.trMap = trMap;
    }

    private List<GPXEntry> readCSV(InputStream inputStream, double defaultSpeed, char separator, char quoteChar) {
        try {
            List<InputCSVEntry> inputEntries = new CsvToBeanBuilder<InputCSVEntry>(new InputStreamReader(inputStream))
                    .withType(InputCSVEntry.class)
                    .withSeparator(separator)
                    .withQuoteChar(quoteChar)
                    .build()
                    .parse();
            InputCSVEntry last = null;
            ArrayList<GPXEntry> result = new ArrayList<GPXEntry>(inputEntries.size());
            DistanceCalc distCalc = Helper.DIST_PLANE;
            long millis = 0;
            for (InputCSVEntry entry: inputEntries) {
                if (last != null) {
                    millis += Math.round(distCalc.calcDist(last.getLatitude(), last.getLongitude(),
                            entry.getLatitude(), entry.getLongitude()) * 3600 / defaultSpeed);
                    last = entry;
                }
                result.add(entry.toGPXEntry(millis));
            }
            return result;
        } catch (NumberFormatException e) {
            throw new java.lang.RuntimeException(e.toString());
        }
    }

    public List<GPXEntry> parseInput(InputStream inputStream, String contentType, char separator, char quoteChar) {
        if (contentType.equals(MediaType.APPLICATION_XML) || contentType.equals("application/gpx+xml")) {
            return new GPXFile().doImport(inputStream, 50).getEntries();
        }
        if (contentType.equals("text/csv")) {
            return readCSV(inputStream, 50, separator, quoteChar);
        }
        if (!contentType.equals(null)) {
            throw new java.lang.RuntimeException("Unsupported input MIME type");
        }
        // guess input type
        System.out.println("guessing file type");
        InputStreamReader isr = new InputStreamReader(inputStream);
        char[] beginning = new char[5];
        try {
            isr.read(beginning, 0, 5);
        } catch (IOException e) {
            throw new java.lang.RuntimeException(e.toString());
        }
        String declaration = String.valueOf(beginning);
        if (declaration.equals("<?xml")) {
            return new GPXFile().doImport(inputStream, 50).getEntries();
        }
        return readCSV(inputStream, 50, separator, quoteChar);
    }

    private String getCSVOutput(PathWrapper path, char separator) {
        PointList points = path.getPoints();
        StringBuilder str = new StringBuilder(points.getSize() * 2 * 15);
        str.append("longitude").append(separator).append("latitude\n");
        for (int i = 0; i < points.getSize(); ++i) {
            str.append(Double.toString(points.getLon(i)))
                .append(separator)
                .append(Double.toString(points.getLat(i)))
                .append('\n');
        }
        return str.toString();
    }

    @POST
    // @Consumes({MediaType.APPLICATION_XML, "application/gpx+xml", "text/csv"})
    // We don't declare @Consumes types here because otherwise request without a Content-type header would fail.
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/gpx+xml"})
    public Response doPost(
            InputStream inputStream,
            @Context HttpServletRequest httpReq,
            @Context UriInfo uriInfo,
            @QueryParam("type") @DefaultValue("json") String outType,
            @QueryParam("csv_input.separator") @DefaultValue(";") char csvInputSeparator,
            @QueryParam("csv_input.quoteChar") @DefaultValue("\"") char quoteChar,
            @QueryParam("csv_output.separator") @DefaultValue(";") char csvOutputSeparator,
            @QueryParam(INSTRUCTIONS) @DefaultValue("true") boolean instructions,
            @QueryParam(CALC_POINTS) @DefaultValue("true") boolean calcPoints,
            @QueryParam("points_encoded") @DefaultValue("true") boolean pointsEncoded,
            @QueryParam("vehicle") @DefaultValue("car") String vehicleStr,
            @QueryParam("locale") @DefaultValue("en") String localeStr,
            @QueryParam(Parameters.DETAILS.PATH_DETAILS) List<String> pathDetails,
            @QueryParam("gpx.trackname") @DefaultValue("GraphHopper Track") String trackName,
            @QueryParam("gpx.route") @DefaultValue("true") boolean withRoute /* default to false for the route part in next API version, see #437 */,
            @QueryParam("gpx.track") @DefaultValue("true") boolean withTrack,
            @QueryParam("gpx.waypoints") @DefaultValue("false") boolean withWayPoints,
            @QueryParam("gpx.millis") String timeString,
            @QueryParam("traversal_keys") @DefaultValue("false") boolean enableTraversalKeys,
            @QueryParam(MAX_VISITED_NODES) @DefaultValue("3000") int maxVisitedNodes,
            @QueryParam("gps_accuracy") @DefaultValue("40") double gpsAccuracy) {
        StopWatch sw = new StopWatch().start();
        boolean writeGPX = "gpx".equalsIgnoreCase(outType);
        instructions = writeGPX || instructions;
        String infoStr = httpReq.getRemoteAddr() + " " + httpReq.getLocale() + " " + httpReq.getHeader("User-Agent");
        String logStr = httpReq.getQueryString() + " " + infoStr;
        FlagEncoder encoder;
        try {
            encoder = encodingManager.getEncoder(vehicleStr);
        } catch (IllegalArgumentException err) {
            throw new WebApplicationException("Vehicle not supported: " + vehicleStr);
        }
        FastestWeighting fastestWeighting = new FastestWeighting(encoder);
        Weighting turnWeighting = hopper.createTurnWeighting(hopper.getGraphHopperStorage(),
                fastestWeighting, hopper.getTraversalMode());
        AlgorithmOptions opts = AlgorithmOptions.start()
                .traversalMode(hopper.getTraversalMode())
                .maxVisitedNodes(maxVisitedNodes)
                .weighting(turnWeighting)
                .hints(new HintsMap().put("vehicle", vehicleStr))
                .build();
        MapMatching mapMatching = new MapMatching(hopper, opts);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);
        float took = 0;
        try {
            List<GPXEntry> inputGPXEntries = parseInput(inputStream, httpReq.getHeader("Content-type"), csvInputSeparator, quoteChar);
            MatchResult mr = mapMatching.doWork(inputGPXEntries);
            com.graphhopper.routing.Path path = mapMatching.calcPath(mr);
            Translation tr = trMap.getWithFallBack(Helper.getLocale(localeStr));
            PathMerger pathMerger = new PathMerger().
                    setEnableInstructions(instructions).
                    setPathDetailsBuilders(hopper.getPathDetailsBuilderFactory(), pathDetails);
            PathWrapper pathWrapper = new PathWrapper();
            pathMerger.doWork(pathWrapper, Collections.singletonList(path), tr);

            // GraphHopper thinks an empty path is an invalid path, and further that an invalid path is still a path but
            // marked with a non-empty list of Exception objects. I disagree, so I clear it.
            pathWrapper.getErrors().clear();
            GHResponse rsp = new GHResponse();
            rsp.add(pathWrapper);

            took = sw.stop().getSeconds();
            logger.info(logStr + ", took:" + took);
            long time = timeString != null ? Long.parseLong(timeString) : System.currentTimeMillis();
            if (writeGPX) {
                return Response.ok(rsp.getBest().getInstructions().createGPX(trackName, time, false, withRoute, withTrack, withWayPoints, Constants.VERSION), "application/gpx+xml").
                        header("Content-Disposition", "attachment;filename=" + "GraphHopper.gpx").
                        header("X-GH-Took", "" + Math.round(took * 1000)).
                        build();
            } else if ("csv".equalsIgnoreCase(outType)) {
                return Response.ok(getCSVOutput(rsp.getBest(), csvOutputSeparator), "text/csv")
                        .header("Content-Disposition", "attachment;filename=" + "GraphHopper.csv")
                        .header("X-GH-Took", "" + Math.round(took * 1000))
                        .build();
            } else {
                ObjectNode map = WebHelper.jsonObject(rsp, instructions, calcPoints, false, pointsEncoded, took);

                Map<String, Object> matchStatistics = new HashMap<>();
                matchStatistics.put("distance", mr.getMatchLength());
                matchStatistics.put("time", mr.getMatchMillis());
                matchStatistics.put("original_distance", mr.getGpxEntriesLength());
                matchStatistics.put("original_time", mr.getGpxEntriesMillis());
                map.putPOJO("map_matching", matchStatistics);

                if (enableTraversalKeys) {
                    List<Integer> traversalKeylist = new ArrayList<>();
                    for (EdgeMatch em : mr.getEdgeMatches()) {
                        EdgeIteratorState edge = em.getEdgeState();
                        // encode edges as traversal keys which includes orientation, decode simply by multiplying with 0.5
                        traversalKeylist.add(GHUtility.createEdgeKey(edge.getBaseNode(), edge.getAdjNode(), edge.getEdge(), false));
                    }
                    map.putPOJO("traversal_keys", traversalKeylist);
                }
                return Response.ok(map).
                        header("X-GH-Took", "" + Math.round(took * 1000)).
                        build();
            }
        } catch (java.lang.RuntimeException err) {
            logger.error(logStr + ", took:" + took + ", error:" + err);
            return WebHelper.errorResponse(err, writeGPX);
        }
    }
}
