# GraphHopper Routing Web Interface Customized for OpenRailRouting

This repository contains the GraphHopper routing web interface customized for OpenRailRouting.

Code was copied from GraphHopper and is licensed under Apache License version 2.

## Building

### Building on your local machine

```sh
npm install
npm run bundle
```

### Building in a Docker container with bind mount

```sh
build-tools/build_docker.sh
build-tools/run_build.sh
```

This will create a Docker container mounting this directory. It runs NodeJS.
After installing the dependencies, you will get a interactive Bash shell.
Type `npm run bundle` (or any other command of your choice) and `exit` afterwards.
