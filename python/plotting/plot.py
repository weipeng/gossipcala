import re
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import seaborn as sns
sns.set_context('talk')
sns.set_style('white')


def plot(log, feature):
    df = pd.read_csv(log)
    
    df.reset_index()
    df[['graphMeanDegree', 'simCounter', 'gossipType', 'meanEffectiveRounds']].to_csv('tmp.csv')
    ax = sns.tsplot(time='graphMeanDegree', value=feature,
                    unit='simCounter', condition='gossipType',
                    interpolate=True, 
                    err_style='ci_bars',# 'boot_traces', 'boot_kde', 'unit_traces', 'unit_points'],
                    data=df)

    plt.show() 
    plt.close()

def plot_sens_analysis(log, feature):
    plt.figure(figsize=(3.2, 3.7))
    #plt.rcParams['text.usetex'] = True

    df = pd.read_csv(log)
    df = df[['stoppingThreshold', 'gossipType', 'simCounter', feature]]

    df = df.rename(columns={'stoppingThreshold': 'stopping threshold',
                            'meanL1AbsoluteError': 'mean L1 absolute error',
                            'gossipType': 'gossip type'})
    ax = sns.tsplot(time='stopping threshold', value='mean L1 absolute error',
                    unit='simCounter', condition='gossip type',
                    interpolate=True,
                    err_style= 'ci_band', #['ci_band', 'ci_bars', 'unit_traces'], #'unit_points'],
                    data=df, ci=99, color='black')

    plt.ticklabel_format(style='sci', axis='y', scilimits=(0,0), labelsize=7, useOffset=False)
    plt.xlabel(r'stopping threashold $\tau$')
    plt.legend()
    plt.tight_layout()
    plt.savefig('%s.pdf' % log.rstrip('.csv'), format='pdf')
    plt.close()
    
if __name__ == '__main__':
    import sys
    
    plot_sens_analysis(sys.argv[1], 'meanL1AbsoluteError')
