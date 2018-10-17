#! /bin/bash


echo ">"
echo ">"
echo "> Setting the application in demo mode"
echo "> url=http://localhost:8080/login"
echo "> user=demo@talend.com, password=P@ssw0rd"
echo ">"
echo ">"

export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.environment=development"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.oauth2.keys.generate=true"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.oauth2.keys.log=true"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.persistence.database.driver=org.h2.Driver"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.persistence.database.url=jdbc:h2:file:./data/marketplace"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.persistence.database.username=SA"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dtalend.marketplace.persistence.database.password=SA"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dopenjpa.Log=slf4j"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dopenjpa.jdbc.SynchronizeMappings=buildSchema"
export MEECROWAVE_OPTS="$MEECROWAVE_OPTS -Dgeronimo.jwt-auth.org.eclipse.microprofile.authentication.JWT.clockSkew=100000"
