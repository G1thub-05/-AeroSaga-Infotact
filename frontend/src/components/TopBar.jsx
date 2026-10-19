import { Crosshair } from "lucide-react";
export default function TopBar({ connected, backendMode, noticeActive }) {
	return (
		<header className={`topbar${noticeActive ? " topbar--notice" : ""}`}>
			<a className="brand" href="#">
				<span className="brand-mark">
					<Crosshair size={20} />
				</span>
				<span>
					AEROSAGA<small>MISSION CONTROL</small>
				</span>
			</a>
			<div className="top-status">
				<span className={`live-dot ${connected ? "" : "offline"}`} />
				<span>{connected ? "LIVE TELEMETRY" : "RECONNECTING"}</span>
				<i />
				<span className="cluster">
					<span />{" "}
					{backendMode === "TEMPORAL" ? "TEMPORAL CLUSTER" : "LOCAL DEMO"}{" "}
					<b>
						{backendMode === "TEMPORAL"
							? connected
								? "CONNECTED"
								: "WAITING"
							: "NON-DURABLE"}
					</b>
				</span>
			</div>
			<div className="operator">
				<div className="avatar">OP</div>
				<span>
					OPS CONTROL<small>SHIFT A · RAIPUR OPS</small>
				</span>
			</div>
		</header>
	);
}