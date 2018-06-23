# Gossip-based Distributed Averaging Algorithms
This repo was created for comparing three standard gossip solutions in the distributed mean problem. 
The repository host tried to write an paper with regard to the evaluation and the comparison of these three algorithms on three random graphs: random, scale-free, and small-world.
However, due to my bad writing skills and the lack of interest to the community, that paper did not work out.
And, I just turned to some other completely different topics. 

Anyone who finds that this has at least a little value, just takes it. 

I would like to stress that the code is also contributed by @wuliaososhunhun https://github.com/wuliaososhunhun. 
He has contributed to the code as least as much as I have (or probably even more than I have).

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
pip install numpy scipy scikit-learn networkx matplotlib pandas seaborn joblib simplejson scikits.bootstrap
```


