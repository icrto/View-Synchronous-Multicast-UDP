#!/bin/bash
NR_NODES=3
NR_MAX_NON_STABLE_MSGS=1
NR_MAX_STABLE_MSGS=3
NR_MEASUREMENTS=2
IP="239.0.0.4"
PORT=6789
DROP_RATE=0
AVG_DELAY=0
STD_DELAY=0
DIRECTORY="/Users/icrto/Documents/GitHub/SDIS/results"
FILEPATH="/Users/icrto/Documents/GitHub/SDIS/results/$(date +%Y%m%d_%H%M%S)"
if [ ! -d "$DIRECTORY" ]; then
  mkdir /Users/icrto/Documents/GitHub/SDIS/results
fi
mkdir $FILEPATH

for(( k = 0; k < $NR_MAX_NON_STABLE_MSGS + 1; k++ ))
do
  echo " "
  echo "NR NON STABLE MSGS" + $k
  echo " "
  for(( l = 0; l < $NR_MEASUREMENTS; l++ ))
  do
    cd /Users/icrto/Documents/GitHub/SDIS/'View Synchronous Multicast'
    echo " "
    echo "MEASURE" + $l
    echo " "
    PIDS=()
    for (( i = 1; i < $NR_NODES + 1; i++ ))
    do
      java -jar node.jar $NR_NODES $i $IP $PORT $DROP_RATE $AVG_DELAY $STD_DELAY $FILEPATH $NR_MAX_STABLE_MSGS $k &
      PIDS+=($!)
    done
    echo ${PIDS[@]}
    sleep 1
    cd /Users/icrto/Documents/GitHub/SDIS/ControllerApp
    java -jar controller.jar $NR_NODES 1 &
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
