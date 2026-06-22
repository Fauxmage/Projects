import type { ReactNode } from "react";
import { api } from "../api";
import { useFetch } from "../useFetch";
import { MessageCard, PageHeader } from "../components/PageState";

function Stat({
  label,
  value,
  unit,
  accent,
  children,
}: {
  label: string;
  value: string;
  unit?: string;
  accent?: boolean;
  children?: ReactNode;
}) {
  return (
    <div className="card p-5">
      <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">
        {label}
      </p>
      <p
        className={`mono tnum mt-2 text-3xl font-semibold ${
          accent ? "text-emerald-300" : "text-zinc-100"
        }`}
      >
        {value}
        {unit && (
          <span className="ml-1 text-base font-normal text-zinc-500">
            {unit}
          </span>
        )}
      </p>
      {children}
    </div>
  );
}

export default function BatchStats() {
  const { data, error, loading } = useFetch(() => api.stats(), [], {
    intervalMs: 10000,
  });
  const s = data?.stats[0];

  const deviationMs = s ? Math.round((s.mean - s.expected_s) * 1000 * 10) / 10 : 0;
  const absDev = Math.abs(deviationMs);
  const devTone =
    absDev < 10
      ? "text-emerald-400"
      : absDev < 30
        ? "text-amber-400"
        : "text-rose-400";

  return (
    <div>
      <PageHeader
        title="Statistics"
        description="Expected interval at 100 Hz, 25 samples/batch is 0.250 seconds."
      />

      {loading && <MessageCard title="Loading…" />}
      {error && <MessageCard title="Failed to load data" subtitle={error} />}
      {!loading && !error && !s && (
        <MessageCard
          title="Not enough data uploaded yet"
        />
      )}

      {s && (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
          <Stat label="Mean interval" value={s.mean.toFixed(3)} unit="s" accent>
            <p className={`mt-2 text-xs font-medium ${devTone}`}>
              {deviationMs >= 0 ? "+" : ""}
              {deviationMs.toFixed(1)} ms vs. expected
            </p>
          </Stat>
          <Stat label="Std. deviation" value={s.std_dev.toFixed(3)} unit="s" />
          <Stat label="Min interval" value={s.min.toFixed(3)} unit="s" />
          <Stat label="Max interval" value={s.max.toFixed(3)} unit="s" />
          <Stat label="Batches persisted" value={String(s.n_batches)} />
          <Stat label="Large gaps" value={String(s.n_outliers)}>
            <p className="mt-2 text-xs text-zinc-500">
              excluded (&gt; 5 s or negative)
            </p>
          </Stat>
        </div>
      )}
    </div>
  );
}
