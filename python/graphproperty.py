import os
import gzip
import networkx as nx
import numpy as np
import simplejson as json
from networkx.readwrite import json_graph
from joblib import Parallel, delayed


def get_graph_property(fn, i):
    doc = ['Order', 'Index', 'Mean degree',
           'Variance of degree', 'Min degree', 'Max degree',
           'Radius', 'Diameter', 'Density', 'Connectivity',
           'Mean eccentricity', 'Variance of eccentricity', 'Min eccentricity', 'Max eccentricity',
           'Mean clustering', 'Variance of clustering', 'Min clustering', 'Max clustering']

    with open('graphproperties_%d.csv' % i, 'ab') as fout:
        fout.write('\t'.join(doc)+'\n')
        if 'sw' in fn: return

        print 'Processing', fn
        with gzip.open('../graphs/'+fn) as f:
            jdata = json.load(f)

        index = jdata['index']
        del jdata['index']
        del jdata['order']
        mean_degree = jdata['mean_degree']
        del jdata['mean_degree']
        var_degree = jdata['var_degree']
        del jdata['var_degree']
        del jdata['meanSharedNeighbors']

        G = json_graph.node_link_graph(jdata)
        min_degree = np.min(G.degree().values())
        max_degree = np.max(G.degree().values())

        ecc = nx.eccentricity(G)
        min_ecc = np.min(ecc.values())
        max_ecc = np.max(ecc.values())
        mean_ecc = np.mean(ecc.values())
        var_ecc = np.var(ecc.values())
        radius = min_ecc
        diameter = max_ecc
        #center = nx.center(G)
        #periphery = nx.periphery(G)
        density = nx.density(G)
        connectivity = nx.node_connectivity(G)

        clustering = nx.clustering(G)
        min_clust = np.min(clustering.values())
        max_clust = np.max(clustering.values())
        mean_clust = np.mean(clustering.values())
        var_clust = np.var(clustering.values())

        row = [G.order(), index, mean_degree,
               var_degree, min_degree, max_degree,
               radius, diameter, density, connectivity,
               mean_ecc, var_ecc, min_ecc, max_ecc,
               mean_clust, var_clust, min_clust, max_clust]

        fout.write('\t'.join(map(str, row)) + '\n')

if __name__ == '__main__':
    import sys

    n_jobs = 8 if len(sys.argv) <= 1 else int(sys.argv[1])
    Parallel(n_jobs=n_jobs)(
        delayed(get_graph_property)(fn, i%n_jobs) 
        for i, fn in enumerate(os.listdir('../graphs')))
