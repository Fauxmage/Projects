import pandas as pd
import numpy as np
from scipy import signal
import os


def resample_dataset(input_dir, output_dir, target_fs=32, source_fs=100):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    activities = ["walking", "sitting", "standing", "laying"]

    for activity in activities:
        file_path = os.path.join(input_dir, f"{activity}_accel_sample.csv")
        if not os.path.exists(file_path):
            continue

        print(f"Resampling {activity} to {target_fs}Hz...")
        df = pd.read_csv(file_path)

        num_samples = int(len(df) * target_fs / source_fs)

        resampled_data = {}
        for axis in ["X_AXIS", "Y_AXIS", "Z_AXIS"]:
            resampled_data[axis] = signal.resample(df[axis].values, num_samples)

        resampled_df = pd.DataFrame(resampled_data)

        output_path = os.path.join(output_dir, f"{activity}_accel_sample.csv")
        resampled_df.to_csv(output_path, index=False)


if __name__ == "__main__":
    resample_dataset("csv_data", "csv_data_32hz")
