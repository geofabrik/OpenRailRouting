### 1.1 [not yet released]

* Upgrade to GraphHopper 11.0 with osm-reader-callbacks patches
* Reduce priority of tracks in opposite direction (against `railway:preferred_direction=*`) from `0.7` to `0.5`.
* Bugfix: Increase encoder for `rail_average_speed` from 5 to 7 bits in order to store speeds larger than 155 km/h. This will reduce travel times returned by all API endpoints if high speed trains (>= 160 km/h) are involved.

### 1.0 [9 July 2024]

* first release
* GraphHopper 9.1 with osm-reader-callbacks patches
