import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import os
import json
import sklearn
from sklearn.model_selection import (
    StratifiedShuffleSplit,
    cross_val_score,
    StratifiedKFold,
)
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, f1_score
from sklearn.preprocessing import StandardScaler


CSV_DIR_100 = "csv_data_100hz"
CSV_DIR_50 = "csv_data_50hz"
CSV_DIR_32 = "csv_data_32hz"

WINDOW_SIZE = 200  # Try experimenting with different window sizes!
OVERLAP = 0.5  # Remember to cite paper
RANDOM_STATE = 42

ADL_MAP = {
    "walking": 1,
    "sitting": 2,
    "standing": 3,
    "laying": 4,
    # "stairs_up": 5,
    # "stairs_down": 6
}


def extract_features(window):
    # Calculate mean, std, range for each axis
    means = np.mean(window, axis=0)
    stds = np.std(window, axis=0)
    ranges = np.ptp(window, axis=0)

    # SMA given by: sum(abs(x) + abs(y) + abs(z)) / window_length
    sma = np.sum(np.abs(window)) / window.shape[0]

    # Try adding tilting to better distinguish between static ADLs than just using mean, std, range
    tilt_x = (
        np.mean(
            np.arctan2(window[:, 0], np.sqrt(window[:, 1] ** 2 + window[:, 2] ** 2))
        )
        * 180
        / np.pi
    )
    tilt_y = (
        np.mean(
            np.arctan2(window[:, 1], np.sqrt(window[:, 0] ** 2 + window[:, 2] ** 2))
        )
        * 180
        / np.pi
    )
    tilt_z = (
        np.mean(
            np.arctan2(np.sqrt(window[:, 0] ** 2 + window[:, 1] ** 2), window[:, 2])
        )
        * 180
        / np.pi
    )

    return np.hstack([means, stds, ranges, sma, tilt_x, tilt_y, tilt_z])


def load_dataset(directory):
    X = []
    y = []

    window_size = WINDOW_SIZE

    if "100hz" in directory:
        window_size = 500
    elif "50hz" in directory:
        window_size = 250
    elif "32hz" in directory:
        window_size = 160

    for activity, activity_id in ADL_MAP.items():
        samples_path = os.path.join(directory, f"{activity}_accel_sample.csv")

        if not os.path.exists(samples_path):
            print(f"{activity}: File not found.")
            continue

        df = pd.read_csv(samples_path)
        data = df[["X_AXIS", "Y_AXIS", "Z_AXIS"]].values

        # Sliding Window
        step = int(window_size * (1 - OVERLAP))
        for i in range(0, len(data) - window_size, step):
            window = data[i : i + window_size]
            X.append(extract_features(window))
            y.append(activity_id)

    return np.array(X), np.array(y)


def evaluate_model(model, X_test, y_test, class_names, DIR_PATH):
    y_pred = model.predict(X_test)

    print("Testing Results:")
    print(f"Weightd F1-Score: {f1_score(y_test, y_pred, average='weighted'):.4f}")
    print("\n\nSci-Kit Classification Report:")
    print(classification_report(y_test, y_pred, target_names=class_names))

    

    
    cm = confusion_matrix(y_test, y_pred)
    cm_norm = cm.astype("float") / cm.sum(axis=1)[:, np.newaxis] * 100

    plt.figure(figsize=(10, 8))
    sns.heatmap(
        cm_norm,
        annot=True,
        fmt=".1f",
        cmap="Blues",
        xticklabels=class_names,
        yticklabels=class_names,
    )

    current_hz = DIR_PATH.split("_")[-1].replace("hz", "")

    plt.title(f"Normalized Confusion Matrix {current_hz} Hz")
    plt.ylabel("True Activity")
    plt.xlabel("Predicted Activity")
    plt.savefig(f"cm_{current_hz}hz.svg")
    # plt.show()


def main(DIR_PATH):
    X, y = load_dataset(DIR_PATH)

    if len(X) == 0:
        exit()

    strat_kfold = StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE)
    model_cv = RandomForestClassifier(
        n_estimators=100, random_state=RANDOM_STATE, n_jobs=-1
    )
    cv_scores = cross_val_score(model_cv, X, y, cv=strat_kfold, scoring="f1_weighted")

    print(f"Mean Cross Val. F1 Score: {np.mean(cv_scores):.4f}\n")
    print(f"Standard Deviation:  {np.std(cv_scores):.4f}\n")

    strat_shuffle = StratifiedShuffleSplit(
        n_splits=1, test_size=0.2, random_state=RANDOM_STATE
    )

    train_idx, test_idx = next(strat_shuffle.split(X, y))
    X_train, X_test = X[train_idx], X[test_idx]
    y_train, y_test = y[train_idx], y[test_idx]

    # Standardize features
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_test = scaler.transform(X_test)

    model = RandomForestClassifier(
        n_estimators=100, random_state=RANDOM_STATE, n_jobs=-1
    )
    model.fit(X_train, y_train)

    classes = [adl.capitalize() for adl in ADL_MAP.keys()]
    evaluate_model(model, X_test, y_test, classes, DIR_PATH)
    
    print(f"\nSciKit-Learn Version: {sklearn.__version__}")


if __name__ == "__main__":
    for dir in [CSV_DIR_100, CSV_DIR_50, CSV_DIR_32]:
        print(f"\n{'=' * 60}\nCurrently Processing: {dir}\n{'=' * 60}")
        main(dir)
