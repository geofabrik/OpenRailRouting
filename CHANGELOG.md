### 1.1 [6 Feb 2026]

This is a maintenance release with a couple of bugfixes, increased test coverage and a new GraphHopper version.

* Upgrade to GraphHopper 11.0 with osm-reader-callbacks patches
* Upgrade from JDK 8 to JDK 17
* Times penalities for u-turn are no longer taken into account for calculating travel times due to a change in GraphHopper (they continue to influence which route will be chosen). You have to set `profiles[].turn_costs.enable_uturn_times` to true for all profiles. Otherwise the calculated travel times will be shorter than usual.
* Add more tests to ensure that routing results in order to detect if routing results change due to GraphHopper upgrades.
* Remove unnecessary dependencies
* Reduce priority of tracks in opposite direction (against `railway:preferred_direction=*`) from `0.7` to `0.5`.
* Bugfix: Increase encoder for `rail_average_speed` from 5 to 7 bits in order to store speeds larger than 155 km/h. This will reduce travel times returned by all API endpoints if high speed trains (>= 160 km/h) are involved.
* Bugfix: Allow encoded value `max_speed` to store speed limits up to 510 km/h (instead of 256 km/h upstream).
* Bugfix: Adapt map matching frontend to current GraphHopper API.

### 1.0 [9 July 2024]

* first release
* GraphHopper 9.1 with osm-reader-callbacks patches
