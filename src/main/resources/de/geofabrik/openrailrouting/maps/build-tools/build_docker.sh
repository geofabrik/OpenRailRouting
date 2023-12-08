#! /usr/bin/env bash

set -euo pipefail

SCRIPTDIR=$(dirname $0)

cd $SCRIPTDIR

echo "Building image nodejs-build-env:1"

docker build \
    --build-arg=HOST_UID=$(id -u) \
    --build-arg=HOST_GID=$(id -g) \
    --build-arg=HOST_USER=$USER \
    --tag=nodejs-build-env:1 \
    .

echo "SUCCESS: Built image nodejs-build-env:1"
