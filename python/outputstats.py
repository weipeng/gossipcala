import numpy as np
import pandas as pd


for j in [10, 100, 1000]:
    dfs = []
    for num in xrange(200, 1001, 200):
        log = '../output/%d/%d_sim_out_normal_%d_%s.csv' % (j, num, j, 'PUSHPULL')
        df = pd.read_csv(log)
        dfs.append(df)
        print df.groupby('graphMeanDegree')['meanWastedRounds'].mean()
        des = df.describe()
        print j, num, des['meanWastedRounds']
    #df = dfs[0].append(dfs[1:], ignore_index=True)

    #des = df.describe()
    #print des['meanBusyMessages']

