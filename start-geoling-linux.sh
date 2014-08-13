#!/bin/bash

# set working directory
cd $(dirname "$0")

# options for Java virtual machine:
# set maximum heap size to allow GeoLing to use more memory:
# - default max heap size:
#   (empty value, i.e., no parameter, means that Java auto-selects a value
#    depending on your total physical memory installed, usually 1/4)
#MAX_HEAP=""
# - user-defined max heap size:
#   (note that at most ~1400m to ~1600m is possible on 32-bit machines)
MAX_HEAP="-Xmx1400m"

# start application
java $MAX_HEAP -splash:geoling-splash-screen.png -jar geoling.jar
