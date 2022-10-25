#! /usr/bin/env bash
# Build OpenRailRouting with local Maven repository on file system.
# This script is useful if you fix bugs in the GraphHopper library.

set -euo pipefail

BASEDIR=$(pwd)

# Get GraphHopper version string
cd graphhopper
echo "Get GraphHopper version string"
GH_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd "$BASEDIR"

BASEDIR=$(pwd)
echo "Puring maven_repository"
MAVEN_REPO="$BASEDIR/maven_repository"
rm -rf "$MAVEN_REPO"/*
cd graphhopper
echo "Cleaning"
mvn clean
echo "Installing"
mvn --projects web,core,web-bundle,map-matching,web-api -P include-client-hc -am -DskipTests=true compile package install
echo "Deploying"
mvn deploy:deploy-file -Durl=file://"$MAVEN_REPO" -Dfile=core/target/graphhopper-core-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-core -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://"$MAVEN_REPO" -Dfile=web/target/graphhopper-web-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://"$MAVEN_REPO" -Dfile=web-bundle/target/graphhopper-web-bundle-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web-bundle -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://"$MAVEN_REPO" -Dfile=web-api/target/graphhopper-web-api-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-web-api -Dpackaging=jar -Dversion=$GH_VERSION
mvn deploy:deploy-file -Durl=file://"$MAVEN_REPO" -Dfile=map-matching/target/graphhopper-map-matching-$GH_VERSION.jar -DgroupId=com.graphhopper -DartifactId=graphhopper-map-matching -Dpackaging=jar -Dversion=$GH_VERSION
cd $BASEDIR
echo "Building OpenRailRouting"
MAVEN_OPTS=-Xss20m mvn clean compile install -U -f pom-local-repository.xml
