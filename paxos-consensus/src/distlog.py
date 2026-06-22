

class DistLog:
    def __init__(self, address, local_log):
        self.address = address
        self.local_log = local_log

    def commit(self, value, log_index):
        print(f"\nNode[{self.address}] has commited to log!\nCommited value: [{value}]\nLog index: [{log_index}]\n")
        self.local_log.append(value)

    def re_commit(self, value, n):
        if value:
            for entry in range(len(self.local_log), n):
                self.local_log.append(value[entry])
                print(f"\nNode[{self.address}] has updated its log!\nCommited value: [{value}]\nLog index: [{n}]\n")
