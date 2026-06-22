import { useEffect, useState } from "react";

interface FetchState<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
}

interface FetchOptions {
  intervalMs?: number;
}

export function useFetch<T>(
  fn: () => Promise<T>,
  deps: unknown[] = [],
  options: FetchOptions = {},
): FetchState<T> {
  const { intervalMs } = options;
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const load = (initial: boolean) => {
      if (initial) {
        setLoading(true);
        setError(null);
      }
      return fn()
        .then((res) => {
          if (active) {
            setData(res);
            setError(null);
          }
        })
        .catch((e: unknown) => {
          if (active) setError(e instanceof Error ? e.message : String(e));
        })
        .finally(() => {
          if (active && initial) setLoading(false);
        });
    };

    load(true);

    const timer = intervalMs
      ? setInterval(() => load(false), intervalMs)
      : undefined;

    return () => {
      active = false;
      if (timer) clearInterval(timer);
    };
  }, [...deps, intervalMs]);

  return { data, error, loading };
}
