import os
import gzip
import networkx as nx
import numpy as np
import simplejson as json
from networkx.readwrite import json_graph


with open('graphproperties.csv', 'wb') as fout:
    doc = ['Order', 'Index', 'Mean degree', 
           'Variance of degree', 'Min degree', 'Max degree',
           'Radius', 'Diameter', 'Density', 'Connectivity',
           'Mean clustering', 'Variance of clustering', 'Min clustering', 'Max clustering']

    fout.write('\t'.join(doc))
    fout.write('\n')
    for fn in os.listdir('../graphs'):
        if 'sf' not in fn: continue
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
        
        radius = nx.radius(G)
        diameter = nx.diameter(G)
        #eccentricity = nx.eccentricity(G)
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
               mean_clust, var_clust, min_clust, max_clust]
        fout.write('\t'.join(map(str, row)))
        fout.write('\n')


