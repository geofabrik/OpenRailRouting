# OpenStreetMap based routing on railway tracks

This is a prototype of a routing engine for railways based on a forked version of the
[GraphHopper](https://github.com/graphhopper/graphhopper) routing engine and OpenStreetMap data.

Following features are currently supported:

* simple routing requests
* map matching
* taking turn angles into account (to avoid U-turns on points)
* disabling turns on railway crossings (OSM tag `railway=railway_crossing`)
* using only tracks which have a compatible gauge
* not using tracks without catenary if it is an electrical locomotive/electrical multiple unit
* distinction between third rails and catenary
* support of tracks with multiple gauges
* support of tracks with switchable voltage and frequency

Lacking features:

* avoiding the usage of the opposite track (on double-track lines)
* taking the low acceleration and the long breaking distances of trains into account
* a lot of features which would need data which is not in OSM (incline, structure gauges)


## Building

This project uses Maven 3.x for building. The forked GraphHopper is provided as a Git submodule.

```sh
git submodule init
git submodule update
bash build.sh
```

JUnit 4.x is used for unit tests.

## Configuration

Configuration happens via a YAML file which is given as a positional parameter
when starting the routing engine.  Most parts of the configuration are
identical to GraphHopper. However, one part is different – the flag encoders
(aka routing profiles):

```yaml
# This section sets the properties of the flag encoders like maximum speed,
# supported power systems, supported gauges and the factor to use to encode the speed
# values.
#
# Properties of flagEncoderProperties:
#   name: name of the flag encoder – used by the API
#   electrified: list of compatible values of the OSM tag electrified=* separated by semicola – as a string
#   voltages: list of compatible values of the OSM tag voltage=* separated by semicola – as a string
#   frequencies: list of compatible values of the OSM tag frequency=* separated by semicola – as a string
#   gauges: list of compatible values of the OSM tag gauge=* separated by semicola – as a string
#   maxspeed: maximum speed of this flag encoder in kph
#   speedFactor: divisor for divide speed values by to encode them in the flags of an edge of the graph
#
# If electrified, voltages, frequencies or gauges is missing, the profile accepts any value. This is recommended for
# an all-gauge diesel engine.
flagEncoderProperties:
  - name: tgv_all
    electrified: contact_line
    voltages: 15000;25000;1500;3000
    frequencies: 16.7;16.67;50;0
    gauges: 1435
    maxspeed: 319
    speedFactor: 11
  - name: non_tgv
    gauges: 1435
    maxspeed: 120
    speedFactor: 5

graphhopper:
  # Use the 'profiles' property. graph.flag_encoders is not supported!
  profiles: tgv_all,non_tgv

  # Any other values can be found in the GraphHopper documentation and are explained in the exemplary configuration in this repository
```


## Running

To run the routing engine, execute

```sh
java -Xmx2500m -Xms50m -Dgraphhopper.prepare.ch.weightings=no \
  -Dgraphhopper.datareader.file=$OSMFILE -Dgraphhopper.profiles=freight_diesel \
  -jar target/railway_routing-0.0.1-SNAPSHOT-jar-with-dependencies.jar $ACTION $CONFIG_FILE $OPTARG
```

The tool currently supports three different actions (`$ACTION` above):

* `import` to import the graph (graph will be stored at the subdirectory `graph-cache/`)
* `serve` to listen to HTTP requests for the API and the web interface on the port specified in a YAML configuration file (see
  `config.yml` as an example). If no data has been imported, an import of the routing graph will happen first.
* `match` do map matching. This command needs additional arguments called `$OPTARG` above.

All commands have some arguments to be handed over as Java system variables using the `-Dkey=value`
option of the JVM. These arguments can also be given using the YAML file.

### Import

Required settings to be given either as Java system properties (`-Dgraphhopper.datareader.file=PATH` or in the YAML file):

* `graphhopper.datareader.file=$PATH`: path to OSM file
* `graphhopper.graph.location=./graph-cache`: directory where the graph should be written to
  (default: `./graph-cache`)
* `graphhopper.profiles=freight_electric_15kvac_25kvac,freight_diesel,tgv_15kvac25kvac1.5kvdc,tgv_25kvac1.5kvdc3kvdc`:
  flag encoders to be used. Following encoders are available but you can define more on your own:
  * `freight_electric_15kvac_25kvac`
  * `freight_diesel`
  * `tgv_15kvac25kvac1.5kvdc`
  * `tgv_25kvac1.5kvdc3kvdc`
  * `freight_electric_25kvac1.5kvdc3kvdc`

### Web

Required settings to be given either as Java system properties (`-Dgraphhopper.datareader.file=PATH` or in the YAML file):

* `graphhopper.datareader.file=$PATH`: path to OSM file
* `graphhopper.graph.location=./graph-cache`: directory where the graph should be read from
  (default: `./graph-cache`)
* `server.applicationConnector.port=$PORT`: port to be opened by Jetty
* `graphhopper.profiles=<flag_encoders>`: this must be the same as used for the import

### Match

Required settings to be given either as Java system properties (`-Dgraphhopper.datareader.file=PATH` or in the YAML file):

* `graphhopper.datareader.file=$PATH`: path to OSM file
* `graphhopper.graph.location=./graph-cache`: directory where the graph should be read from
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

See [third_party.md](third_party.md) for a list of all third-party code in this repository

## Thank you

Development of this project has been supported by [Geofabrik](https://www.geofabrik.de) and [SNCF](https://www.sncf.com/fr).
