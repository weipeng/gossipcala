# README

## Dependencies 
* **Ant**
* **SBT**
* **Scala 2.11.8**

## Instruction
### Optimizer (DEPRECIATED)
[DEPRECIATED] After cloning the repository and before running sbt, one should first run 

```
#!shell

sh setupotpimizer.sh
```
This script installs the new optimizer for Scala, that deals with AKKA. The old Scala optimizer fails to incorporate well with AKKA, therefore we switch to a new prominent one. 

### Build and Run
In the sequel, one can run sbt to build the project using the following command:
```
sbt clean compile
```
One option to run the simulations is by:
```
sbt run
```