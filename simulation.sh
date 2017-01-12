rm /tmp/gossipcala.log
mkdir -p ./output/logs

for ds in "normal_100" "normal_1000"  # "normal_10" "normal_100" "normal_1000"
do 
  for num in 200 400 800 1000
  do
    for type in "weighted" #"pushsum" "pushpull"
    do
      if [ $type == "weighted" ]
      then 
        i=3
      elif [ $type == "pushsum" ]
      then 
        i=24
      elif [ $type == "pushpull" ]
      then
        i=3
      fi 
      echo $type $i
      sbt run "-Dalgorithm.stopping-threshold=$i \
               -Dsimulation.gossip-type=$type \
               -Dsimulation.num-nodes=$num \
               -Dsimulation.data-source=$ds"

      #mv /tmp/gossipcala.log ./output/logs/$num\_$type\_$ds.log
    done
  done
done

