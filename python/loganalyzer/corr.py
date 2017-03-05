import pandas as pd
import os


def round2(x):
    try:
        return round(x, 2)
    except:
        return x

dfs = []
gtype = 'PUSHPULL'
for var in [10, 100, 1000]:
    #for fn in os.listdir('../../output/%d' % var):
    for num in [200, 400, 600, 800, 1000, 5000]: 
        fn = '%d_sim_out_normal_%d_%s.csv' % (num, var, gtype)
        if gtype not in fn: continue
        df = pd.read_csv('../../output/%d/%s' % (var, fn)) 
        dfs.append(df)

    if num == 5000: continue
    dfs.append(pd.read_csv('../../output/%d_sim_out_normal_%d_%s.csv' % (num, var, gtype)))
output_df = dfs[0].append(dfs[1:], ignore_index=True)
output_df.rename(columns={'graphMeanDegree': 'Mean degree', 
                          'graphIndex': 'Index',
                          'graphOrder': 'Order'}, inplace=True)

output_df['X'] = output_df['meanL1AbsoluteError'].apply(lambda x: 1 if x>0.0001 else 0)
z_df = pd.DataFrame({'Z': output_df.groupby(['Order', 'Index', 'Mean degree'])['X'].sum()}).reset_index()
#print output_df


graph_df = pd.read_csv('../../python/g_prop.csv', sep='\t')
merged_df = output_df.merge(graph_df, on=['Order', 'Index', 'Mean degree'])

merged_df = merged_df.merge(z_df, on=['Order', 'Index', 'Mean degree'])


#print merged_df.corr()['meanRounds']
rslts = []
rslt = merged_df.corr('spearman')[['meanRounds', 
                                   'meanMessages',
                                   'meanWastedRounds',
                                   'Z']]

rslt.fillna(0, inplace=True)
#rslt = rslt.apply(round2)
rslt.to_csv('%s_spe.csv' % gtype)

rslt = merged_df.corr('pearson')[['meanRounds',
                                  'meanMessages',
                                  'meanWastedRounds',
                                  'Z']]

rslt.fillna(0, inplace=True)
#rslt = rslt.apply(round2)
rslt.to_csv('%s_pea.csv' % gtype)
#print merged_df.corr()['meanL1AbsoluteError']
#merged_df['X'] = merged_df['Min degree'].apply(lambda x: 1 if x<4 else 0)
#merged_df['Y'] = merged_df['Max degree'].apply(lambda x: 1 if x<4 else 0)


#merged_df['Z'] = merged_df['meanL1AbsoluteError'].apply(lambda x: 0 if x>0.001 else 1)

#from sklearn.metrics import mutual_info_score as mi
