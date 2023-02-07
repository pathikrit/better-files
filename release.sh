#!/usr/bin/env bash

set -euxo pipefail

# See https://github.com/olafurpg/sbt-ci-release#git
# Usage ./release.sh v3.9.1
# Note this force pushes using -f below - useful to retry deploys
VERSION=$1
git tag -f -a ${VERSION} -m "${VERSION}"
git push origin ${VERSION} -f
