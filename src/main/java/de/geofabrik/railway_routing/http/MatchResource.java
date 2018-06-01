package de.geofabrik.railway_routing.http;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.GPXFile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.StopWatch;

@Path("match")
public class MatchResource {
    private static final Logger logger = LoggerFactory.getLogger(MatchResource.class);

    private final GraphHopper hopper;
    private final EncodingManager encodingManager;
    
    @Inject
    public MatchResource(GraphHopper graphHopper, EncodingManager encodingManager/*, @Named("hasElevation") Boolean hasElevation*/) {
        this.hopper = graphHopper;
        this.encodingManager = encodingManager;
    }

    @POST
    @Consumes({MediaType.APPLICATION_XML, "application/gpx+xml"})
    @Produces({MediaType.APPLICATION_XML, "application/gpx+xml"})
    public Response doPost(
            InputStream inputStream,
            @Context HttpServletRequest httpReq,
            @Context UriInfo uriInfo,
            @QueryParam("vehicle") @DefaultValue("car") String vehicleStr,
            @QueryParam("gpsAccuracy") @DefaultValue("40") double gpsAccuracy,
            @QueryParam("maxNodes") @DefaultValue("10000") int maxNodes) {
        StopWatch sw = new StopWatch().start();
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
                .maxVisitedNodes(maxNodes)
                .weighting(turnWeighting)
                .hints(new HintsMap().put("vehicle", vehicleStr))
                .build();
        MapMatching mapMatching = new MapMatching(hopper, opts);
        mapMatching.setMeasurementErrorSigma(gpsAccuracy);
        float took = 0;
        try {
            List<GPXEntry> inputGPXEntries = new GPXFile().doImport(inputStream, 50).getEntries();
            MatchResult mr = mapMatching.doWork(inputGPXEntries);
            InstructionList il = null;
            GPXFile gpxFile = new GPXFile(mr, il);
            String response = gpxFile.createString();
            took = sw.stop().getSeconds();
            logger.info(logStr + ", took:" + took);
            return Response.ok(response, "application/gpx+xml").
                    header("Content-Disposition", "attachment;filename=" + "GraphHopper.gpx").build();
        } catch (java.lang.RuntimeException err) {
            took = sw.stop().getSeconds();
            logger.error(logStr + ", took:" + took + ", error:" + err);
            throw new WebApplicationException(err);
        }
    }
}
