#!/bin/bash
set -euo pipefail

SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/../khakis/bin/common.sh"

KAFKA_IMAGE=${KAFKA_IMAGE:-"eyeris/kafka"}

KAFKA_CPUSHARES=${KAFKA_CPUSHARES:-4}
KAFKA_MEMORY=${KAFKA_MEMORY:-768m}

# Container configuration
EYERIS_PLATFORM_DIRECT_PORTS=${EYERIS_PLATFORM_DIRECT_PORTS:-1}
EYERIS_PLATFORM_PORTS=${EYERIS_PLATFORM_PORTS:--P}
EYERIS_DEVELOPER_MODE=${EYERIS_DEVELOPER_MODE:-true}

KAFKA_CMD="--name=eyeris-kafka --cpu-shares=$KAFKA_CPUSHARES --memory=$KAFKA_MEMORY  -e KAFKA_REPLICATION=1 -e KAFKAOPS_REPLICATION=1 -e KAFKA_HSIZE=512 -e ADVERTISED_HSTN=kafka.eyeris -e ZOOKEEPER=zookeeper.eyeris:2181 -e ZOOKEEPEROPS=zookeeper.eyeris:2181 --link eyeris-zookeeper:zookeeper.eyeris $KAFKA_IMAGE"

if [ -n "${EYERIS_PLATFORM_DIRECT_PORTS}" ] && [ "${EYERIS_PLATFORM_PORTS}" == "-P" ]; then
#    ZOOKEEPER_CMD="-P -p 2181:2181 -p 2888:2888 -p 3888:3888 $ZOOKEEPER_CMD"
    KAFKA_CMD="-P -p 9092:9092 $KAFKA_CMD"
fi

docker_run -d $KAFKA_CMD

sleep 2
wait

