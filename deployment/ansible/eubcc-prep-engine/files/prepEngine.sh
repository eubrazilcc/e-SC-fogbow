#!/bin/bash

# Global variables
ENGINE_XML=/usr/local/eSC-engine/etc/engine.xml
TIMEOUT=3

# An operation to set the server address and jms port in engine.xml
config_engine()
{
    SRV_URL=$1
    JMS_PORT=$2
    
    # The assumption is that the URL is correct
    SRV_ADDR=$(echo $SRV_URL | cut -d '/' -f 3 - | cut -d '@' -f 2 - | cut -d ':' -f 1)
    echo "Extracted server address is: $SRV_ADDR"
    
    if [ -z $SRV_ADDR ] ; then
        echo "Invalid server address"
        exit 3
    fi

    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Engine']/Parameter[@Name='ServerBaseURL']/@Value" -v $SRV_URL $ENGINE_XML

    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Performance']/Parameter[@Name='JMSServerHost']/@Value" -v $SRV_ADDR $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='JMS']/Parameter[@Name='JMSServer']/@Value" -v $SRV_ADDR $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Provenance']/Parameter[@Name='JMSServerHost']/@Value" -v $SRV_ADDR $ENGINE_XML

    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Performance']/Parameter[@Name='JMSServerPort']/@Value" -v $JMS_PORT $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='JMS']/Parameter[@Name='JMSPort']/@Value" -v $JMS_PORT $ENGINE_XML
    xmlstarlet ed -L -u "/PipelineDocument/Data/Parameter[@Name='Properties']/Parameter[@Name='SystemProperties']/Parameter[@Name='Provenance']/Parameter[@Name='JMSServerPort']/@Value" -v $JMS_PORT $ENGINE_XML
}

# An operation to check the current availability of the server
check_server()
{
    SRV_URL=$1
    JMS_PORT=$2
    
    # The assumption is that the URL is correct
    SRV_ADDR_PORT=$(echo $SRV_URL | cut -d '/' -f 3 - | cut -d '@' -f 2 -)
    SRV_ADDR=$(echo $SRV_ADDR_PORT | cut -d ':' -f 1 -s)
    if [ -z $SRV_ADDR ] ; then
        SRV_ADDR=$(echo $SRV_ADDR_PORT | cut -d ':' -f 1)
        PROTO=$(echo $SRV_URL | cut -d ':' -f 1 -s)
        if [ "$PROTO" == "http" ] ; then
            SRV_PORT=80
        elif [ "$PROTO" == "https" ] ; then
            SRV_PORT=443
        else
            echo "Unsupported protocol $PROTO in URL '$SRV_URL'"
            exit 2
        fi
    else
        SRV_PORT=$(echo $SRV_ADDR_PORT | cut -d ':' -f 2 -s)
    fi

    echo "Checking availability at address: '$SRV_ADDR' for ports: $SRV_PORT, $JMS_PORT"
    if [ -z $SRV_ADDR ] ; then
        echo "Invalid server address"
        #exit 3
    fi

    check_port $SRV_ADDR $SRV_PORT 3
    check_port $SRV_ADDR $JMS_PORT 3
}

check_port()
{
    HOST=$1
    PORT=$2
    TIMEOUT=$3
    
    if [ -z $TIMEOUT ] ; then
        TIMEOUT=5
    fi

    echo "Checking availability of ${HOST}:${PORT}, timeout = ${TIMEOUT}..."

    nc -z -w $TIMEOUT $HOST $PORT
    if [ $? -eq 0 ] ; then
        echo "[OK] ${HOST}:${PORT} is open."
        return 0
    else
        echo "[FAILED] ${HOST}:${PORT} is unreachable at the moment."
        return 1
    fi
}

if [ $# -lt 2 ] ; then
    echo "Missing some arguments: $0 <serverBaseURL> <jmsPort>"
    exit 1
fi

SERVER_BASE_URL=$1
JMS_PORT=$2

check_server $SERVER_BASE_URL $JMS_PORT

echo "Using server URL: $SERVER_BASE_URL and JMS port: $JMS_PORT to configure e-SC engine at: $ENGINE_XML"
service eSC-engine stop

config_engine $SERVER_BASE_URL $JMS_PORT

update-rc.d eSC-engine enable
service eSC-engine start
