#!/bin/bash

BIN="java -jar algo_supermarket_health_check-assembly-1.0-SNAPSHOT.jar"
PREV_TRADING_DAY="2016-03-31"

while [ 1 ]
do
    clear
    $BIN dev.properties full $PREV_TRADING_DAY nodetail onetime
    $BIN uat.properties full $PREV_TRADING_DAY nodetail onetime
    $BIN pdn.properties full $PREV_TRADING_DAY nodetail onetime
    for i in $(seq 1 10)
    do
        echo -n "."
        sleep 1
    done
done
