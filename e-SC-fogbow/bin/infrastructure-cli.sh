#!/bin/bash
SCRIPT_PATH=`readlink -f $0`
DIRNAME=`dirname $SCRIPT_PATH`
cd $DIRNAME/..
java -cp target/fogbow-escience-central-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.fogbowcloud.infrastructure.Main $@