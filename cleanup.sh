
CONFIG=$1
netid=$2

n=1
cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    #echo $i
    nodes=$( echo $i | cut -f1 -d" ")
    while read line 
    do
        host=$( echo $line | awk '{ print $2 }' )
	domain=".utdallas.edu"
        machine=$host$domain

        echo $machine
        ssh -o StrictHostKeyChecking=no $netid@$machine "ps -u $USER | grep java | tr -s ' ' | cut -f1 -d' ' | xargs kill " &
        ssh -o StrictHostKeyChecking=no $netid@$machine "ps -fu $USER | grep java | tr -s ' ' | cut -f2 -d' ' | xargs kill " &

        n=$(( n + 1 ))
        if [ $n -gt $nodes ];then
        	break
        fi
    done
   
)


echo "Cleanup complete"

