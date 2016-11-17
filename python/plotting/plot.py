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

def plot_sens_analysis(log1, log2, feature):
    fig = plt.figure(figsize=(3.9, 4.1))
    ax = fig.add_subplot(111)
    ax1 = ax.twinx()
    #plt.rcParams['text.usetex'] = True

    df = pd.read_csv(log1)
    df = df[['stoppingThreshold', 'gossipType', 'simCounter', feature]]

    df = df.rename(columns={'stoppingThreshold': 'stopping threshold',
                            'meanL1AbsoluteError': 'mean L1 absolute error',
                            'gossipType': 'gossip type'})
    sns.tsplot(time='stopping threshold', value='mean L1 absolute error',
                    unit='simCounter', condition='gossip type',
                    interpolate=True, ax=ax,  
                    err_style= 'ci_bars', #['ci_band', 'ci_bars', 'unit_traces'], #'unit_points'],
                    data=df, ci=99, color='black', marker='^')
    

    lgds = [r'$\mathbf{\mathcal{N}(0, 100)}$', r'$\mathbf{\mathcal{N}(0, 1000)}$']
    lgds = ['low variance', 'high variance']


    
    df1 = pd.read_csv(log2)
    df1 = df1[['stoppingThreshold', 'gossipType', 'simCounter', feature]]

    df1 = df1.rename(columns={'stoppingThreshold': 'stopping threshold',
                              'meanL1AbsoluteError': 'mean L1 absolute error',                            
                              'gossipType': 'gossip type'})

    sns.tsplot(time='stopping threshold', value='mean L1 absolute error',
                unit='simCounter', condition='gossip type',
                #interpolate=True, 
                err_style= 'ci_band', #['ci_band', 'ci_bars', 'unit_traces'], #'unit_points'],
                data=df1, ci=99, ax=ax1, color='r')
    
    #ax1.lines[-1].set_marker("o")
    ax1.yaxis.label.set_visible(False)

    handles, labels = ax.get_legend_handles_labels()
    handles1, labels1 = ax1.get_legend_handles_labels()
    ax.legend_.remove()
    ax1.legend_.remove()
    plt.legend(handles=handles+handles1, labels=lgds, loc=0, prop={'weight': 'bold'})
    for tl in ax1.get_yticklabels():
        tl.set_color('r')

    ax.ticklabel_format(style='sci', axis='y', scilimits=(0,0), labelsize=7, useOffset=False)
    ax1.ticklabel_format(style='sci', axis='y', scilimits=(0,0), labelsize=7, useOffset=False)
    plt.xlabel(r'stopping threashold $\tau$')
    plt.title('Push Sum')
    
    plt.tight_layout()
    plt.savefig('%s.pdf' % log1.rstrip('.csv'), format='pdf')

    plt.close()
    
if __name__ == '__main__':
    import sys
    
    plot_sens_analysis(sys.argv[1], sys.argv[2], 'meanL1AbsoluteError')
