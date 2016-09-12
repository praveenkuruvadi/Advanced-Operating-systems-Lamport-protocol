#!/bin/bash

PROG=Start
Program=Start.java
CONFIG=$1
netid=$2

config_file_name=$(echo $CONFIG | rev | cut -f1 -d"/" | rev | cut -f1 -d".")

sed -e "s/#.*//" $CONFIG | sed -e "/^\s*$/d" > temp
echo >> temp
node_count=0
host_names=()

current_line=1

while read line; 
do
	line=$(echo $line | tr -s ' ')

	if [ $current_line -eq 1 ]; then
		#number of nodes
		node_count=$(echo $line | cut -f1 -d" ")
		#convert it to an integer
  		let node_count=$node_count+0 
  		
  		root=$(echo $line | cut -f2 -d" ")
  		
  	else

  		if [ $current_line -le $(expr $node_count + 1) ]; then
  			#nodes_location+=$( echo -e $line"#" )	
  			node_id=$(echo $line | cut -f1 -d" ")
  			hostname=$(echo $line | cut -f2 -d" ")
  			host_names[$node_id]="$hostname"	
  		fi
  	fi
  	let current_line+=1
done < temp;

javac $Program &&

# iterate through the date collected above and execute on the remote servers
for node_id in $(seq 0 $(expr $node_count - 1))
do
	host=${host_names[$node_id]}
	domain=".utdallas.edu"
        machine=$host$domain
        ssh -o StrictHostKeyChecking=no $netid@$machine "cd $(pwd); java $PROG $node_id $CONFIG" &

done


