JAR=target/sncf_railway_routing-0.0.1-SNAPSHOT-jar-with-dependencies.jar

if [ "$JAVA" = "" ]; then
 JAVA=java
fi

if [ ! -f "$JAR" ]; then
  mvn -DskipTests=true install assembly:single
fi

# We need at least this much memory when running graphhopper. If there's out of
# memory errors, then up this from 25g to 50g

JAVA_OPTS="$JAVA_OPTS -Xmx500m -Xms50m"

#echo "$JAVA" $JAVA_OPTS -jar $JAR "$@" jetty.port=8981
exec "$JAVA" $JAVA_OPTS -jar $JAR "$@" jetty.port=8981
