import { NavLink, Outlet, useLocation } from "react-router-dom";

const links = [
  { to: "/", label: "Overview", end: true },
  { to: "/battery-graph", label: "Battery", end: false },
  { to: "/bstats", label: "Statistics", end: false },
  { to: "/results", label: "Model", end: false },
  { to: "/datatags", label: "Tags", end: false },
];

export default function Layout() {
  const location = useLocation();
  const isHome = location.pathname === "/";

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-zinc-800/70 bg-zinc-950/70 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 md:px-8">
          <div className="flex items-center gap-2.5">
            <span className="text-sm font-semibold tracking-tight text-zinc-100">
              ubr004
            </span>
            <span className="hidden text-xs text-emerald-400 sm:inline">
              Pebble telemetry
            </span>
          </div>

          {/* Nav */}
          <nav className="ml-auto flex items-center gap-1 overflow-x-auto">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) =>
                  `whitespace-nowrap rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-emerald-400/10 text-emerald-300"
                      : "text-zinc-400 hover:bg-zinc-800/60 hover:text-zinc-100"
                  }`
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main
        className={
          isHome
            ? "w-full max-w-none px-4 py-8 md:px-8 md:py-12"
            : "mx-auto max-w-6xl px-4 py-8 md:px-8 md:py-12"
        }
      >
        <Outlet />
      </main>
    </div>
  );
}
