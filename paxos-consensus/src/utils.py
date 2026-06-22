import os
import subprocess


def kill_servers():
    nodes_list = "nodes_list.txt"

    with open(nodes_list, "r") as f:
        lines = f.readlines()

    for line in lines:
        node = line.strip()
        if not node:
            continue

        try:
            host, port = node.split(":")
            pid = subprocess.check_output(f"lsof -ti:{port}", shell=True, text=True).strip()
            
            if pid:
                print(f"Killing process {pid} on port {port}")
                os.system(f"kill {pid}")

        except subprocess.CalledProcessError:
            print(f"No process found running on port {port}")

    print("All servers stopped.")

    if os.path.exists(nodes_list):
        os.remove(nodes_list)
        print("File [nodes_list.txt] deleted.")


def cmp_log(output_id):
    result = None
    r = subprocess.run(["python3", "log-comparer.py", f"{output_id}"], capture_output=True, text=True)

    print(f"Result: {r.stdout}")

    for line in r.stdout.splitlines():
        cleanse = line.strip()
        if cleanse in ("Success", "Failure"):
            result = cleanse
            print(result)
            break

    return result

if __name__ == "__main__":
    kill_servers()
