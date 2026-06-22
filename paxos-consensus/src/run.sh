#!/bin/bash

scenario=${1:-0}
echo "You can give an integer input ./run.sh *n* to test scenario *n*, defaults to 0 if no input!"

num_servers=5
output_id=$(date "+%Y%m%d-%H%M%S")

nodes_list="nodes_list.txt"

# Check if the file exists
if [ -e "$nodes_list" ]; then
	# File exists, so truncate it to clear contents
	>"$nodes_list"
else
	# File does not exist, create it
	touch "$nodes_list"
fi

addresses=()

for i in $(seq 1 $num_servers); do
	number=$((RANDOM % (65535 - 49152 + 1) + 49152))
	address="localhost:$number"
	addresses+=("$address")
done

for address in "${addresses[@]}"; do
	echo python3 log-server.py $output_id $address "${addresses[@]}" &
	python3 log-server.py $output_id $address "${addresses[@]}" &
	echo "$address" >>"$nodes_list"
done

sleep 2

echo python3 log-client.py $output_id $scenario "${addresses[@]}"
python3 log-client.py $output_id $scenario "${addresses[@]}"
