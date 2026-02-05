function displayError(message) {
    var errorBox = document.getElementById('errors');
    if (message.length > 0) {
        errorBox.innerText += '\n';
    }
    errorBox.innerText += message;
    errorBox.style.display = 'block';
}

// load available flag encoders
function loadInfos() {
    var xhr = new XMLHttpRequest();
    var url = '/info?type=json';
    xhr.open('GET', url, true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.responseType = "json";
    xhr.onreadystatechange = function() {
        if(xhr.readyState == XMLHttpRequest.DONE && xhr.status == 200) {
            if (!xhr.response.hasOwnProperty('profiles')) {
                displayError('The response by the routing API does not list any supported routing profile.');
            }
            var supported_vehicles = xhr.response.profiles.map((v) => v.name);
            var vehicleSelect = document.getElementById('vehicle');
            supported_vehicles.forEach(function(elem) {
                var optionElement = document.createElement("option");
                optionElement.text = elem;
                vehicleSelect.add(optionElement);
            });
	    vehicleSelect.value = supported_vehicles[0];
        }
    }
    xhr.onerror = function() {
        displayError('Failed to fetch basic info about the routing API.');
    };
    xhr.send();
}

loadInfos();

// INFO: Following variables have be modified when this page is deployed on a server
var start_latitude = 48.86; // initial latitude of the center of the map
var start_longitude = 2.39; // initial longitude of the center of the map
var start_zoom = 10; // initial zoom level
var max_zoom = 19; // maximum zoom level the tile server offers
// End of the variables which might be modified if deployed on a server


// define both base maps
var osmOrgTilesLayer = L.tileLayer("//{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, imagery CC-BY-SA'
});

// define overlay
var gpxLayer = null;
var gpxDataStr = '';

// set current layer, by default the layer with the local tiles
var currentBaseLayer = osmOrgTilesLayer;

// get layer and location from anchor part of the URL if there is any
var anchor = location.hash.substr(1);
if (anchor != "") {
    elements = anchor.split("/");
    if (elements.length == 4) {
        if (elements[0] == 'osm.org') {
            currentBaseLayer = osmOrgTilesLayer;
        }
        start_zoom = elements[1];
        start_latitude = elements[2];
        start_longitude = elements[3];
    }
}

var mymap = L.map('mapid', {
    center: [start_latitude, start_longitude],
    zoom: start_zoom,
    layers: [currentBaseLayer]
});

// layer control
var baseLayers = {"osm.org": osmOrgTilesLayer};
var overlays = {};
var layerControl = L.control.layers(baseLayers, overlays);
layerControl.addTo(mymap);

// functions executed if the layer is changed or the map moved
function update_url(newBaseLayerName) {
    if (newBaseLayerName == '') {
        if (currentBaseLayer == osmOrgTilesLayer) {
            newBaseLayerName = 'osm.org';
        }
    }
    var origin = location.origin;
    var pathname = location.pathname;
    var newurl = origin + pathname + "#" + newBaseLayerName + '/' + mymap.getZoom() + "/" + mymap.getCenter().lat.toFixed(6) + "/" +mymap.getCenter().lng.toFixed(6);
    history.replaceState('data to be passed', document.title, newurl);
}

// event is fired if the base layer of the map changes
mymap.on('baselayerchange', function(e) {
    if (e.name == 'local') {
        currentBaseLayer = localTilesLayer;
    } else if (e.name == 'osm.org') {
        currentBaseLayer = osmOrgTilesLayer;
    } else {
        console.log("Could not find layer " + e.name);
    }
    update_url(e.name);
});

// change URL in address bar if the map is moved
 mymap.on('move', function(e) {
     update_url('');
 });

function showLoading(turnOn) {
    var loading = document.getElementById('loading');
    if (turnOn) {
        loading.style.display = 'block';
    	document.getElementById('save').disabled = true;
    } else {
        loading.style.display = 'none';
    }
}

function clearResults() {
    if (gpxLayer != null) {
        gpxLayer.clearLayers();
    }
    document.getElementById('results').style.display = 'none';
    document.getElementById('errors').style.display = 'none';
    document.getElementById('errors').innerText = '';
}

function saveGPX(e) {
    var blob = new Blob([gpxDataStr], {type: "application/gpx+xml;charset=utf-8"});
    saveAs(blob, "result.gpx");
}

function displayGPX(gpxData) {
    gpxLayer = new L.GPX(gpxData, {async: true, marker_options: {startIcon: false, endIcon: false}})
        .on('loaded', function(e) {
            mymap.fitBounds(e.target.getBounds(), {padding: [20, 20]});
                showLoading(false);
                document.getElementById('results').style.display = 'block';
                document.getElementById('trackLength').innerText = Math.round(gpxLayer.get_distance()) / 1000 + " km";
            }
        )
        .addTo(mymap);
    document.getElementById('save').disabled = false;
}


function retrieveResult(gpxData) {
    showLoading(true);
    var xhr = new XMLHttpRequest();
    var url = "/match?";
    url += "&gps_accuracy=" + document.getElementById('gpsAccuracy').value;
    url += "&max_visited_nodes=" + document.getElementById('maxNodes').value;
    url += "&profile=" + document.getElementById('vehicle').value;
    url += "&fill_gaps=" + document.getElementById('fillGaps').checked;
    url += "&type=gpx&gpx.route=false";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-type", "application/gpx+xml");
    xhr.responseType = "text";
    xhr.onreadystatechange = function() {
        if (xhr.readyState == XMLHttpRequest.DONE && xhr.status == 200) {
            gpxDataStr = xhr.response;
            displayGPX(xhr.response);
        } else if (xhr.readyState == XMLHttpRequest.DONE) {
            var parser = new DOMParser();
            var doc = parser.parseFromString(xhr.response, "text/xml");
            displayError(doc.getElementsByTagName('message')[0].childNodes[0].nodeValue);
            showLoading(false);
        }
    }
    xhr.send(gpxData);
}

function getMatched(e) {
    clearResults();
    var f = document.getElementById('files').files[0];
    var reader = new FileReader();
    var gpxContent = "";
    reader.onload = function(ev) {
        gpxContent = reader.result;
        retrieveResult(gpxContent);
    }
    reader.readAsText(f);
}

document.getElementById('upload').addEventListener('click', getMatched);
document.getElementById('save').addEventListener('click', saveGPX);

