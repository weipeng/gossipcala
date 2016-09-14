# README

## Dependencies 
* **Ant**
* **SBT**
* **Scala 2.11.8**

## Instruction
[INVALID] After cloning the repository and before running sbt, one should first run 

```
#!shell

sh setupotpimizer.sh
```
This script installs the new optimizer for Scala, that deals with AKKA. The old Scala optimizer fails to incorporate well with AKKA, therefore we switch to a new prominent one. 

In the sequel, one can run sbt to build th project using the following command:
```
sbt clean compile
```