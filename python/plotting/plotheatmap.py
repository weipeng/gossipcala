import matplotlib.pyplot as plt
from matplotlib import cm
import seaborn as sns; sns.set()
import pandas as pd

plt.rc('text', usetex=True)
corrs = ['Corr type = Pearson', 'Corr type = Spearman']

for gtype in ['PUSHSUM', 'WEIGHTED', 'PUSHPULL']:
    fig, axes = plt.subplots(1, 2, sharey=True, figsize=(5., 5.)) 

    df1 = pd.read_csv('covs/%s_pea.csv' % gtype, index_col=0)
    df2 = pd.read_csv('covs/%s_spe.csv' % gtype, index_col=0)

    cbar_ax = fig.add_axes([.894, .3, .03, .4])
    sns.heatmap(df1, ax=axes[0], cbar=False, linewidth=1,   
                vmin=-1, vmax=1, annot=True, annot_kws={"size": 8})
    sns.heatmap(df2, ax=axes[1], linewidth=1, cbar_ax=cbar_ax, 
                vmin=-1, vmax=1, annot=True, annot_kws={"size": 8})

    for i, corr in enumerate(corrs):
        axes[i].set_title(corr, fontweight='bold')   

    plt.setp(axes[0].get_yticklabels(), rotation=0)

    plt.tight_layout(rect=[0, 0, .9, 1])
    plt.savefig('%s-corr.pdf' % gtype.lower())

plt.close('all')
