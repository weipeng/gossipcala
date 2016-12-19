from numpy.random import *
from numpy import savetxt

if __name__ == '__main__':
    prefix = '../syntheticdata/data'
    for n in xrange(200, 1001, 200):
        #normal_data = normal(100, 100, n)
        #savetxt('%s/normal_100_%d.csv.gz' % (prefix, n), normal_data)

        normal_data = normal(100, 1000, n)
        savetxt('%s/normal_1000_%d.csv.gz' % (prefix, n), normal_data)

        #logit_data = logistic(100, 100, n)
        #savetxt('%s/logit_100_%d.csv.gz' % (prefix, n), logit_data)

        #logit_data = logistic(100, 1000, n)
        #savetxt('%s/logit_1000_%d.csv.gz' % (prefix, n), logit_data)

