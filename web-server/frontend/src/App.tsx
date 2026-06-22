import { Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import Home from "./pages/Home";
import Datatags from "./pages/Datatags";
import BatteryGraph from "./pages/BatteryGraph";
import BatchStats from "./pages/BatchStats";
import Results from "./pages/Results";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/datatags" element={<Datatags />} />
        <Route path="/battery-graph" element={<BatteryGraph />} />
        <Route path="/bstats" element={<BatchStats />} />
        <Route path="/results" element={<Results />} />
      </Route>
    </Routes>
  );
}
