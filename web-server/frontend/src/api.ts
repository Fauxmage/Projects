export interface Sample {
  x: number;
  y: number;
  z: number;
}

export interface Batch {
  id: number;
  timestamp: number;
  battery_level: number;
  samples: Sample[];
}

export interface BatchesResponse {
  batches: Batch[];
  total_count: number;
}

export interface BatteryResponse {
  labels: string[];
  data: number[];
}

export interface JitterStats {
  mean: number;
  std_dev: number;
  max: number;
  min: number;
  n_batches: number;
  n_intervals: number;
  n_outliers: number;
  expected_s: number;
}

export interface StatsResponse {
  stats: JitterStats[];
}

export interface ResultsResponse {
  files: string[];
}

export interface RuntimeResponse {
  first_timestamp: number | null;
  latest_timestamp: number | null;
  total_runtime_ms: number;
}

async function getJSON<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Request to ${url} failed: ${res.status}`);
  }
  return (await res.json()) as T;
}

export const api = {
  batches: (limit = 10) =>
    getJSON<BatchesResponse>(`/api/batches?limit=${limit}`),
  battery: () => getJSON<BatteryResponse>("/api/battery"),
  stats: () => getJSON<StatsResponse>("/api/stats"),
  results: () => getJSON<ResultsResponse>("/api/results"),
  runtime: () => getJSON<RuntimeResponse>("/api/runtime"),
};
