# OpenStreetMap based routing on railway tracks

This is a prototype of a routing engine for railways based on the
[GraphHopper](https://github.com/graphhopper/graphhopper) routing engine and OpenStreetMap data.

Following features are currently supported:

* simple routing requests
* map matching
* taking turn angles into account (to avoid U-turns on points)
* using only tracks which have a compatible gauge
* not using tracks without catenary if it is an electrical locomotive/electrical multiple unit
* distinction between third rails and catenary
* support of tracks with multiple gauges
* support of tracks with switchable voltage and frequency

Lacking features:

* disabling turns on railway crossings (OSM tag `railway=railway_crossing`)
* avoiding the usage of the opposite track (on double-track lines)
* taking the low acceleration and the long breaking distances of trains into account
* many profiles
* a lot of features which would need data which is not in OSM (incline, structure gauges)


## Building

This project uses Maven 3.x for building. You can invoke Maven on command line to build this project
by calling

```sh
mvn clean compile assembly:single
```

JUnit 4.x is used for unit tests.

## Running

To run the routing engine, execute

```sh
java -Xmx500m -Xms50m -Dlog4j2.configurationFile=logging.xml \
  -jar target/railway_routing-0.0.1-SNAPSHOT-jar-with-dependencies.jar action=$ACTION \
  datareader.file=$OSMFILE jetty.port=$JETTY_PORT jetty.resourcebase=graphhopper_webapp
```

The tool currently supports three different actions:

* `action=import` to import the graph (graph will be stored at the subdirectory `graph-cache/`)
* `action=web` to run the web interface on port `$JETTY_PORT`
* `action=match` do map matching. The additional argument . Following optional ar

### Import

Required arguments:

* `datareader.file=$PATH`: path to OSM file
* `graph.location=./graph-cache`: directory where the graph should be written to (default: `./graph-cache`)

### Web

Required arguments:

* `datareader.file=$PATH`: path to OSM file
* `graph.location=./graph-cache`: directory where the graph should be read from (default: `./graph-cache`)
* `jetty.port=$PORT`: port to be opened by Jetty
* `jetty.resourcebase=$PATH`: path to webserver document root (if you want to have a web interface
  instead just a plain API). This is usually the path to `web/src/main/webapp/` in the Graphhopper
  repository.

### Match

Required arguments:

* `datareader.file=$PATH`: path to OSM file
* `graph.location=./graph-cache`: directory where the graph should be read from (default: `./graph-cache`)
* `gpx.location=$PATTERN` is required. This can be either a single GPX file or a wildcard pattern
  like `/path/to/dir/mytracks/*.gpx`. The resulting routes will be written as GPX files to same
  directory but `.res.gpx` will be appended to their file names.

Optional arguments:

* `gps_accuracy=$NUMBER`: GPS accuracy in metres
* `max_nodes_to_visit=$NUMBER`
* `vehicle=$VEHICLE`: routing profile to be used.

## License

see [LICENSE.txt](LICENSE.txt)

## Thank you

Development of this project has been supported by [Geofabrik](https://www.geofabrik.de) and [SNCF](https://www.sncf.com/fr).
