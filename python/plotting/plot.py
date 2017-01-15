import re
import gzip
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import seaborn as sns
sns.set_context('paper')
sns.set(palette='Set1')

gtypes = {
    'PUSHSUM': 'Push-sum',
    'PUSHPULL': 'Push-pull',
    'WEIGHTED': 'Sum-weight'
}

def round2(x):
    return round(x, 2)

def plot(feature, show=False):
    #fig = plt.figure(figsize=(8., 4.9))
    for j in [10, 100, 1000]:
        fig, axes = plt.subplots(1, 5, figsize=(21., 3.9), sharey=True, dpi=1200)
        for i, num in enumerate(range(200, 1001, 200)):
            ax = axes[i]
            ax.locator_params(nbins=6, axis='x')
            log = '../../output/%d/%d_sim_out_normal_%d_%s.csv' % (j, num, j, 'PUSHPULL')
            df = pd.read_csv(log)
            df = pd.DataFrame()

            log1 = '../../output/%d/%d_sim_out_normal_%d_%s.csv' % (j, num, j, 'PUSHSUM')
            df1 = pd.read_csv(log1)
            #print num, df1[df1['graphMeanDegree'] <= 20]['meanRounds'].mean()
            log2 = '../../output/%d/%d_sim_out_normal_%d_%s.csv' % (j, num, j, 'WEIGHTED')
            df2 = pd.read_csv(log2)
            #print 'W', num, df2[df2['graphMeanDegree'] <= 20]['meanRounds'].mean()
              
            df = df.append([df1, df2], ignore_index=True)

            if j == 10:
                df['data'] = 'YT'
            elif j == 100:
                df['data'] = 'N1'
            elif j == 1000:
                df['data'] = 'N2'

            #df['simCounter'] = df['simCounter'] % 35

            df['graph order'] = 'G(%d)' % num
            df['Mean waste rate'] = df['meanWastedRounds'] / df['meanRounds'].astype(float)
        
            df['gossipType'] = df['gossipType'].apply(gtypes.get)
            df['graphMeanDegree'] = df['graphMeanDegree'].round(3)
            df['graphMeanDegree'] = df['graphMeanDegree'].round(2)
            df['meanSharedNeighbors'] = df['meanSharedNeighbors'].round(3)
            df['stdRounds'] = df['varRounds'].apply(np.sqrt)
          
            cdict = {'stoppingThreshold': 'stopping threshold',
                     'meanL1AbsoluteError': 'MLAPE',
                     'gossipType': 'Gossip type',
                     'graphMeanDegree': 'Mean degree',
                     'meanRounds': 'Mean NO. of rounds',
                     'varRounds': 'Variance of the NO. of the messages',
                     'meanWastedRounds': 'Mean NO. of waste rounds',
                     'meanMessages': 'Mean NO. of messages',
                     'varMessages': 'Variance of the NO. of the messages'}

            if feature == 'meanMessages':
                df[feature] = df[feature] + df['meanBusyMessages']
                
            if feature != 'Mean waste rate':
                sns.tsplot(time='graphMeanDegree', value=feature,
                           unit='simCounter', condition='gossipType',
                           ci=95, data=df, ax=ax, estimator=np.median)

                if feature== 'meanMessages':
                    df['Type'] = df['gossipType'] + ' - effective'
                    df[feature] = df[feature] - df['meanBusyMessages']
                    sns.tsplot(time='graphMeanDegree', value=feature,
                               unit='simCounter', condition='Type', 
                               color='orange', ci=95, marker='o',
                               data=df[df['Type'] == 'Push-pull - effective'], 
                               ax=ax, estimator=np.median)
            else:
                sns.boxplot(x='graphMeanDegree', y='Mean waste rate', 
                            hue='gossipType', notch=True, data=df, ax=ax,
                            whis=[5, 95], meanline=True)
            
                
            if i == 0: 
                ax.set_ylabel(cdict.get(feature, feature), fontsize=17)
            else:
                ax.set_ylabel('')
            
            handles, labels = ax.get_legend_handles_labels()
            ax.legend_.remove()
            ax.set_xlabel('Mean degree', fontsize=17)
            ax.set_title(r'G(%d)' % num, size=17)
            ax.set_rasterization_zorder(-10)
            
    
        loc = (.7, .91) if feature != 'meanMessages' else (.556, .91)
        plt.figlegend(handles=handles, labels=labels, 
                      loc=loc, fontsize=16, ncol=5)
        plt.tight_layout(pad=1.7, h_pad=2., w_pad=.8)
        #plt.tight_layout()
        #plt.subplots_adjust(hspace=1.)
        f_str = '-'.join(feature.split(' '))
        plt.savefig('./figures/%d-%s.pdf' % (j, f_str), dpi=1200) 

    plt.close('all')

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
    
    sns.set_style('white')
    ax = sns.tsplot(time='stopping threshold', value='MLAPE',
                    unit='simCounter', condition='gossip type',
                    interpolate=True,
                    err_style='ci_bars',
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
    plt.legend(handles=handles, labels=labels, loc=0, fontsize=14, frameon=True)

    ax.ticklabel_format(style='sci', axis='y', scilimits=(0,0), labelsize=8, useOffset=False)
    plt.rc('font', size=9)
    plt.xlabel(r'Stopping threashold $\tau$')

    gtype = gtypes[gossip_type]
    plt.title(r'%s in $G(%d)$' % (gtype, num), fontsize=14)
    
    plt.tight_layout()
    if show:
        plt.show()
    else:
        plt.savefig('./figures/sa/%s-%d-%s.pdf' % (gossip_type, num, f_str), format='pdf')
    
    plt.close()
    
if __name__ == '__main__':
    #for gtype in ['WEIGHTED', 'PUSHSUM', 'PUSHPULL']:
    #    for num in xrange(200, 1001, 200):
    #        plot_sens_analysis(gtype, num)
    plot('Mean waste rate')
    plot('meanL1AbsoluteError')
    plot('meanRounds')
    plot('meanMessages')
