#! /usr/bin/env bash

set -euo pipefail

# Get GraphHopper version string
cd graphhopper
GH_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd ..

# Get Map-Matching version string
cd map-matching
MM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd ..

BASEDIR=$(pwd)
rm -rf maven_repository/*
cd graphhopper
mvn clean
mvn --projects web,core,reader-osm,web-bundle,api -P include-client-hc -am -DskipTests=true compile package
# mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=core/target/graphhopper-core-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=forked-graphhopper-core -Dpackaging=jar -Dversion=$GH_VERSION
# mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=web/target/graphhopper-web-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=forked-graphhopper-web -Dpackaging=jar -Dversion=$GH_VERSION
# mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=reader-osm/target/graphhopper-reader-osm-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=forked-graphhopper-reader-osm -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=core/target/graphhopper-core-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-core -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=web/target/graphhopper-web-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=web-bundle/target/graphhopper-web-bundle-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web-bundle -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=web-api/target/graphhopper-web-api-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web-api -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=api/target/graphhopper-api-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-api -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=reader-osm/target/graphhopper-reader-osm-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-reader-osm -Dpackaging=jar -Dversion=$GH_VERSION
cd $BASEDIR/map-matching/
mvn --projects matching-web,matching-core -am -DskipTests=true clean compile package
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=matching-core/target/graphhopper-map-matching-core-$MM_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-map-matching-core -Dpackaging=jar -Dversion=$MM_VERSION
mvn deploy:deploy-file -Durl=file://$(pwd)/../maven_repository/ -Dfile=matching-web/target/graphhopper-map-matching-web-$MM_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-map-matching-web -Dpackaging=jar -Dversion=$MM_VERSION
cd $BASEDIR
MAVEN_OPTS=-Xss20m mvn compile assembly:single -U
