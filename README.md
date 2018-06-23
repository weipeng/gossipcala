# Gossip-based Distributed Averaging Algorithms
This repo was created for comparing three standard gossip solutions in the distributed mean problem. 
The question in this problem is simply that: what is the mean of some variables which are shared by a number of devices in the network?
One instance could be to ask the average temperature for the bunch of sensors in a wireless network. 
- Random Gossip 
- Push-sum Gossip
- Weighted Gossip 
Please check out the enoumous number of papers if you are unfamiliar with any of them. 

The repository host tried to write an paper with regard to the evaluation and the comparison of these three algorithms on three random graphs: random, scale-free, and small-world.
However, due to his bad writing skills and the lack of interest to the community, that paper did not work out.
And later, he just turned to some other completely different topics and got locked in. 
If you find this is at least of some interest to you, just take it. 

## Contributor
I would like to stress that the code is also contributed by @wuliaososhunhun https://github.com/wuliaososhunhun. 
He has contributed to the code at least as much as I have (or probably even more than I have).
He should be given the credit. 

# Running the Code 
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


