echo OFF
set LOG4J=%~dp0../log4j.properties
if exist {%LOG4J%} (
set CONF_LOG=-Dlog4j.configuration=file:%LOG4J%
) else (
set CONF_LOG=
)

START "/B" java %CONF_LOG% -cp %~dp0../target/fogbow-escience-central-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.fogbowcloud.Main %~dp0../capacityplanner.conf
