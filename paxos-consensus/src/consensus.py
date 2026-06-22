import hashlib
import time
import requests
import threading
from distlog import DistLog
from roles.proposer import Proposer
from roles.acceptor import Acceptor
from roles.learner import Learner


"""
Consensus Algorithm: Multi-Paxos

Keep all code related to paxos in this file!

    1. Roles -> Seperate classes
    2. Phases -> Methods
    3. Leader Selection process and Leader Failure detection

Note(s) to self:
 - Designwise, roles as seperate classes, maybe phases too?
 - Cut classes early if implementation goes to shit!!
 - Just get it somewhat working at first, look for
   tmux.sh script used during Chord if necessary.
 - Check out the typing library
"""

class PaxNode:
    def __init__(self, address, id, nodes_list, local_log, nodes_hash, nodes_id, set_leader, leader_id, is_leader=False):
        self.kv_nodes = nodes_hash
        self.nodes_id = nodes_id
        self.address = address
        self.peers = self.peers_cleanup(nodes_list)
        self.id = id

        self.is_leader = is_leader
        self.is_crashed = None
        self.current_leader = set_leader
        self.current_leader_id = leader_id
        
        self.loc_log = local_log
        self.dist_log = DistLog(address, local_log)
        self.proposer = Proposer(self.address, self.id, self.peers, self.is_leader, self.dist_log, self.current_leader, self.is_crashed, self.nodes_id)
        self.acceptor = Acceptor(self.address, self.peers, self.is_leader)
        self.learner = Learner(self.address, self.peers, self.is_leader, self.is_crashed, self.dist_log)

        self.hb_thread = None
        self.running = False

        self.leader_init()

        if not self.is_leader:
            self.start_threads()


    def start_threads(self):
        if self.hb_thread is None or self.hb_thread.is_alive():
            self.hb_thread = threading.Thread(target=self.send_hb, daemon=True)
            self.hb_thread.start()

    def stop_threads(self):
        self.running = False
        if self.is_crashed:
            if self.hb_thread is not None:
                self.hb_thread.join(timeout=1)

    def update_leader(self, leader, leader_id):
        self.current_leader = leader
        self.current_leader_id = leader_id

    def leader_init(self):
        if self.is_leader:
            self.current_leader = self.address
            self.proposer.prepare(self.address, self.id)

    def peers_cleanup(self, peers):
        for peer in peers:
            if peer == self.address:
                peers.remove(peer)
                return peers

    def crashed(self, is_crashed):
        self.is_crashed = is_crashed
        print(f"Node[{self.address}] has crashed: {self.is_crashed}")

    def recover_node(self, recovered):
        self.is_crashed = recovered
        print(f"Node[{self.address}] has recovered: {self.is_crashed}.")


    def set_state(self, updated_state):
        self.state = updated_state

    def propose_value(self, val):
        if not self.is_crashed:
            if self.is_leader:
                return self.proposer.accept(val)
            else:
                # Forward the HTTP request to the current leader
                return self.forward_request(self.current_leader, val)

    def prepare_handler(self, prop_n):
        if not self.is_crashed:
            r = self.acceptor.promise(prop_n, self.address)
            return r

    def accept_handler(self, n, val):
        if not self.is_crashed:
            r = self.acceptor.accept(n, val)
            if r == True:
                return True
            return False

    def forward_request(self, leader, val):
        if not self.is_crashed:
            if not self.is_leader:
                url = f'http://{leader}'
                try:
                    r = requests.put(url, data=val, timeout=5)
                    if r.status_code == 200:
                        return True
                    return False
                except requests.exceptions.RequestException:
                    print(f"Failed to forward request to current leader\n")
    

    def recovery_routine(self):
        if not self.is_crashed:
            self.learner.validate_log_entries(self.loc_log)

    def send_hb(self):
        time.sleep(1)

        hb_tries = 0
        while not self.is_crashed:
            while not self.is_leader:
                url = f'http://{self.current_leader}/heartbeat'
                data = {"node": self.address}
                try:
                    r = requests.post(url, json=data, timeout=1)
                    hb_tries += 1
                    if r.status_code == 200:
                        print(f"Heartbeat received by [{self.current_leader}, attempts: {hb_tries}]\n")
                        hb_tries = 0

                except requests.exceptions.RequestException:
                    hb_tries += 1
                    print(f"Could not send heartbeat to {self.current_leader}, attempts {hb_tries} | Leader Election\n")
                    self.propse_new_leader()
                time.sleep(1)

    def propse_new_leader(self):
        if not self.is_leader:
            n = 0
            tmp_nodes = self.nodes_id
            curr_lead = max(tmp_nodes)
            if not self.is_crashed:
                tmp_nodes.remove(curr_lead)
                if self.id == max(tmp_nodes):
                    print(f"\n\nMy ID: {self.id} | Max ID: {max(tmp_nodes)} | IDs: {tmp_nodes}\n\n")
                    self.is_leader = True
                    self.stop_threads()

                    data = {
                        "msg_id": n,
                        "src": self.address,
                        "lead_id": self.id
                    }

                    for peer in self.peers:
                        url = f'http://{peer}/prepare'
                        try:
                            requests.post(url, json=data, timeout=5)
                        except requests.exceptions.RequestException:
                            print(f"Could not send proposal to {peer}\n")
                return
