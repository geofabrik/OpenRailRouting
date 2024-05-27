# OpenStreetMap based routing on railway tracks

This is a prototype of a routing engine for railways based on a forked version of the
[GraphHopper](https://github.com/geofabrik/graphhopper/tree/osm-reader-callbacks) routing engine and OpenStreetMap data.

## Features

Following features are currently supported:

* simple routing requests
* map matching
* isochrones
* taking turn angles into account (to avoid U-turns on points)
* disabling turns on railway crossings (OSM tag `railway=railway_crossing`)
* support of tracks with multiple gauges
* support of tracks with switchable voltage and frequency
* support of different speeds in forward and backward direction

The configured vehicle profiles can:

* use only tracks which have a compatible gauge
* skip tracks without catenary if it is an electrical locomotive/electrical multiple unit
* take the type of electrification (third rails vs. catenary) into account
* avoid using tracks against theire preferred direction (useful for railway lines with multiple tracks)

Missing features:

* taking the low acceleration and the long breaking distances of trains into account
* a lot of features which would need data which is not in OSM (incline, structure gauges)
* support for barriers (e.g. gates)

## Web Frontend

This project includes a web frontend which is a fork of the original GraphHopper web frontend.

## Building

This project uses Maven (for the Java code) and NodeJS for the web frontend for building.
`npm` has to be installed.

```sh
git submodule init
git submodule update
mvn clean install
```

JUnit 5 is used for unit tests.

## Configuration

You can configure OpenRailRouting by editing its GraphHopper configuration (YAML file). Please refer to [config.yml](config.yml) for details.

### Profiles

Each route request has to specify a profile. The available profiles are determined by the configuration when the graph is imported.
Each profile consists of

* `name`: name of the profile
* `turn_costs`: turn costs
  * `vehicle_types`: vehicle types used for vehicle-specific turn restrictions. Available values: `train`, `light_rail`, `tram`, `subway`
  * `u_turn_costs`: time penality in seconds for reversing
* `custom_model_files`: list of files that define the profile. The files are searched in `src/main/resources/com/graphhopper/custom_profiles` and, if set, the path specified by `custom_models.directory`.

Please refer to the [GraphHopper documentation about custom models](https://github.com/graphhopper/graphhopper/blob/master/docs/core/custom-models.md) for a detailed explanation how custom models work. The following contains a few notes about differences between GraphHopper and OpenRailRouting.

In addition to the encoded values supported by GraphHopper, OpenRailRouting can encoded the following encoded values per edge:

* `voltage`: decimal (precision: 100 Volt), missing values are encoded as `0.0`
* `frequency`: decimal (precision: 2.5 Hz), missing values are encoded as `0.0`
* `electrified`: enum (`UNSET`, `NO`, `OTHER`, `CONTACT_LINE`, `RAIL`)
* `gauge`: integer, missing values are encoded as `0.0`
* `railway_class`: enum for the value of the OSM `railway=*` tag: `RAIL`, `LIGHT_RAIL`, `TRAM`, `SUBWAY`, `NARROW_GAUGE`, `FUNICULAR`
* `railway_service`: enum for the value of the OSM `service=*` tag: `NONE`, `SIDING`, `YARD`, `SPUR`, `CROSSOVER`
* `preferred_direction`: boolean value, true if the requested direction matches the preferred direction of operation of the track

In order to make use of these encoded values, you have to set them in the GraphHopper configuration using:

```yaml
graphhopper:
  graph.encoded_values: gauge,voltage,electrified,frequency,road_environment,max_speed,rail_access,rail_average_speed,railway_class,railway_service
```

If an edge has multiple gauges, voltages, freuqencies or types of electrification (e.g. multi-gauge tracks or tracks), the edge is duplicated during import. As author of a custom model, you don't need to take this into account.

OpenRailRouting provides a few basic profiles where you can combine and built upon:

* `all_tracks.json` accepts any track.
* `rail.json` routes on any `railway=rail`
* `tramtrain.json` routes on `railway=rail/light_rail/tram`
* `gauge_1435.json` limits accessible tracks to those with `gauge=1435` or missing gauge.
* `15kv-ac_750v-dc.json` limits accessible tracks to those with 15 kV 16.7 Hz AC, 750 V DC or missing information about power systems. Tracks with `electrified=no/rail` will be assumed as inaccessible.


## Running

To run the routing engine, execute

```sh
java -Xmx2500m -Xms50m \
  -Ddw.graphhopper.datareader.file=$OSMFILE \
  -jar target/railway_routing-0.0.1-SNAPSHOT.jar ACTION [ARGUMENTS] CONFIG_FILE [OPTARG]
```

The tool currently supports three different actions (`ACTION` above):

* `import` to import the graph (graph will be stored at the subdirectory `graph-cache/`)
* `serve` to listen to HTTP requests for the API and the web interface on the port specified in a YAML configuration file (see
  `config.yml` as an example). If no data has been imported, an import of the routing graph will happen first.
* `match` do map matching. This command needs additional arguments called `OPTARG` above.

### Import

Arguments:

* `--input`, `-i`: Path to input file (.osm.pbf format)
* `--output`, `-o`: Path to output directory where the graph should be written to

Required settings to be given either as Java system properties (`-Dgraphhopper.datareader.file=PATH` or in the YAML file):

* `graphhopper.datareader.file=$PATH`: path to OSM file
* `graphhopper.graph.location=./graph-cache`: directory where the graph should be written to
  (default: `./graph-cache`)

### Serve

Required settings to be given either as Java system properties (`-Ddw.KEY=VALUE` or in the configuration file):

* `dw.graphhopper.datareader.file=$PATH`: path to OSM file
* `dw.graphhopper.graph.location=./graph-cache`: directory where the graph should be read from
  (default: `./graph-cache`)
* `dw.server.applicationConnector.port=$PORT`: port to be opened by Jetty

### Match

Required settings to be given either as Java system properties (`-Ddw.KEY=VALUE` or in the configuration file):

* `dw.graphhopper.datareader.file=$PATH`: path to OSM file
* `dw.graphhopper.graph.location=./graph-cache`: directory where the graph should be read from
  (default: `./graph-cache`)

Following arguments have to be provided (not as Java system variables). You can retrieve this list
by calling
`java -jar target/railway_routing-0.0.1-SNAPSHOT-jar-with-dependencies.jar match config.yml`

* `--gpx-location=$PATTERN` is required. This can be either a single GPX file or a wildcard pattern
  like `/path/to/dir/mytracks/*.gpx`. The resulting routes will be written as GPX files to same
  directory but `.res.gpx` will be appended to their file names. Wildcard patterns need to be
  enclosed with quotes (`--gpx-location="path/to/files/*.gpx"`) to prevent your shell expanding
  the pattern.
* `-V VEHICLE`, `--vehicle=$VEHICLE`: routing profile to be used.

Optional command line arguments:

* `-a NUMBER`, `--gps-accuracy=NUMBER`: GPS accuracy in metres (default: 40)
* `--max_nodes=NUMBER`: maximum number of nodes to visit between two trackpoints (default: 10,000)

## License

See [LICENSE.txt](LICENSE.txt)

See [THIRD_PARTY.md](THIRD_PARTY.md) for a list of all third-party code in this repository

## Thank you

Development of this project has been supported by [Geofabrik](https://www.geofabrik.de) and [SNCF](https://www.sncf.com/fr).
