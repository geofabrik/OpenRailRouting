package de.geofabrik.railway_routing.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.MultiException;
import com.graphhopper.PathWrapper;
import com.graphhopper.http.WebHelper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.gpx.Gpx;
import com.graphhopper.matching.gpx.Trk;
import com.graphhopper.matching.gpx.Trkseg;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
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
import com.graphhopper.util.shapes.GHPoint;
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

    private List<GPXEntry> importGpx(InputStream inputStream) {
        XmlMapper xmlMapper = new XmlMapper();
        Gpx gpx;
        try {
            gpx = xmlMapper.readValue(inputStream, Gpx.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse GPX data stream.");
        }
        if (gpx.trk == null) {
            throw new IllegalArgumentException("No tracks found in GPX document. Are you using waypoints or routes instead?");
        }
        if (gpx.trk.size() > 1) {
            throw new IllegalArgumentException("GPX documents with multiple tracks not supported yet.");
        }
        return gpx.trk.get(0).getEntries();
    }

    public List<GPXEntry> parseInput(InputStream inputStream, String contentType, char separator, char quoteChar)
            throws SAXException, IOException, ParserConfigurationException {
        if (contentType.equals(MediaType.APPLICATION_XML) || contentType.equals("application/gpx+xml")) {
            return importGpx(inputStream);
        }
        if (contentType.equals("text/csv")) {
            return readCSV(inputStream, 50, separator, quoteChar);
        }
        if (!contentType.equals(null)) {
            throw new java.lang.RuntimeException("Unsupported input MIME type");
        }
        // guess input type
        InputStreamReader isr = new InputStreamReader(inputStream);
        char[] beginning = new char[5];
        try {
            isr.read(beginning, 0, 5);
        } catch (IOException e) {
            throw new java.lang.RuntimeException(e.toString());
        }
        String declaration = String.valueOf(beginning);
        if (declaration.equals("<?xml")) {
            return importGpx(inputStream);
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

    // copied from com.graphhopper.resources.RouteResource
    static void initHints(HintsMap m, MultivaluedMap<String, String> parameterMap) {
        for (Map.Entry<String, List<String>> e : parameterMap.entrySet()) {
            if (e.getValue().size() == 1) {
                m.put(e.getKey(), e.getValue().get(0));
            } else {
                // Do nothing.
                // TODO: this is dangerous: I can only silently swallow
                // the forbidden multiparameter. If I comment-in the line below,
                // I get an exception, because "point" regularly occurs
                // multiple times.
                // I think either unknown parameters (hints) should be allowed
                // to be multiparameters, too, or we shouldn't use them for
                // known parameters either, _or_ known parameters
                // must be filtered before they come to this code point,
                // _or_ we stop passing unknown parameters alltogether..
                //
                // throw new WebApplicationException(String.format("This query parameter (hint) is not allowed to occur multiple times: %s", e.getKey()));
            }
        }
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
            @QueryParam("gpx.route") @DefaultValue("true") boolean withRoute,
            @QueryParam("gpx.track") @DefaultValue("true") boolean withTrack,
            @QueryParam("traversal_keys") @DefaultValue("false") boolean enableTraversalKeys,
            @QueryParam(MAX_VISITED_NODES) @DefaultValue("3000") int maxVisitedNodes,
            @QueryParam("gps_accuracy") @DefaultValue("40") double gpsAccuracy,
            @QueryParam("fill_gaps") @DefaultValue("false") boolean fillGaps) throws Exception {
        StopWatch sw = new StopWatch().start();
        boolean writeGPX = "gpx".equalsIgnoreCase(outType);
        instructions = writeGPX || instructions;
        String infoStr = httpReq.getRemoteAddr() + " " + httpReq.getLocale() + " " + httpReq.getHeader("User-Agent");
        String logStr = httpReq.getQueryString() + " " + infoStr;
        FlagEncoder encoder;
        try {
            encoder = encodingManager.getEncoder(vehicleStr);
        } catch (IllegalArgumentException err) {
            throw new IllegalArgumentException("Vehicle not supported: " + vehicleStr);
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
            PathMerger pathMerger = new PathMerger().
                    setEnableInstructions(instructions).
                    setPathDetailsBuilders(hopper.getPathDetailsBuilderFactory(), pathDetails);
            PathWrapper pathWrapper = new PathWrapper();
            Translation tr = trMap.getWithFallBack(Helper.getLocale(localeStr));
            List<MatchResult> matchResultsList = new ArrayList<MatchResult>(2);
            List<Path> mergedPaths = new ArrayList<Path>(3);
            do {
                // fill gap with normal routing if matching in the last iteration of this loop ended at a gap
                if (mapMatching.matchingAttempted() &&
                        mapMatching.getSucessfullyMatchedPoints() < inputGPXEntries.size() - 1) {
                    List<GHPoint> points = new ArrayList<GHPoint>();
                    points.add((GHPoint) inputGPXEntries.get(mapMatching.getSucessfullyMatchedPoints()));
                    points.add((GHPoint) inputGPXEntries.get(mapMatching.getSucessfullyMatchedPoints() + 1));
                    GHRequest request =  new GHRequest(points);
                    initHints(request.getHints(), uriInfo.getQueryParameters());
                    request.setVehicle(encodingManager.getEncoder(vehicleStr).toString()).
                        setLocale(localeStr).
                        setPathDetails(pathDetails).
                        getHints().
                        put(CALC_POINTS, calcPoints).
                        put(INSTRUCTIONS, instructions);
                    GHResponse response = new GHResponse();
                    List<Path> paths = hopper.calcPaths(request, response);
                    if (response.hasErrors()) {
                        logger.error("  Failed to calc a path to fill a gap in the map matching: " + response.getErrors().toString());
                        throw new MultiException(response.getErrors());
                    } else {
                        mergedPaths.add(paths.get(0));
                    }
                }
                MatchResult mr = mapMatching.doWork(inputGPXEntries, !fillGaps);
                mergedPaths.add(mr.getMergedPath());
                matchResultsList.add(mr);
            } while (mapMatching.hasPointsToBeMatched());

            // GraphHopper thinks an empty path is an invalid path, and further that an invalid path is still a path but
            // marked with a non-empty list of Exception objects. I disagree, so I clear it.
            pathWrapper.getErrors().clear();
            GHResponse rsp = new GHResponse();
            pathMerger.doWork(pathWrapper, mergedPaths, tr);
            rsp.add(pathWrapper);

            took = sw.stop().getSeconds();
            logger.info(logStr + ", took:" + took);
            long time = System.currentTimeMillis();
            if (!inputGPXEntries.isEmpty()) {
                time = inputGPXEntries.get(0).getTime();
            }
            if (writeGPX) {
                return Response.ok(rsp.getBest().getInstructions().createGPX("", time, false, withRoute, withTrack, false, Constants.VERSION), "application/gpx+xml").
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

                double matchLength = 0, gpxEntriesLength = 0;
                int matchMillis = 0, gpxEntriesMillis = 0;
                List<Integer> traversalKeylist = new ArrayList<>();
                for (MatchResult mr : matchResultsList) {
                    matchLength += mr.getMatchLength();
                    matchMillis += mr.getMatchMillis();
                    gpxEntriesLength += mr.getGpxEntriesLength();
                    gpxEntriesMillis += mr.getGpxEntriesMillis();
                    if (enableTraversalKeys) {
                        for (EdgeMatch em : mr.getEdgeMatches()) {
                            EdgeIteratorState edge = em.getEdgeState();
                            // encode edges as traversal keys which includes orientation, decode simply by multiplying with 0.5
                            traversalKeylist.add(GHUtility.createEdgeKey(edge.getBaseNode(), edge.getAdjNode(), edge.getEdge(), false));
                        }
                    }
                }
                Map<String, Object> matchStatistics = new HashMap<>();
                matchStatistics.put("distance", matchLength);
                matchStatistics.put("time", matchMillis);
                matchStatistics.put("original_distance", gpxEntriesLength);
                matchStatistics.put("original_time", gpxEntriesMillis);
                map.putPOJO("map_matching", matchStatistics);

                if (enableTraversalKeys) {
                    map.putPOJO("traversal_keys", traversalKeylist);
                }
                return Response.ok(map).
                        header("X-GH-Took", "" + Math.round(took * 1000)).
                        build();
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (java.lang.RuntimeException | SAXException | IOException | ParserConfigurationException err) {
            logger.error(logStr + ", took:" + took + ", error:" + err);
            throw err;
        }
    }
}
