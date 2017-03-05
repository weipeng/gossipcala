import gzip
import networkx as nx
import numpy as np
from networkx.readwrite import json_graph
from simplejson import dumps

for graph_type in ['sf', 'sw']:
    for num_nodes in xrange(10000, 10001, 200):
        if graph_type == 'sf':
            for p in xrange(5, 51, 5):
                for i in xrange(5):
                    while True:
                        G = nx.barabasi_albert_graph(num_nodes, p)
                        #G = nx.watts_strogatz_graph(num_nodes, 30, 0.4)
                        if nx.is_connected(G):# and np.min(G.degree().values()) <= 5:
                            break
                        print np.min(G.degree().values())

                    data = json_graph.node_link_data(G)
                    data['index'] = i 
                    data['order'] = G.order()
                    data['mean_degree'] = np.mean(G.degree().values())
                    data['var_degree'] = np.var(G.degree().values())
                    
                    nodes = G.nodes()
                    shared_nbs_count = []
                    #for k, node_i in enumerate(nodes):
                    #    for node_j in nodes[k+1:]:
                    #        shared_nbs_count.append(sum(1 for _ in nx.common_neighbors(G, node_i, node_j)))                         
                    data['meanSharedNeighbors'] = 0 #np.mean(shared_nbs_count)

                    jdata = dumps(data)
                    with gzip.open('../graphs/sf_%d_%d_%d.data.gz'% (num_nodes, p, i), 'wb') as f:
                        f.write(jdata)
        continue
        if graph_type == 'sw':
            for p in xrange(20, 101, 10):
                for i in xrange(5):
                    while True:
                        G = nx.watts_strogatz_graph(num_nodes, p, .4)
                        if nx.is_connected(G):
                            break
                    data = json_graph.node_link_data(G)
                    data['index'] = i
                    data['order'] = G.order()
                    data['mean_degree'] = np.mean(G.degree().values())
                    data['var_degree'] = np.var(G.degree().values())
                    
                    nodes = G.nodes()
                    shared_nbs_count = []
                    for k, node_i in enumerate(nodes):
                        for node_j in nodes[k+1:]:
                            shared_nbs_count.append(sum(1 for _ in nx.common_neighbors(G, node_i, node_j)))                         
                    data['meanSharedNeighbors'] = np.mean(shared_nbs_count)

                    jdata = dumps(data)
                    with gzip.open('../graphs/sw_%d_%d_%d.data.gz'% (num_nodes, p, i), 'wb') as f:
                        f.write(jdata)
