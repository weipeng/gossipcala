# README

After clone the repository, before running sbt, first run 

```
#!shell

sh installOptimizer.sh 
```
This script installs the new optimizer in Scala, that deals with AKKA. Since the old Scala optimizer fails to incorporate well with AKKA, we switch to a new prominent one. 