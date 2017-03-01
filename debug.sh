#!/bin/bash

DBG_PORT=5005
ARG_MISC="-classpath %classpath -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DBG_PORT}"
MAIN_CLASS="cn.edu.tsinghua.ee.fi.akka.remote_test.App"
ARGS="odl1.nopqzip.com Sender 30"

if [ -n $1 ]; then
  [ $1 = "sender" ] && ARGS="odl1.nopqzip.com Sender 30"
  [ $1 = "receiver" ] && ARGS="odl2.nopqzip.com Receiver"
fi

echo "Debuging $MAIN_CLASS"

mvn exec:exec -Dexec.executable="java" -Dexec.args="${ARG_MISC} ${MAIN_CLASS} ${ARGS}"