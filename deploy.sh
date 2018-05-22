#!/usr/bin/env bash

sbt updateImpactSubmit coverageReport coverageAggregate codacyCoverage
bash <(curl -s https://codecov.io/bash)
git config --global user.email "pathikritbhowmick@msn.com"
git config --global user.name "travis-ci"
git config --global push.default simple
sbt ghpagesPushSite +publish
