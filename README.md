# README

## Dependencies 
* **Ant**
* **SBT**
* **Scala 2.11.8**

## Instruction
### Build and Run
One can run sbt to build the project using the following command:
```
sbt clean compile 
```
One option to run the simulations is using 
```
sbt run 
```

## Dependency of Python
To install all the required python packages, run
```
pip install ujson numpy scipy scikit-learn networkx matplotlib pandas seaborn
```
or, if you prefer conda, run the following one instead.
```
conda install ujson numpy scipy scikit-learn networkx matplotlib pandas seaborn
```
