import subprocess


SCENARIOS = [0, 1, 2, 3, 4]

def launch_test(sc):
    for _ in range(0, 5):
        print(f"Running scenario {sc}, iteration {_}")
        r = subprocess.run(["./run.sh", f"{sc}"], capture_output=True, text=True, check=True)
        print(f"Result: {r.stdout}\n")

def iter_tests():
    print(f"###########################")
    print(f"##### LAUNCHING TESTS #####")
    print(f"###########################\n")

    for scenario in SCENARIOS:
        print(f"Current scenario in progress: {scenario}\n")
        launch_test(scenario)


if __name__ == "__main__":
    iter_tests()
