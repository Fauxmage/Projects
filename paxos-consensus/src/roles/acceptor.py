class Acceptor:
    def __init__(self, address, peers, is_leader):
        self.address = address
        self.peers = peers
        self.is_leader = is_leader
        self.n_promise = None
        self.accepted_val = None


    def promise(self, prop_n, src):
        if self.n_promise is None or prop_n > self.n_promise:
            self.n_promise = prop_n
            new_v = {
                "promise": True,
                "accepted_val": self.accepted_val
            }
            return new_v
        # Reject val if check is not true
        rejected_v = {
            "promise": True,
            "accepted_val": self.accepted_val
        }
        return rejected_v

    def accept(self, prop_n, val):
        # print(f"ACCEPTOR: n_p: {self.n_promise}, n: {prop_n}")
        if self.n_promise is None or prop_n > self.n_promise:
            print(f"[{self.address}] Accepted: n/n: {prop_n}/{self.n_promise}, proposal: {val}\n")
            self.n_promise = prop_n
            self.accepted_val = (prop_n, val)
            return True
        return False

    def accepted(self):
        return
