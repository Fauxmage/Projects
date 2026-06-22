import requests
import random
import json

class Learner:
    def __init__(self, address, peers, is_leader, is_crashed, dist_log):
        self.address = address
        self.peers = peers
        self.is_leader = is_leader
        self.is_crashed = is_crashed
        self.log = dist_log

    def validate_log_entries(self, log):
        print(f"Node[{self.address}] recovered\nValidating log... \n")
        tmp = []
        log_len = len(log)
        data = {"log": log,
                "log_len": log_len,
                "addr": self.address}

        for peer in self.peers:
            url = f'http://{peer}/validate_log'
            try:
                r = requests.post(url, json=data, timeout=5)
                if r.status_code == 200:
                    tmp.append(r.json())
                    print(f"\n\nLog entries successfully validated!\nResponse: [{r}]\n\n")
            except requests.RequestException:
                print(f"No response from [{peer}]\n")

        if not tmp:
            print("No response from any peers\n")
            return


        #TODO: Fix target_node, corner case where
        # there are multiple nodes with equal log length.

        node_max_len = max(node['logdata'] for node in tmp)
        target_node = [node for node in tmp if node['logdata'] == node_max_len]
        
        # In case there are more than one node with the same number of log entries,
        # then we pick a random one.
        the_chosen_one = random.choice(target_node)

        self.fetch_updated_log(the_chosen_one['addr'], node_max_len)
    
    def fetch_updated_log(self, target, n):
        print(f"[{self.address}]: Fetching log entries from [{target}]\n")
        url = f'http://{target}/getlog'
        r = requests.get(url, timeout=5)
        r_data = r.json()
        self.log.re_commit(r_data['log'], n)

        print(f"\nReceived current log! [{r_data['log']}]\n\n")
