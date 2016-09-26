
with open('/tmp/gossipcala.log') as f:
    for line in f:
        tmps = line.strip().split(' - ')    
        if len(tmps) != 2:
            continue 
        
        if '/user/8#' in tmps[-1]:
            print tmps[-1]

