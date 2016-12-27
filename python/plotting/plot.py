import re
import gzip
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import seaborn as sns
sns.set_context('poster')
sns.set(palette='Set1')
sns.set_style('white')


def plot(log, feature, show=False):
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

def plot_sens_analysis(gossip_type, num, show=False):
    fig = plt.figure(figsize=(3.4, 3.75))
    gossip_type = gossip_type.upper()

    log = '../../output/sa/%d_sim_out_normal_1000_%s.csv' % (num, gossip_type.upper())
    log1 = '../../output/sa/%d_sim_out_normal_100_%s.csv' % (num, gossip_type.upper())    
    log2 = '../../output/sa/%d_sim_out_normal_10_%s.csv' % (num, gossip_type.upper())

    df0 = pd.read_csv(log)
    df0['gossipType'] = 'N1'

    df1 = pd.read_csv(log1)
    df1['gossipType'] = 'N2'        

    df2 = pd.read_csv(log2)
    df2['gossipType'] = 'YT'


    df = df0.append([df1, df2])
    df = df[['stoppingThreshold', 'gossipType', 'simCounter', 'meanL1AbsoluteError']]

    df = df.rename(columns={'stoppingThreshold': 'stopping threshold',
                            'meanL1AbsoluteError': 'MLAPE',
                            'gossipType': 'gossip type'})
    
    ax =sns.tsplot(time='stopping threshold', value='MLAPE',
                   unit='simCounter', condition='gossip type',
                   interpolate=True,
                   err_style= 'ci_bars',
                   data=df, ci=95,
                   color=['black', 'blue', 'red'])
    
    markers = ['^', 'o', '*']
    for i in xrange(1, 4):
        i *= -1
        ax.lines[i].set_marker(markers[i])

    handles, labels = ax.get_legend_handles_labels()
    for i, handle in enumerate(handles):
        handle.set_marker(markers[i])

    ax.legend_.remove()
    plt.legend(handles=handles, labels=labels, loc=0, fontsize=13, frameon=True)

    ax.ticklabel_format(style='sci', axis='y', scilimits=(0,0), labelsize=8, useOffset=False)
    plt.rc('font', size=9)
    plt.xlabel(r'Stopping threashold $\tau$')

    gtypes = {
        'PUSHSUM': 'Push-sum',
        'PUSHPULL': 'Push-pull',
        'WEIGHTED': 'Weighted'
    }
    gtype = gtypes[gossip_type]
    plt.title(r'%s in $G(%d)$' % (gtype, num), fontsize=14)
    
    plt.tight_layout()
    if show:
        plt.show()
    else:
        plt.savefig('%s-%d.pdf' % (gossip_type, num), format='pdf')
    
    plt.close()
    
if __name__ == '__main__':
    for gtype in ['WEIGHTED', 'PUSHSUM', 'PUSHPULL']:
        for num in xrange(200, 1001, 200):
            plot_sens_analysis(gtype, num)
    #plot_sens_analysis('WEIGHTED', 200, show=True)
