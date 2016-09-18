from numpy.random import *
from numpy import savetxt

if __name__ == '__main__':
    for n in xrange(200, 1001, 200):
        normal_data = normal(100, 100, n)
        savetxt('data/normal_100_%d.csv.gz' % n, normal_data)

        normal_data = normal(100, 1000, n)
        savetxt('data/normal_1000_%d.csv.gz' % n, normal_data)

        logit_data = logistic(100, 100, n)
        savetxt('data/logit_100_%d.csv.gz' % n, logit_data)

        logit_data = logistic(100, 1000, n)
        savetxt('data/logit_1000_%d.csv.gz' % n, logit_data)

