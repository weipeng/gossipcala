import gzip
import networkx as nx
from networkx.readwrite import json_graph
from cjson import encode 

for graph_type in ['sf', 'sw']:
    for num_nodes in xrange(200, 1001, 200):
        if graph_type == 'sf':
            for p in xrange(10, 51, 5):
                for i in xrange(5):
                    while True:
                        G = nx.barabasi_albert_graph(num_nodes, p)
                        if nx.is_connected(G):
                            break    
                    data = json_graph.adjacency_data(G)
                    jdata = encode(data)
                    with gzip.open('../graphs/sf_%d_%d_%d.data.gz'% (num_nodes, p, i), 'wb') as f:
                        f.write(jdata)

        if graph_type == 'sw':
            for p in xrange(20, 101, 10):
                for i in xrange(5):
                    while True:
                        G = nx.watts_strogatz_graph(num_nodes, p, .4)
                        if nx.is_connected(G):
                            break
                    data = json_graph.adjacency_data(G)
                    jdata = encode(data)
                    with gzip.open('../graphs/sf_%d_%d_%d.data.gz'% (num_nodes, p, i), 'wb') as f:
                        f.write(jdata)
