rm /tmp/gossipcala.log

#for type in "pushsum" "pushpull"
for type in "weighted"
do
  echo $type
  for i in {3..30}
  do
  if (( $i % 3 != 0 ))
  then
    echo "We skip the threshold $i"
    continue 
  fi
  echo "Testing threhold $i"
  sbt run "-Dalgorithm.stopping-threshold=$i -Dsimulation.gossip-type=$type"
  done
done

