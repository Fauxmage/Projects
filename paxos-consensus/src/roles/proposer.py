import requests
import time

class Proposer:
    def __init__(self, address, id, peers, is_leader, log, leader, crashed, nodes_id):
        self.address = address
        self.id = id
        self.peers = peers
        self.current_leader = leader
        self.is_leader = is_leader
        self.is_crashed = crashed
        self.nodes_list = nodes_id

        self.n = 0    # n´th message, incremented by each message send
        self.log_idx = 0    # Log index if needed for consistent ordering
        self.log = log



    def prepare(self, address, leader_id):
        if not self.is_crashed:
            if self.is_leader:
                data = {
                    "msg_id": self.n,
                    "src": address,
                    "lead_id": leader_id
                }

                for peer in self.peers:
                    self.n += 1
                    url = f'http://{peer}/prepare'
                    try:
                        requests.post(url, json=data)
                    except requests.exceptions.RequestException:
                        print(f"Could not send proposal to {peer}\n")

    def accept(self, val):
        if not self.is_crashed:
            if self.is_leader:
                log_value = val
                quorum = (len(self.peers) + 1) // 2
                ctr = 0

                data = {
                    "msg_id": self.n,
                    "idx": self.log_idx,
                    "src": self.address,
                    "value": log_value
                }

                self.log_idx += 1
                for peer in self.peers:
                    url = f'http://{peer}/accept'
                    self.n += 1
                    try:
                        r = requests.post(url, json=data, timeout=3)
                        if r.status_code == 200:
                            ctr += 1
                    except requests.exceptions.RequestException:
                        print(f"[Leader]: Could not send accept to {peer}\n")

                if ctr >= quorum:
                    ctr = 0
                    for peer in self.peers:
                        try:
                            commit = f'http://{peer}/commit'
                            requests.post(commit, json=data, timeout=3)
                        except:
                            print(f"Could not make request to {peer}")

                    self.log.commit(val, self.log_idx)
                    return True
                return False
