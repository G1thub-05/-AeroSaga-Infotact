import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./css/global.css";
import "./css/TopBar.css";
import "./css/MetricsBar.css";
import "./css/Layout.css";
import "./css/FleetPanel.css";
import "./css/MapPanel.css";
import "./css/DroneDetails.css";
import "./css/AppFooter.css";
import "./css/responsive.css";

createRoot(document.getElementById("root")).render(
	<React.StrictMode>
		<App />
	</React.StrictMode>,
);
