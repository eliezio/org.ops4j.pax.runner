#!/bin/sh
#
# Script to run Pax Runner, which starts OSGi frameworks with applications.
#
#

java $JAVA_OPTS -jar ~/.m2/repository/org/ops4j/pax/runner/pax-runner/0.6.0-SNAPSHOT/pax-runner-0.6.0-SNAPSHOT.jar "$@"