# README

## Dependencies 
* **Ant**
* **SBT**
* **Scala 2.11.8**

## Instruction
After cloning the repository and before running sbt, one should first run 

```
#!shell

sh setupotpimizer.sh
```
This script installs the new optimizer for Scala, that deals with AKKA. The old Scala optimizer fails to incorporate well with AKKA, therefore we switch to a new prominent one. 

In the sequel, one can run sbt to build th project using the following command:
```
sbt clean compile
```

## Network Topologies
We run our simulations over the stochastic complex graphs, mainly scale-free and small-world graphs. One can run the file ''generategraph.py'' to regenerate the graphs. Every single graph is saved in a JSON format, and stored as a gzip file.

### Naming Conventions
#### Scale-free Graph
For the scale-free graph, we name them with the acronym ''sf'', followed by the graph order, the number initial nodes in the lattice, and the index. Index is used as we generate a few topologies even with the same graph generating parameters. For example:
```
#!any
sf_200_40_1.data.gz
```

#### Small-world Graph
The small-world graph uses the prefix ''sw'', the graph order, the number of the nodes in the initial ring, and the index. E.g.
```
sw_200_50_2.data.gz
```
Note that, we fix the rewiring probability to be 0.4 for the moment, but may change it later.