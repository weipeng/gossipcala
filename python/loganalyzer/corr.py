import pandas as pd
import os
import sys
from scikits.bootstrap import bootstrap as bs
from scipy.stats.mstats import spearmanr 
from scipy.stats import pearsonr

def sp(x, y):
    return spearmanr(x, y)[0]

def ps(x, y):
    return pearsonr(x, y)[0]

def round2(x):
    try:
        return round(x, 2)
    except:
        return x


def bs_ci_corr(cols, idxs, rslt, merged_df, func):
    for mea_k, mea_v in cols.items():
        for gprop_k, gprop_v in idxs.items():
            x = merged_df[mea_k].values
            y = merged_df[gprop_k].values
            lowhigh = bs.ci((x, y), func, n_samples=1000)
            lowhigh = tuple(map(round2, lowhigh))
            rslt.loc[rslt.index == gprop_v, mea_v] = '(%0.2f, %0.2f)' % lowhigh

    return rslt



cols = {'meanRounds': r'$\mathcal{R}$', 'meanMessages': r'$\mathcal{M}$',
        'meanWastedRounds': r'$\mathcal{W}$', 'Z': r'$\mathcal{I}$'}

idxs = {'Density': r'$DEN$',  
        'Mean degree': r'$\overline{V}$', 
        'Min degree': r'$Min(V)$',
        'Max degree': r'$Max(V)$', 
        'Variance of degree': r'$Var(V)$',
        'Mean eccentricity': r'$\overline{E}$', 
        'Min eccentricity': r'$Min(E)$',
        'Max eccentricity': r'$Max(E)$', 
        'Variance of eccentricity': r'$Var(E)$',
        'Mean clustering': r'$\overline{C}$', 
        'Min clustering': r'$Min(C)$',
        'Max clustering': r'$Max(C)$', 
        'Variance of clustering': r'$Var(C)$'}

dfs = []
gtype = sys.argv[1] #'WEIGHTED'
print (gtype)
for var in [10, 100, 1000]:
    for num in [200, 400, 600, 800, 1000]: #, 5000, 10000]: 
        fn = '%d_sim_out_normal_%d_%s.csv' % (num, var, gtype)
        df = pd.read_csv('../../output/%d/%s' % (var, fn)) 
        dfs.append(df)

    if num in [5000, 10000]: continue
    dfs.append(pd.read_csv('../../output/%d_sim_out_normal_%d_%s.csv' % (num, var, gtype)))

output_df = dfs[0].append(dfs[1:], ignore_index=True)
output_df.rename(columns={'graphMeanDegree': 'Mean degree', 
                          'graphIndex': 'Index',
                          'graphOrder': 'Order'}, inplace=True)

output_df['X'] = output_df['meanL1AbsoluteError'].apply(
                    lambda x: 1 if x>0.0001 else 0)

z_df = pd.DataFrame({
            'Z': output_df.groupby(['Order', 'Index', 
                                    'Mean degree'])['X'].sum()
        }).reset_index()


graph_df = pd.read_csv('../../python/g_prop.csv')

merged_df = output_df.merge(graph_df, on=['Order', 'Index', 'Mean degree'])

merged_df = merged_df.merge(z_df, on=['Order', 'Index', 'Mean degree'])

meas = ['meanRounds', 'meanMessages', 'meanWastedRounds', 'Z']
gprops = ['Density', 'Mean degree', 'Min degree', 
          'Max degree', 'Variance of degree',
          'Mean eccentricity', 'Min eccentricity',
          'Max eccentricity', 'Variance of eccentricity',
          'Mean clustering', 'Min clustering',
          'Max clustering', 'Variance of clustering'] 

rslts = []
rslt = merged_df.corr('spearman')[meas]

rslt.fillna(0, inplace=True)
#rslt = rslt.apply(round2)
rslt = rslt[rslt.index.isin(gprops)]
rslt.rename(index=idxs, columns=cols, inplace=True)
rslt.to_csv('%s_spe.csv' % gtype)

#rslt = bs_ci_corr(cols, idxs, rslt, merged_df, sp)
#rslt.to_csv('%s_spe-ci.csv' % gtype)

print (rslt)
print ('Finished processing spearman')

rslt = merged_df.corr('pearson')[meas]

rslt.fillna(0, inplace=True)
rslt = rslt[rslt.index.isin(gprops)]

rslt.rename(index=idxs, columns=cols, inplace=True)
rslt.to_csv('%s_pea.csv' % gtype)
#rslt = bs_ci_corr(cols, idxs, rslt, merged_df, ps)
#rslt.to_csv('%s_pea-ci.csv' % gtype)
print ('Finished processing pearman')

