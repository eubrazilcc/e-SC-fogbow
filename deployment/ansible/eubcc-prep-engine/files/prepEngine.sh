#!/bin/bash

PORT_TO_CHECK=5445
TIMEOUT=5
ENGINE_XML=/usr/local/eSC-engine/etc/engine.xml

# An operation to set the server address in the engine.xml
set_server_addr()
{
    SRV_ADDR=$1

    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Engine']/Parameter[@Name='APIHost']/@Value" -v $SRV_ADDR $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Performance']/Parameter[@Name='JMSServerHost']/@Value" -v $SRV_ADDR $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='JMS']/Parameter[@Name='JMSServer']/@Value" -v $SRV_ADDR $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Provenance']/Parameter[@Name='JMSServerHost']/@Value" -v $SRV_ADDR $ENGINE_XML
}

if [ $# -lt 1 ] ; then
    echo "Missing argument. At least one IP address of the e-SC server is needed"
    exit 1
fi

for ARG in "$@" ; do
    echo "Checking whether host $ARG opened port $PORT_TO_CHECK (assuming it's JMS)..."
    nc -z -w $TIMEOUT $ARG $PORT_TO_CHECK
    if [ $? -eq 0 ] ; then
        echo "Ok. Port $PORT_TO_CHECK is open. Using $ARG as the e-SC server address."
        service eSC-engine stop
        set_server_addr $ARG
        update-rc.d eSC-engine enable
        service eSC-engine start
    else
        echo "Failed. $ARG:$PORT_TO_CHECK is unreachable."
    fi
done
