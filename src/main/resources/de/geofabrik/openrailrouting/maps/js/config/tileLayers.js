var ghenv = require("./options.js").options;
var tfAddition = '';
if (ghenv.thunderforest.api_key)
    tfAddition = '?apikey=' + ghenv.thunderforest.api_key;

var mapilionAddition = '';

var osAPIKey = '';
if (ghenv.omniscale.api_key)
    osAPIKey = ghenv.omniscale.api_key;

var osmAttr = '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors';
var swissAttr = osmAttr + ', ASTER GDEM, EarthEnv-DEM90, CDEM contains information under OGL Canadamap <a href="https://creativecommons.org/licenses/by/4.0/">cc-by</a> <a href="https://github.com/xyztobixyz/OSM-Swiss-Style">xyztobixyz</a>';
var osmCHAttr = osmAttr + ', ASTER GDEM, EarthEnv-DEM90, CDEM contains information under OGL Canada';
var osmFRAttr = osmAttr + ', Openstreetmap France';
var openrailwaymapAttr = ' <a href="https://www.openrailwaymap.org/">OpenRailwayMap</a>, <a href="https://creativecommons.org/licenses/by-sa/2.0/">cc-by-sa</a>';

// Automatically enable high-DPI tiles if provider and browser support it.
var retinaTiles = L.Browser.retina;

var osm = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: osmAttr
});

var osmde = L.tileLayer('https://tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png', {
    attribution: osmAttr
});

var swiss = L.tileLayer('https://tile.osm.ch/osm-swiss-style/{z}/{x}/{y}.png', {
    attribution: osmAttr
});

var osmch = L.tileLayer('https://tile.osm.ch/switzerland/{z}/{x}/{y}.png', {
    attribution: osmCHAttr
});

var osmfr = L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
    attribution: osmFRAttr
});

var openrailwaymapInfra = L.tileLayer('https://tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png', {
    attribution: openrailwaymapAttr
});

var openrailwaymapMaxspeed = L.tileLayer('https://tiles.openrailwaymap.org/maxspeed/{z}/{x}/{y}.png', {
    attribution: openrailwaymapAttr
});

var openrailwaymapElectrification = L.tileLayer('https://tiles.openrailwaymap.org/electrification/{z}/{x}/{y}.png', {
    attribution: openrailwaymapAttr
});

var availableTileLayers = {
    "OSM Carto": osm,
    "OSM Carto DE": osmde ,
    "Swiss": swiss,
    "OSM CH": osmch,
    "OSM FR": osmfr,
};

var overlays;
var extraOverlays = {
    "OpenRailwayMap Infrastructure": openrailwaymapInfra,
    "OpenRailwayMap Maxspeed": openrailwaymapMaxspeed,
    "OpenRailwayMap Electrification": openrailwaymapElectrification,
};
module.exports.enableVectorTiles = function () {
    var omniscaleGray = L.tileLayer('https://maps.omniscale.net/v2/' +osAPIKey + '/style.grayscale/layers.world,buildings,landusages,labels/{z}/{x}/{y}.png?' + (retinaTiles ? '&hq=true' : ''), {
        layers: 'osm',
        attribution: osmAttr + ', &copy; <a href="https://maps.omniscale.com/">Omniscale</a>'
    });
    availableTileLayers["Omniscale Dev"] = omniscaleGray;

    require('leaflet.vectorgrid');
    var vtLayer = L.vectorGrid.protobuf("/mvt/{z}/{x}/{y}.mvt?details=max_speed&details=road_class&details=road_environment", {
      rendererFactory: L.canvas.tile,
      maxZoom: 20,
      minZoom: 10,
      interactive: true,
      vectorTileLayerStyles: {
        'roads': function(properties, zoom) {
            // weight == line width
            var color, opacity = 1, weight = 1, rc = properties.road_class;
            // if(properties.speed < 30) console.log(properties)
            if(rc == "motorway") {
                color = '#dd504b'; // red
                weight = 3;
            } else if(rc == "primary" || rc == "trunk") {
                color = '#e2a012'; // orange
                weight = 2;
            } else if(rc == "secondary") {
                weight = 2;
                color = '#f7c913'; // yellow
            } else {
                color = "#aaa5a7"; // grey
            }
            if(zoom > 16)
               weight += 3;
            else if(zoom > 15)
               weight += 2;
            else if(zoom > 13)
               weight += 1;

            return {
                weight: weight,
                color: color,
                opacity: opacity
            }
        },
      },
    })
    .on('click', function(e) {
    })
    .on('mouseover', function(e) {
        console.log(e.layer.properties);
        // remove route info
        $("#info").children("div").remove();
        // remove last vector tile info
        $("#info").children("ul").remove();

        var list = "";
        $.each(e.layer.properties, function (key, value) {
            list += "<li>" + key + ": " + value + "</li>";
        });
        $("#info").append("<ul>"+list+"</ul>");
        $("#info").show();
    }).on('mouseout', function (e) {
//        $("#info").html("");
    });
    overlays = { "Local MVT": vtLayer };
}

module.exports.activeLayerName = "OSM Carto";
module.exports.defaultLayer = osm;

module.exports.getAvailableTileLayers = function () {
    return availableTileLayers;
};

module.exports.getOverlays = function () {
    return Object.assign({}, overlays, extraOverlays);
};

module.exports.selectLayer = function (layerName) {
    var defaultLayer = availableTileLayers[layerName];
    if (!defaultLayer)
        defaultLayer = module.exports.defaultLayer;

    return defaultLayer;
};
