import gzip
import networkx as nx
import numpy as np
from networkx.readwrite import json_graph
from ujson import dumps, load
import seaborn as sns
import pandas as pd
import matplotlib.pyplot as plt  


def read_graph(graph_json_f, num_nodes=200, par=45):
    fig, axes = plt.subplots(1, 3, figsize=(14, 4.5))
    
    #ax1 = plt.subplot(111)
    #ax2 = plt.subplot(122)

    graphs = []
    graph_json_fs = ['../graphs/sf_%d_%d_%d.data.gz' % (num_nodes, par, x) for x in xrange(5)]
    for graph_json_f in graph_json_fs:
        with gzip.open(graph_json_f) as f:
            data = load(f)
        
        del data['mean_degree']
        del data['var_degree']
        del data['meanSharedNeighbors']
        del data['index']
        del data['order']

        G = json_graph.node_link_graph(data)
        graphs.append(G)
    
    means = []
    print 'mean degree:', 
    for g in graphs:
        m = np.mean(g.degree().values())
        means.append(m)
        print m,
    else:
        print 
    
    vs = []
    print 'var degree:', 
    for g in graphs:
        var = np.var(g.degree().values())
        vs.append(var)
        print var,
    else:
        print 

    print 'min degree:', 
    for g in graphs: 
        print np.min(g.degree().values()),
    else:
        print 

    print 'max degree:', 
    for g in graphs:
        print np.max(g.degree().values()), 
    else:
        print 
    
    degrees = []
    names = []
    for i, g in enumerate(graphs):
        degrees.append(g.degree().values()) 
        names.append(['graph %d' % i] * g.order())
    
    degrees = np.array(degrees).flatten()
    names = np.array(names).flatten()
    print degrees.shape, names.shape
    df = pd.DataFrame({'degrees': degrees, 'names': names})
    sns.boxplot(x="names", y="degrees",
                data=df, linewidth=2.5, 
                ax=axes[0], palette='Set2', whis=[5, 95])

    yt = pd.read_csv('../output/10/%d_sim_out_normal_10_PUSHSUM.csv' % num_nodes)
    yt['Data'] = 'YT'
    n1 = pd.read_csv('../output/1000/%d_sim_out_normal_1000_PUSHSUM.csv' % num_nodes)
    n1['Data'] = 'N1'
    n2 = pd.read_csv('../output/100/%d_sim_out_normal_100_PUSHSUM.csv' % num_nodes)
    n2['Data'] = 'N2'
    df1 = yt.append([n1, n2], ignore_index=True)
    df1 = df1[(df1['graphMeanDegree'] <50) & (df1['graphMeanDegree'] >40)]
    df1['bad performance'] = df1['meanL1AbsoluteError'].apply(lambda x: 1. if x > 0.0001 else 0.)
    #yt_bad_rate = df1.groupby('graphIndex')['bad performance > 0.001'].sum() / 35.
    #print yt_bad_rate
    #print yt_bad_rate.as_matrix()
    #sns.barplot(range(5), 1. / yt_bad_rate.as_matrix(), ax=axes[1], palette='Set2')
    sns.barplot(x='graphIndex', y='bad performance', 
                data=df1, ax=axes[1], palette='Set2')
    maxes = np.array([np.min(g.degree().values()) for g in graphs], dtype=float)
    sns.barplot(x=range(5), y=1./maxes, 
                #y=np.array(vs)/np.array(means), 
                ax=axes[2], palette='Set2', errwidth=1.5)

    #df1['simCounter'] = pd.Series(range(df1.shape[0]))
    #sns.tsplot(time="graphIndex", value="meanL1AbsoluteError", 
    #           unit='simCounter', condition='Data', data=df1, ax=axes[2], 
    #           color='Set2')
    plt.tight_layout()
    plt.show()
    plt.close()
    

read_graph(None, 400, 45)
