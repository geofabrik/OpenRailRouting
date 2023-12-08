#! /usr/bin/env bash

set -euo pipefail

cd /nodejs-build-dir
echo -n "Building with NodeJS version: "
node -v
echo -n "Building with NPM version: "
npm -v
npm install
/bin/bash
