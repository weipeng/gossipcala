from numpy.random import *
from numpy import savetxt


if __name__ == '__main__':
    prefix = '../syntheticdata/data'
    for n in xrange(5000, 5001, 200):
        #normal_data = normal(100, 100, n)
        #savetxt('%s/normal_100_%d.csv.gz' % (prefix, n), normal_data)

        while True:
            normal_data = normal(100, 1000, n)
            print normal_data.mean(), normal_data.std()
            if abs(normal_data.mean()/100-1) < 0.01 and abs(normal_data.std()/1000-1) < 0.01: 
                savetxt('%s/normal_1000_%d.csv.gz' % (prefix, n), normal_data)
                break

        #logit_data = logistic(100, 100, n)
        #savetxt('%s/logit_100_%d.csv.gz' % (prefix, n), logit_data)

        #logit_data = logistic(100, 1000, n)
        #savetxt('%s/logit_1000_%d.csv.gz' % (prefix, n), logit_data)

