#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

SCALA_VER=$(grep 'scalaVersion' ${DIR}/../project/Common.scala | grep -o '"[0-9][^"]*"' | head -n 1 | tr -d '"');
SDK_VER=$(grep -o '"[0-9][^"]*"' ${DIR}/../project/Common.scala | head -n 1 | tr -d '"')


bash ${DIR}/allocate_test_cloud.sh "Scala ${SCALA_VER} SDK ${SDK_VER}"
