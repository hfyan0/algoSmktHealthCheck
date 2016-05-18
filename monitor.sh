#!/bin/bash

#BIN=./target/universal/stage/bin/algo_supermarket_health_check 
DMTH=/home/$(whoami)/Dropbox/utilities/dmath
BIN="java -jar algo_supermarket_health_check-assembly-1.0-SNAPSHOT.jar"
PREV_TRADING_DAY=xxx
PROP1=pdn.properties
PROP2=uat.properties
PROP3=dev.properties
#SCOPE=consistency
SCOPE=full
DETAIL=nodetail
# DETAIL=detail
MODEINSCT=continuous
MODE=onetime

echo "Please enter previous trading day [YYYY-MM-DD]"
echo "(just press enter for the last normal business day)"
read PREV_TRADING_DAY

if [[ -z $PREV_TRADING_DAY ]]
then
    WEEKDAY=$(date +'%u')
    if [[ $WEEKDAY == 1 ]]
    then
        PREV_TRADING_DAY=$($DMTH $(date +'%Y%m%d') -3)
    else
        PREV_TRADING_DAY=$($DMTH $(date +'%Y%m%d') -1)
    fi
    PREV_TRADING_DAY=$(echo $PREV_TRADING_DAY | cut -c1-4)"-"$(echo $PREV_TRADING_DAY | cut -c5-6)"-"$(echo $PREV_TRADING_DAY | cut -c7-8)
fi

while [ 1 ]
do
    clear
    echo "Previous trading day: $PREV_TRADING_DAY"

    $BIN $PROP1 $SCOPE $PREV_TRADING_DAY $DETAIL $DETAIL $MODE
    # $BIN $PROP2 $SCOPE $PREV_TRADING_DAY $DETAIL $DETAIL $MODE
    # $BIN $PROP3 $SCOPE $PREV_TRADING_DAY $DETAIL $DETAIL $MODE

    if [[ $MODEINSCT == "onetime" ]]
    then
        exit
    fi

    for i in $(seq 1 60)
    do
        echo -n "."
        sleep 1
    done

done
