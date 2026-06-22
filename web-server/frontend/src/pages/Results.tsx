import { api } from "../api";
import { useFetch } from "../useFetch";
import { MessageCard, PageHeader } from "../components/PageState";
import { prettyResultName } from "../format";

export default function Results() {
  const { data, error, loading } = useFetch(() => api.results());
  const files = data?.files ?? [];

  return (
    <div>
      <PageHeader
        title="Model Accuracy"
        description="Confusion matrices for activity-classification at different sampling rates."
      />

      {loading && <MessageCard title="Loading…" />}
      {error && <MessageCard title="Failed to load results" subtitle={error} />}
      {!loading && !error && files.length === 0 && (
        <MessageCard title="No results to display." />
      )}

      {files.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {files.map((file) => (
            <figure key={file} className="card overflow-hidden">
              <figcaption className="flex items-center justify-between border-b border-zinc-800/70 px-4 py-3">
                <span className="text-sm font-medium text-zinc-200">
                  {prettyResultName(file)}
                </span>
                <span className="mono text-xs text-zinc-500">confusion matrix</span>
              </figcaption>
              <div className="bg-white p-3">
                <img
                  src={`/get-pdf/${file}`}
                  alt={`Confusion matrix ${prettyResultName(file)}`}
                  className="block h-auto w-full"
                />
              </div>
            </figure>
          ))}
        </div>
      )}
    </div>
  );
}
