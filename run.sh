#!/bin/bash

MAIN_CLASS="cn.edu.tsinghua.ee.fi.akka.remote_test.App"
ARGS="odl1.nopqzip.com Sender 30"

if [ -n $1 ]; then
  [ $1 = "sender" ] && ARGS="odl1.nopqzip.com Sender 30"
  [ $1 = "receiver" ] && ARGS="odl2.nopqzip.com Receiver"
fi

echo "Running $MAIN_CLASS"
mvn exec:java -Dexec.mainClass=${MAIN_CLASS} -Dexec.args=${ARGS}

