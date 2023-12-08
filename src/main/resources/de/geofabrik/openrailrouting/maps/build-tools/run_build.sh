#! /usr/bin/env bash

set -euo pipefail

SCRIPTDIR=$(dirname $0)
cd "$SCRIPTDIR"

echo "Starting container"
docker run \
    --attach=STDIN --attach=STDOUT --attach=STDERR \
    --interactive=true --tty=true \
    --user=$(id -u ${USER}):$(id -g ${USER}) \
    --mount=type=bind,source=$(pwd)/..,destination=/nodejs-build-dir \
    nodejs-build-env:1 || true

CONTAINER_ID=$(docker container ls -a -q -f ancestor=nodejs-build-env:1)

echo "Removing all NodeJS build containers: $CONTAINER_ID"
docker container rm $CONTAINER_ID
