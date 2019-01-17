#!/bin/bash
VARIABLE="NODES"
NR_NODES=6
NR_MAX_NON_STABLE_MSGS=10
NR_MAX_STABLE_MSGS=10
NR_MEASUREMENTS_FINAL=12
IP="239.0.0.4"
PORT=6789
DROP_RATE=0
AVG_DELAY=0
STD_DELAY=0
DIRECTORY="results"
FILEPATH="results/nodes/$NR_NODES"
if [ ! -d "$DIRECTORY" ]; then
  mkdir results
fi
mkdir $FILEPATH

for(( k = $NR_NODES; k < $NR_NODES + 1; k++ ))
do
  echo " "
  echo "NR NODES" + $k
  echo " "
  	x=$(($NR_NODES-1))
 	z=$(($NR_MEASUREMENTS_FINAL/$x))
	t=$(($NR_MEASUREMENTS_FINAL%$k))

	if [ $t -ne 0 ]; then
		((z+=1))
	fi
	NR_MEASUREMENTS=$z
  for(( l = 0; l < $NR_MEASUREMENTS; l++ ))
  do
    echo " "
    echo "MEASURE" + $l
    echo " "
    PIDS=()
    for (( i = 1; i < $k + 1; i++ ))
    do
      java -jar node.jar $k $i $IP $PORT $DROP_RATE $AVG_DELAY $STD_DELAY $FILEPATH $NR_MAX_STABLE_MSGS $NR_MAX_NON_STABLE_MSGS $VARIABLE &
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
