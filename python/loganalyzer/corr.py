import pandas as pd
import os


dfs = []
for var in [10, 100, 1000]:
    for fn in os.listdir('../../output/%d' % var):
        if 'PUSHSUM' not in fn: continue
        df = pd.read_csv('../../output/%d/%s' % (var, fn)) 
        dfs.append(df)

output_df = dfs[0].append(dfs[1:], ignore_index=True)
output_df.rename(columns={'graphMeanDegree': 'Mean degree', 
                          'graphIndex': 'Index',
                          'graphOrder': 'Order'}, inplace=True)

output_df['X'] = output_df['meanL1AbsoluteError'].apply(lambda x: 1 if x>0.001 else 0)
output_df = pd.DataFrame({'Z': output_df.groupby(['Order', 'Index', 'Mean degree'])['X'].sum()}).reset_index()
#print output_df


graph_df = pd.read_csv('../../python/graphproperties.csv', sep=',|\t')
merged_df = output_df.merge(graph_df, on=['Order', 'Index', 'Mean degree'])


#print merged_df.corr()['meanL1AbsoluteError']
merged_df['X'] = merged_df['Min degree'].apply(lambda x: 1 if x<4 else 0)
merged_df['Y'] = merged_df['Max degree'].apply(lambda x: 1 if x<4 else 0)


#merged_df['Z'] = merged_df['meanL1AbsoluteError'].apply(lambda x: 0 if x>0.001 else 1)

from sklearn.metrics import mutual_info_score as mi
print mi(merged_df['X'], merged_df['Z']) 
print mi(merged_df['Y'], merged_df['Z'])
