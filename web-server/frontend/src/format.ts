// Small formatting helpers shared across pages.

/** Epoch-milliseconds timestamp -> human-readable local date/time. */
export function formatTimestamp(ms: number): string {
  const d = new Date(ms);
  if (Number.isNaN(d.getTime())) return String(ms);
  return d.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

/** Tailwind text/dot classes for a battery level (emerald / amber / rose). */
export function batteryTone(level: number): { text: string; dot: string } {
  if (level >= 60) return { text: "text-emerald-300", dot: "bg-emerald-400" };
  if (level >= 25) return { text: "text-amber-300", dot: "bg-amber-400" };
  return { text: "text-rose-300", dot: "bg-rose-400" };
}

/** "cm_100hz.svg" -> "100 Hz" */
export function prettyResultName(file: string): string {
  const m = file.match(/(\d+)\s*hz/i);
  return m ? `${m[1]} Hz` : file;
}
