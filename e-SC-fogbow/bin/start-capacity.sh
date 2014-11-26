#!/bin/bash
DIRNAME=`dirname $0`
LOG4J=log4j.properties
cd $DIRNAME/..
if [ -f $LOG4J ]; then
CONF_LOG=-Dlog4j.configuration=file:$LOG4J
else
CONF_LOG=
fi
java $CONF_LOG -cp target/fogbow-escience-central-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.fogbowcloud.Main capacityplanner.conf > /dev/null &
