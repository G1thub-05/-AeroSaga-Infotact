export default function AppFooter({ backendMode }) {
	return (
		<footer className="footer">
			<span>
				AEROSAGA <b>·</b> AUTONOMOUS FLEET ORCHESTRATION
			</span>
			<span>
				<span className="legend-dot mint" /> SYSTEM OPERATIONAL
			</span>
			<span>
				BUILD 1.0.0 <b>·</b>{" "}
				{backendMode === "TEMPORAL" ? "TEMPORAL WORKFLOWS" : "LOCAL SIMULATION"}
			</span>
		</footer>
	);
}