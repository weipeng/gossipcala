# README

## Dependencies 
* **Ant**
* **SBT**
* **Scala 2.11.8**
* **Python 2.7**

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
Please note that, all the python code in the project is in Python2. Running Python3 may get bugs.

To install all the required python packages, run
```
pip install ujson numpy scipy scikit-learn networkx matplotlib pandas seaborn joblib simplejson scikits.bootstrap
```


