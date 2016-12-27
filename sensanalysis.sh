rm /tmp/gossipcala.log

for num in 600 800 1000 #200 400 600 800 1000
do 
  for ds in "normal_1000" #"normal_10" "normal_100" #"normal_1000"
  do
    for type in "pushsum" #"weighted" "pushpull"
    do
      echo $type $ds $num
      for i in {3..30}
      do
      if (( $i % 3 != 0 ))
      then
        echo "We skip the threshold $i"
        continue 
      fi
      echo "Testing threhold $i"
      sbt run "-Dalgorithm.stopping-threshold=$i \
               -Dsimulation.gossip-type=$type \
               -Dsimulation.num-nodes=$num \
               -Dsimulation.data-source=$ds"
      done
    done
  done
done

