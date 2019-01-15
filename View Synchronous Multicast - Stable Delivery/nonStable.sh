#!/bin/bash
VARIABLE="NON_STABLE"
NR_NODES=7
NR_MAX_NON_STABLE_MSGS=200
NR_MAX_STABLE_MSGS=10
NR_MEASUREMENTS=2
IP="239.0.0.4"
PORT=6789
DROP_RATE=0
AVG_DELAY=0
STD_DELAY=0
DIRECTORY="results"
FILEPATH="results/unstable/$(date +%Y%m%d_%H%M%S)"
if [ ! -d "$DIRECTORY" ]; then
  mkdir results
fi
mkdir $FILEPATH

for(( k = 0; k < $NR_MAX_NON_STABLE_MSGS + 1; k++ ))
do
  echo " "
  echo "NR NON STABLE MSGS" + $k
  echo " "
  for(( l = 0; l < $NR_MEASUREMENTS; l++ ))
  do
    echo " "
    echo "MEASURE" + $l
    echo " "
    PIDS=()
    for (( i = 1; i < $NR_NODES + 1; i++ ))
    do
      java -jar node.jar $NR_NODES $i $IP $PORT $DROP_RATE $AVG_DELAY $STD_DELAY $FILEPATH $NR_MAX_STABLE_MSGS $k $VARIABLE &
      PIDS+=($!)
    done
    echo ${PIDS[@]}
    sleep 1
    java -jar ../ControllerApp/controller.jar $NR_NODES 1 &
    PID=$!
    echo $PID
    wait "${PIDS[@]}"
    echo "All nodes terminated"
    kill $PID
    wait
    echo "Controller terminated"
    sleep 1
  done
  sleep 1
done
