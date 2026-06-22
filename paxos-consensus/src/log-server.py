import sys
from types import resolve_bases
from typing import Iterable
from urllib.parse import urlparse
import http.server
import time
import socketserver
import signal
import socket
import hashlib
import json
import requests

from consensus import PaxNode

try:
    output_id, address, nodes_list = sys.argv[1], sys.argv[2], sys.argv[3:]

except IndexError:
    print("Usage: log-server.py <host:port>")
    sys.exit(1)

crashed = False
local_log = []

nodes_ID = []

class ThreadingHTTPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    pass


class LogRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_PUT(self):
        global crashed, local_log

        if crashed:
            print(f"\n{self.server.server_address} Received PUT request while crashed, ignoring\n")
            return
        
        if self_node.is_leader:
            if not crashed:
                content_length = int(self.headers['Content-Length'])
                data = self.rfile.read(content_length).decode('utf-8')

                if self_node.propose_value(data):
                
                    self.send_response(200)
                    self.end_headers()

        elif not self_node.is_leader:
            if not crashed:
                # Forward the request to current leader
                content_length = int(self.headers['Content-Length'])
                raw_data = self.rfile.read(content_length)
                #print(f"{self_node.address} Forwarding PUT request to {self_node.current_leader}\n") 

                if self_node.forward_request(self_node.current_leader, raw_data):
                
                    self.send_response(200)
                    self.end_headers()


    def do_POST(self):
        global crashed, local_log
        url = urlparse(self.path).path

        # If POST is extended, this case should be kept intact and overrule other URLs.
        if crashed and url != "/crash" and url != "/recover" and url != "/exit":
            print(f"\n{self.server.server_address} Received POST request while crashed, ignoring\n")
            return

        if url == "/crash":
            print(f"{self.server.server_address} Simulating crash...")
            # crashed = True
            self_node.crashed(True)
            self_node.stop_threads()
            self.send_response(400)
            self.end_headers()

        elif url == "/recover":
            print(f"{self.server.server_address} Simulating recovery...")
            # crashed = False
            self_node.recover_node(False)
            self_node.start_threads()
            self_node.recovery_routine()
            self.send_response(200)
            self.end_headers()

        elif url == "/exit":
            print(f"{self.server.server_address} Exiting...")
            self_node.stop_threads()
            self.send_response(200)
            self.end_headers()
            print(f"{self.server.server_address}: {local_log}")
            with open(f"output/{output_id}-server-{self.server.server_address[0]}{self.server.server_address[1]}.csv", 'w') as f:
                for entry in local_log:
                    f.write(f"{entry}\n")

        #TODO: All other API´s
        elif url == "/prepare":
            content_length = int(self.headers['Content-Length'])
            data = json.loads(self.rfile.read(content_length).decode('utf-8'))

            prop_n = data.get("msg_id")
            src = data.get("src")
            leader_id = data.get("lead_id")

            if not self_node.is_leader:
                self_node.update_leader(src, leader_id)

            self_node.prepare_handler(prop_n)
            self.send_response(200)
            self.end_headers()

        elif url == "/accept":
            content_length = int(self.headers['Content-Length'])
            data =  json.loads(self.rfile.read(content_length).decode('utf-8'))

            prop_n = data.get("msg_id")
            acc_val = data.get("value")

            self.send_response(200)
            self.end_headers()
            self_node.accept_handler(prop_n, acc_val)

        elif url == "/heartbeat":
            content_length = int(self.headers['Content-Length'])
            data =  json.loads(self.rfile.read(content_length).decode('utf-8'))

            self.send_response(200)
            self.end_headers()

        elif url == "/commit":
            content_length = int(self.headers['Content-Length'])
            data =  json.loads(self.rfile.read(content_length).decode('utf-8'))

            msg_id = data.get("msg_id")
            val = data.get("value")
            log_idx = data.get("idx")

            self_node.dist_log.commit(val, log_idx)

            self.send_response(200)
            self.end_headers()

        elif url == "/fward":
            content_length = int(self.headers['Content-Length'])
            data = self.rfile.read(content_length).decode('utf-8')
            if self_node.is_leader:
                if not crashed:
                    self_node.propose_value(data)
                    
            self.send_response(200)
            self.end_headers()

        elif url == "/validate_log":
            content_length = int(self.headers['Content-Length'])
            data =  json.loads(self.rfile.read(content_length).decode('utf-8'))
            node_loglen = data.get("log_len")

            local_loglen = len(self_node.loc_log)
            remote_loglen = node_loglen

            if local_loglen <= remote_loglen:
                self.send_response(200)
                self.end_headers()
                return
            
            m_data = {"logdata": local_loglen,
                    "addr": self_node.address}
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            r_data = json.dumps(m_data).encode('utf-8')
            print(f"\nReturn response body: [{r_data}] | [{m_data}]\n")
            self.send_header('Content-Length', str(len(r_data)))
            self.end_headers()
            self.wfile.write(r_data)



    def do_GET(self):
        global crashed, local_log
        url = urlparse(self.path).path

        if crashed:
            print(f"\n{self.server.server_address} Received GET request while crashed, ignoring\n")
            return

        if url == "/getlog":
            log_entries = json.dumps({"log": self_node.loc_log}).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(log_entries)))
            print(f"Sending requested log-update, entries: {log_entries}")
            self.end_headers()
            self.wfile.write(log_entries)


def generate_id(address):
    id = address.encode("utf-8")
    hash = hashlib.sha1(id).hexdigest()
    identifier = int(hash, 16) % (10**5)
    return identifier

def start_server(address):
    host, port = address.split(':')
    with ThreadingHTTPServer((host, int(port)), LogRequestHandler) as server:
        print(f"Serving HTTP on {host} port {port}...")
        server.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.serve_forever()



if __name__ == "__main__":
    gen_id = generate_id(address)

    kv_nodes = {}

    for node in nodes_list:
        node_ID = generate_id(node)
        kv_nodes[node] = node_ID
        nodes_ID.append(node_ID)

    max_hash = max(nodes_ID)
    max_id = max(kv_nodes, key=lambda k: kv_nodes[k])
    print(f"Node Dictionary:\n{max_id}\n")


    if gen_id != max_hash:
        self_node = PaxNode(address, gen_id, nodes_list, local_log, kv_nodes, nodes_ID, max_id, leader_id=None, is_leader=False)
    else:
        self_node = PaxNode(address, gen_id, nodes_list, local_log, kv_nodes, nodes_ID, max_id, leader_id=gen_id, is_leader=True)
    
    # Launch remaining nodes
    start_server(address)
