import { api, Batch } from "../api";
import { useFetch } from "../useFetch";
import { MessageCard, PageHeader } from "../components/PageState";
import { formatTimestamp } from "../format";

function StatChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="card flex flex-col items-center justify-center px-5 py-4 text-center">
      <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">
        {label}
      </p>
      <p className="mono tnum mt-1.5 text-2xl font-semibold text-zinc-100">
        {value}
      </p>
    </div>
  );
}

function formatRuntime(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function BatchCard({ batch }: { batch: Batch }) {
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between border-b border-zinc-800/70 px-5 py-4">
        <div>
          <h2 className="text-sm font-semibold text-zinc-100">
            Batch{" "}
            <span className="mono text-emerald-300">#{batch.id}</span>
          </h2>
          <p className="mono mt-0.5 text-xs text-zinc-500">
            {formatTimestamp(batch.timestamp)}
          </p>
        </div>
      </div>

      <div className="px-5 py-3">
        <table className="w-full table-fixed text-sm">
          <colgroup>
            <col className="w-1/4" />
            <col className="w-1/4" />
            <col className="w-1/4" />
            <col className="w-1/4" />
          </colgroup>
          <thead>
            <tr className="text-[11px] uppercase tracking-wider text-zinc-500">
              <th className="pb-2 text-left font-medium">#</th>
              <th className="pb-2 text-left font-medium">X</th>
              <th className="pb-2 text-left font-medium">Y</th>
              <th className="pb-2 text-left font-medium">Z</th>
            </tr>
          </thead>
          <tbody className="mono tnum text-zinc-300">
            {batch.samples.map((s, i) => (
              <tr key={i} className="border-t border-zinc-800/50">
                <td className="py-1.5 text-left text-zinc-600">{i + 1}</td>
                <td className="py-1.5 text-left">{s.x}</td>
                <td className="py-1.5 text-left">{s.y}</td>
                <td className="py-1.5 text-left">{s.z}</td>
              </tr>
            ))}
            {batch.samples.length === 0 && (
              <tr>
                <td colSpan={4} className="py-3 text-center text-xs text-zinc-600">
                  no samples
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function Home() {
  const { data, error, loading } = useFetch(() => api.batches(4), [], {
    intervalMs: 10000,
  });
  const { data: runtimeData } = useFetch(() => api.runtime(), [], {
    intervalMs: 10000,
  });

  const batches = data?.batches ?? [];
  const orderedBatches = [...batches].sort(
    (a, b) =>
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
  );
  const latest = orderedBatches[0];
  const runtimeMs = runtimeData?.total_runtime_ms ?? 0;

  const stats = [
    {
      label: "Total batches",
      value: String(data?.total_count ?? batches.length),
    },
    {
      label: "Latest battery %",
      value: latest ? `${latest.battery_level}%` : "—",
    },
    {
      label: "Total runtime",
      value: batches.length > 0 ? formatRuntime(runtimeMs) : "—",
    },
  ];

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        title="Overview"
        description="Most recent data batches from the Pebble device."
      />

      {!loading && !error && batches.length > 0 && (
        <div className="mb-8 flex flex-wrap justify-center gap-4">
          {stats.map((stat) => (
            <StatChip
              key={stat.label}
              label={stat.label}
              value={stat.value}
            />
          ))}
        </div>
      )}

      {loading && <MessageCard title="Loading…" />}
      {error && <MessageCard title="Failed to load data" subtitle={error} />}
      {!loading && !error && batches.length === 0 && (
        <MessageCard
          title="No data received yet"
          subtitle="Batches will appear here as soon as they are uploaded."
        />
      )}

      {batches.length > 0 && (
        <div className="space-y-6">
          {orderedBatches.map((b) => (
            <div key={b.id} className="w-full">
              <BatchCard batch={b} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

