#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
$GRADLE test
cd $ROOT/khakis
$ROOT/khakis/bin/build.sh
$ROOT/khakis/bin/tag.sh
$ROOT/khakis/bin/push.sh
cd -
$GRADLE pushDocker
