import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  ScriptableContext,
} from "chart.js";
import { Line } from "react-chartjs-2";
import { api } from "../api";
import { useFetch } from "../useFetch";
import { MessageCard, PageHeader } from "../components/PageState";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

const GRID = "rgba(255,255,255,0.05)";
const TICK = "#71717a";
const EMERALD = "#34d399";

export default function BatteryGraph() {
  const { data, error, loading } = useFetch(() => api.battery(), [], {
    intervalMs: 10000,
  });

  const hasData = !loading && !error && data && data.labels.length > 0;

  return (
    <div>
      <PageHeader
        title="Battery"
        description="Pebble battery level per hour."
      />

      <div className="card p-5 md:p-6">
        <div className="h-[460px]">
          {loading && <MessageCard title="Loading…" />}
          {error && <MessageCard title="Failed to load data" subtitle={error} />}
          {!loading && !error && !hasData && (
            <MessageCard title="No data received yet" />
          )}
          {hasData && (
            <Line
              data={{
                labels: data.labels,
                datasets: [
                  {
                    label: "Battery %",
                    data: data.data,
                    borderColor: EMERALD,
                    borderWidth: 2,
                    pointRadius: 0,
                    pointHoverRadius: 4,
                    pointHoverBackgroundColor: EMERALD,
                    tension: 0.35,
                    fill: true,
                    backgroundColor: (ctx: ScriptableContext<"line">) => {
                      const { chart } = ctx;
                      const { ctx: c, chartArea } = chart;
                      if (!chartArea) return "rgba(52,211,153,0.08)";
                      const g = c.createLinearGradient(
                        0,
                        chartArea.top,
                        0,
                        chartArea.bottom
                      );
                      g.addColorStop(0, "rgba(52,211,153,0.28)");
                      g.addColorStop(1, "rgba(52,211,153,0)");
                      return g;
                    },
                  },
                ],
              }}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                interaction: { intersect: false, mode: "index" },
                plugins: {
                  legend: { display: false },
                  tooltip: {
                    backgroundColor: "#18181b",
                    borderColor: "#27272a",
                    borderWidth: 1,
                    titleColor: "#e4e4e7",
                    bodyColor: "#a1a1aa",
                    padding: 10,
                    displayColors: false,
                  },
                },
                scales: {
                  y: {
                    beginAtZero: false,
                    max: 100,
                    grid: { color: GRID },
                    border: { display: false },
                    ticks: { color: TICK, callback: (v) => `${v}%` },
                  },
                  x: {
                    grid: { display: false },
                    border: { display: false },
                    ticks: {
                      color: TICK,
                      maxRotation: 0,
                      autoSkipPadding: 24,
                    },
                  },
                },
              }}
            />
          )}
        </div>
      </div>
    </div>
  );
}
