import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
	Activity,
	Battery,
	CircleAlert,
	CloudLightning,
	ChevronRight,
	Crosshair,
	MapPin,
	Map,
	Mountain,
	Navigation,
	Pause,
	Play,
	Radio,
	Search,
	Satellite,
	Signal,
	Wifi,
	WifiOff,
	X,
} from "lucide-react";
import Globe from "./Globe.jsx";
import Metric from "./components/Metric.jsx";
import WorkflowStep from "./components/WorkflowStep.jsx";
import DroneIcon from "./components/DroneIcon.jsx";
import RefreshButton from "./components/RefreshButton.jsx";
import DroneNavigator from "./components/DroneNavigator.jsx";
import TopBar from "./components/TopBar.jsx";
import AppFooter from "./components/AppFooter.jsx";
import { api } from "./api/client.js";

const label = (value) => (value || "IDLE").replaceAll("_", " ");
const statusClass = (d) =>
	!d.online || d.status === "PAUSED"
		? "amber"
		: ["ACTIVE", "RETURNING"].includes(d.status)
			? "mint"
			: d.status === "COMPLETED"
				? "blue"
				: d.status === "FAILED_SAFE"
					? "red"
					: "muted";

export default function App() {
	const [drones, setDrones] = useState([]),
		[selectedId, setSelectedId] = useState("drone-01");
	const [connected, setConnected] = useState(false),
		[query, setQuery] = useState(""),
		[notice, setNotice] = useState(""),
		[missionDroneId, setMissionDroneId] = useState(""),
		[backendMode, setBackendMode] = useState("CONNECTING");
	const [destination, setDestination] = useState({ lat: 21.27, lon: 81.65 }),
		[source, setSource] = useState({ lat: "", lon: "" });
	const [sourceName, setSourceName] = useState(""),
		[destinationName, setDestinationName] = useState(""),
		[busy, setBusy] = useState(false);
	const [simulateDropFailure, setSimulateDropFailure] = useState(false);
	const [events, setEvents] = useState([]),
		[missionId, setMissionId] = useState("");
	const [tracks, setTracks] = useState({});
	const [refreshing, setRefreshing] = useState(false);
	const [mapStyle, setMapStyle] = useState("satellite");

	const [weather, setWeather] = useState(null);
	const [weatherLoading, setWeatherLoading] = useState(false);

	const updateDrone = useCallback(
		(d) => setDrones((prev) => prev.map((old) => (old.id === d.id ? d : old))),
		[],
	);
	useEffect(() => {
		api
			.getFleet()
			.then((r) => {
				if (!r.ok) throw Error();
				return r.json();
			})
			.then(setDrones)
			.catch(() =>
				setNotice("Backend is offline. Start the Java API to connect."),
			);
		api
			.getHealth()
			.then((r) => r.json())
			.then((data) => setBackendMode(data.mode || "TEMPORAL"))
			.catch(() => {});
		let socket,
			retry,
			stopped = false;
		const connect = () => {
			if (stopped) return;
			socket = new WebSocket(api.fleetSocketUrl);
			socket.onopen = () => setConnected(true);
			socket.onclose = () => {
				setConnected(false);
				if (!stopped) retry = setTimeout(connect, 2200);
			};
			socket.onerror = () => socket.close();
			socket.onmessage = (e) => {
				const data = JSON.parse(e.data);
				if (data.type === "snapshot") setDrones(data.drones);
				if (data.drone) {
					updateDrone(data.drone);
					if (data.mission) {
						const m = data.mission;
						setEvents((old) =>
							[{ ...m, at: new Date().toLocaleTimeString() }, ...old].slice(
								0,
								30,
							),
						);
						setTracks((old) => {
							const previous = old[data.drone.id];
							const current =
								previous?.missionId === m.missionId
									? previous
									: {
											missionId: m.missionId,
											start: { latitude: m.latitude, longitude: m.longitude },
											destination: null,
											outbound: [],
											returning: [],
										};
							const fix = { latitude: m.latitude, longitude: m.longitude };
							const append = (points) =>
								points.length &&
								points.at(-1).latitude === fix.latitude &&
								points.at(-1).longitude === fix.longitude
									? points
									: [...points, fix];
							return {
								...old,
								[data.drone.id]: {
									...current,
									location: m,
									outbound:
										m.currentStep === "NAVIGATE"
											? append(current.outbound)
											: current.outbound,
									returning:
										m.currentStep === "RETURN_TO_BASE" &&
										["RETURNING", "ABORTED", "FAILED_SAFE"].includes(m.status)
											? append(current.returning)
											: current.returning,
								},
							};
						});
					}
				}
			};
		};
		connect();
		return () => {
			stopped = true;
			clearTimeout(retry);
			socket?.close();
		};
	}, [updateDrone]);
	useEffect(() => {
		if (connected) return;
		const refreshFleet = () =>
			api
				.getFleet()
				.then((r) => (r.ok ? r.json() : Promise.reject()))
				.then(setDrones)
				.catch(() => {});
		refreshFleet();
		const timer = setInterval(refreshFleet, 2500);
		return () => clearInterval(timer);
	}, [connected]);
	const selected = drones.find((d) => d.id === selectedId) || drones[0];
	const selectedRef = useRef(selected);
	selectedRef.current = selected;

	useEffect(() => {
		let cancelled = false;
		async function loadWeather() {
			const drone = selectedRef.current;
			if (!drone) return;
			setWeatherLoading(true);
			try {
				const response = await api.getWeather(drone.latitude, drone.longitude);
				if (!response.ok) throw new Error("Weather unavailable");
				const data = await response.json();
				if (!cancelled) setWeather(data);
			} catch {
				if (!cancelled) setWeather(null);
			} finally {
				if (!cancelled) setWeatherLoading(false);
			}
		}

		loadWeather();
		const timer = setInterval(loadWeather, 5 * 60 * 1000);

		return () => {
			cancelled = true;
			clearInterval(timer);
		};
	}, [selected?.latitude, selected?.longitude]);

	useEffect(() => setSource({ lat: "", lon: "" }), [selectedId]);
	const selectedTrack = selected ? tracks[selected.id] : null;
	const selectedMissionId =
		selected?.missionId || (missionDroneId === selected?.id ? missionId : "");
	const filtered = useMemo(
		() =>
			drones.filter((d) =>
				`${d.name} ${d.id} ${d.status}`
					.toLowerCase()
					.includes(query.toLowerCase()),
			),
		[drones, query],
	);
	const activeCount = drones.filter((d) =>
		["ACTIVE", "PAUSED", "RETURNING"].includes(d.status),
	).length;
	const avgBattery = drones.length
		? Math.round(drones.reduce((a, d) => a + d.battery, 0) / drones.length)
		: 0;
	async function action(path, body) {
		setBusy(true);
		setNotice("");
		try {
			const response = await api.post(path, body);
			const data = await response.json().catch(() => ({}));
			if (!response.ok) throw Error(data.message || "Request failed");
			return data;
		} catch (e) {
			setNotice(e.message);
			return null;
		} finally {
			setBusy(false);
		}
	}
	async function lookupPlace(which) {
		const query = (which === "source" ? sourceName : destinationName).trim();
		if (!query) {
			setNotice("Place ka naam enter karein.");
			return;
		}
		setBusy(true);
		setNotice("Place search ho raha hai…");
		try {
			const response = await api.searchPlace(query);
			if (!response.ok) throw Error("Place search service unavailable");
			const results = await response.json();
			if (!results.length)
				throw Error(
					"Place nahi mila. Naam thoda aur specific karke try karein.",
				);
			const place = results[0],
				point = { lat: Number(place.lat), lon: Number(place.lon) };
			if (which === "source") setSource(point);
			else setDestination(point);
			setNotice(`${place.display_name} ke coordinates mil gaye.`);
		} catch (e) {
			setNotice(e.message || "Place search failed.");
		} finally {
			setBusy(false);
		}
	}
	async function dispatch() {
		if (!selected) return;
		const start = {
				latitude: Number(source.lat === "" ? selected.latitude : source.lat),
				longitude: Number(source.lon === "" ? selected.longitude : source.lon),
			},
			target = {
				latitude: Number(destination.lat),
				longitude: Number(destination.lon),
			};
		if (
			![
				start.latitude,
				start.longitude,
				target.latitude,
				target.longitude,
			].every(Number.isFinite)
		) {
			setNotice(
				"Source aur destination ke valid coordinates enter karein ya place name search karein.",
			);
			return;
		}
		const result = await action("/api/missions", {
			droneId: selected.id,
			sourceLat: start.latitude,
			sourceLon: start.longitude,
			destinationLat: target.latitude,
			destinationLon: target.longitude,
			simulateDropFailure,
		});
		if (result) {
			setMissionId(result.missionId);
			setMissionDroneId(selected.id);
			setTracks((old) => ({
				...old,
				[selected.id]: {
					missionId: result.missionId,
					start,
					destination: target,
					outbound: [start],
					returning: [target],
					location: null,
				},
			}));
			setNotice(`Mission ${result.missionId} launched`);
		}
	}
	async function setOnline(online) {
		if (!selected) return;
		const d = await action(
			`/api/drones/${selected.id}/${online ? "reconnect" : "offline"}`,
		);
		if (d) updateDrone(d);
	}
	function focusDrone(drone) {
		if (!drone) return;
		setSelectedId(drone.id);
		window.dispatchEvent(new CustomEvent("focus-drone", { detail: drone }));
	}
	function navigateDrone(direction) {
		if (drones.length < 2 || !selected) return;
		const currentIndex = drones.findIndex((drone) => drone.id === selected.id);
		const nextIndex =
			(currentIndex + direction + drones.length) % drones.length;
		focusDrone(drones[nextIndex]);
	}
	async function refreshFleet() {
		const startedAt = Date.now();
		const controller = new AbortController();
		const requestTimeout = setTimeout(() => controller.abort(), 5000);
		setRefreshing(true);
		setNotice("Refreshing fleet data…");
		try {
			const response = await api.getFleet({ signal: controller.signal });
			if (!response.ok) throw new Error();
			const latestFleet = await response.json();
			setDrones(latestFleet);
			setNotice(`Fleet refreshed · ${latestFleet.length} drones`);
		} catch {
			setNotice("Fleet refresh failed. Check that the backend is running.");
		} finally {
			clearTimeout(requestTimeout);
			const remainingSpin = Math.max(0, 650 - (Date.now() - startedAt));
			if (remainingSpin) {
				await new Promise((resolve) => setTimeout(resolve, remainingSpin));
			}
			setRefreshing(false);
		}
	}
	async function abort() {
		const activeMission =
			selected?.missionId || (missionDroneId === selected?.id ? missionId : "");
		if (!activeMission) return;
		const result = await action(`/api/missions/${activeMission}/abort`);
		if (result) setNotice("Emergency return requested");
	}
	return (
		<main className="app-shell">
			<TopBar
				connected={connected}
				backendMode={backendMode}
				noticeActive={Boolean(notice)}
			/>
			<section className="metrics">
				<Metric
					icon={<DroneIcon />}
					name="FLEET SIZE"
					value={String(drones.length).padStart(2, "0")}
					suffix="DRONES"
				/>
				<Metric
					icon={<Activity />}
					name="MISSIONS ACTIVE"
					value={String(activeCount).padStart(2, "0")}
					suffix="IN FLIGHT"
					accent="mint"
				/>
				<Metric
					icon={<Radio />}
					name="TELEMETRY LINK"
					value={connected ? "STABLE" : "OFFLINE"}
					suffix={connected ? "42 MS" : "—"}
					accent={connected ? "mint" : "amber"}
				/>
				<Metric
					icon={<Battery />}
					name="AVG. BATTERY"
					value={`${avgBattery}%`}
					suffix="FLEET AVG."
					accent="blue"
				/>
				<div className="metric weather">
					<div className="metric-icon amber">
						<CloudLightning size={17} />
					</div>
					<div>
						<span className="metric-label">WEATHER ALERT</span>
						{/* <strong>LIGHT RAIN</strong> */}
						{/* <small>VISIBILITY 8.2 KM</small> */}
						<strong>
							{weatherLoading
								? "LOADING"
								: (weather?.condition?.toUpperCase() ?? "UNAVAILABLE")}
						</strong>
						<small>
							{weather
								? `VISIBILITY ${weather.visibilityKm.toFixed(1)} KM · WIND ${weather.windSpeedKmh} KM/H`
								: "WEATHER DATA UNAVAILABLE"}
						</small>
					</div>
					{/* <span className="weather-pill">MONITORING</span> */}
					<span className="weather-pill">
						{weather?.temperatureC != null ? `${weather.temperatureC}°C` : "—"}
					</span>
				</div>
			</section>
			<div className="workspace">
				<aside className="fleet-panel">
					<div className="panel-heading">
						<div>
							<span className="eyebrow">OPERATIONS</span>
							<h2>
								Fleet overview <span>{drones.length}</span>
							</h2>
						</div>
						<RefreshButton refreshing={refreshing} onClick={refreshFleet} />
					</div>
					<div className="search">
						<Search size={15} />
						<input
							value={query}
							onChange={(e) => setQuery(e.target.value)}
							placeholder="Search fleet..."
						/>
						<kbd>⌘ K</kbd>
					</div>
					<div className="fleet-tabs">
						<span className="active">
							ALL UNITS <b>{drones.length}</b>
						</span>
						<span>
							ACTIVE <b>{activeCount}</b>
						</span>
					</div>
					<div className="drone-list">
						{filtered.map((d) => (
							<button
								className={`drone-row ${selected?.id === d.id ? "selected" : ""}`}
								key={d.id}
								onClick={() => setSelectedId(d.id)}
							>
								<div className={`drone-symbol ${statusClass(d)}`}>
									<DroneIcon size={16} />
								</div>
								<div className="drone-info">
									<strong>
										{d.name}
										<small>{d.id.toUpperCase()}</small>
									</strong>
									<span className={`status ${statusClass(d)}`}>
										<i />
										{label(d.status)}
									</span>
								</div>
								<div className="drone-battery">
									<Battery size={13} />
									{d.battery}%<ChevronRight size={13} />
								</div>
							</button>
						))}
					</div>
					<div className="fleet-footer">
						<span>
							<span className="legend-dot mint" /> In flight
						</span>
						<span>
							<span className="legend-dot amber" /> Paused / Offline
						</span>
						<span>
							<span className="legend-dot muted" /> Standby
						</span>
					</div>
				</aside>
				<section className="map-section">
					<div className="map-top">
						<div className="map-title">
							<span className="eyebrow">RAIPUR, CHHATTISGARH · LIVE VIEW</span>
							<h2>
								Fleet airspace{" "}
								<span className="map-live">
									<i /> LIVE
								</span>
							</h2>
						</div>
						<div className="map-tools">
							<span>
								<span className="legend-dot mint" /> OUTBOUND
							</span>
							<span>
								<span className="legend-dot amber" /> RETURN / HOLD
							</span>
							<span className="route-legend">
								<i className="outbound-line" /> OUT
							</span>
							<span className="route-legend">
								<i className="return-line" /> RETURN
							</span>
							<button
								className="map-tool locate-button"
								disabled={!selected}
								onClick={() =>
									selected &&
									window.dispatchEvent(
										new CustomEvent("focus-drone", { detail: selected }),
									)
								}
								title={
									selected
										? `Focus map on ${selected.name}`
										: "Select a drone first"
								}
							>
								<Crosshair size={15} strokeWidth={2.2} />
								<span>FOCUS DRONE</span>
							</button>
						</div>
					</div>
					<div className="map-frame">
						<Globe
							drones={drones}
							selectedId={selected?.id}
							onSelect={setSelectedId}
							track={selectedTrack}
							mapStyle={mapStyle}
						/>
						<div className="basemap-switch" role="group" aria-label="Map style">
							<button
								className={mapStyle === "satellite" ? "active" : ""}
								onClick={() => setMapStyle("satellite")}
								aria-pressed={mapStyle === "satellite"}
							>
								<Satellite size={14} /> Satellite
							</button>
							<button
								className={mapStyle === "terrain" ? "active" : ""}
								onClick={() => setMapStyle("terrain")}
								aria-pressed={mapStyle === "terrain"}
							>
								<Mountain size={14} /> Terrain
							</button>
							<button
								className={mapStyle === "street" ? "active" : ""}
								onClick={() => setMapStyle("street")}
								aria-pressed={mapStyle === "street"}
							>
								<Map size={14} /> Street
							</button>
						</div>
						<div className="map-coordinates">
							<span>
								<MapPin size={12} /> 21.2514° N&nbsp; 81.6296° E
							</span>
							<span>ALTITUDE 0 — 15 KM</span>
						</div>
						<div className="map-scale">
							<span />
							10 KM
						</div>
						{selected && (
							<div className="selected-card">
								<div className={`drone-symbol ${statusClass(selected)}`}>
									<DroneIcon size={17} />
								</div>
								<div>
									<strong>{selected.name}</strong>
									<span>
										<i className={`legend-dot ${statusClass(selected)}`} />
										{label(selected.status)} · {selected.currentStep}
									</span>
								</div>
								<button onClick={() => setSelectedId(null)}>
									<X size={14} />
								</button>
							</div>
						)}
					</div>
					<div className="map-bottom">
						<span>
							<Signal size={14} /> {mapStyle.toUpperCase()} MAP · 3D GLOBE
						</span>
						<span>
							<span className="legend-dot mint" />{" "}
							{connected ? "STREAMING TELEMETRY" : "WAITING FOR BACKEND"}
						</span>
						<span>LAST SYNC {events[0]?.at || "—"}</span>
					</div>
				</section>
				<aside className="detail-panel">
					{selected ? (
						<>
							<div className="detail-heading">
								<div>
									<span className="eyebrow">UNIT PROFILE</span>
									<h2>
										{selected.name}
										<span className={`status ${statusClass(selected)}`}>
											<i />
											{label(selected.status)}
										</span>
									</h2>
								</div>
								<DroneNavigator
									disabled={drones.length < 2}
									onPrevious={() => navigateDrone(-1)}
									onNext={() => navigateDrone(1)}
								/>
							</div>
							<div className="unit-id">
								<DroneIcon size={15} /> {selected.id.toUpperCase()}{" "}
								<span>·</span> AUTONOMOUS DELIVERY
							</div>
							<div className="detail-stats">
								<div>
									<span>BATTERY</span>
									<strong className={selected.battery < 25 ? "amber-text" : ""}>
										<Battery size={15} />
										{selected.battery}%
									</strong>
									<div className="battery-track">
										<i style={{ width: `${selected.battery}%` }} />
									</div>
								</div>
								<div>
									<span>LINK QUALITY</span>
									<strong>
										<Signal size={15} />
										{selected.online ? "98%" : "—"}
									</strong>
									<small>{selected.online ? "LOW LATENCY" : "NO SIGNAL"}</small>
								</div>
							</div>
							<div className="section-label">
								MISSION PARAMETERS <span>EDIT</span>
							</div>
							<label className="field-label">
								SOURCE ·{" "}
								{source.lat === ""
									? "SELECTED DRONE LOCATION"
									: "CUSTOM LOCATION"}
							</label>
							<div className="place-search">
								<input
									value={sourceName}
									onChange={(e) => setSourceName(e.target.value)}
									placeholder="Source place name (e.g. Raipur Airport)"
								/>
								<button
									type="button"
									onClick={() => lookupPlace("source")}
									disabled={busy}
								>
									FIND
								</button>
							</div>
							<div className="coordinate-fields">
								<label>
									<span>SOURCE LATITUDE</span>
									<input
										type="number"
										step="0.0001"
										value={
											source.lat === ""
												? (selected?.latitude ?? "")
												: source.lat
										}
										onChange={(e) =>
											setSource({ ...source, lat: e.target.value })
										}
									/>
								</label>
								<label>
									<span>SOURCE LONGITUDE</span>
									<input
										type="number"
										step="0.0001"
										value={
											source.lon === ""
												? (selected?.longitude ?? "")
												: source.lon
										}
										onChange={(e) =>
											setSource({ ...source, lon: e.target.value })
										}
									/>
								</label>
							</div>
							<label className="field-label destination-label">
								DESTINATION
							</label>
							<div className="place-search">
								<input
									value={destinationName}
									onChange={(e) => setDestinationName(e.target.value)}
									placeholder="Destination place name (e.g. Raipur AIIMS)"
								/>
								<button
									type="button"
									onClick={() => lookupPlace("destination")}
									disabled={busy}
								>
									FIND
								</button>
							</div>
							<div className="coordinate-fields">
								<label>
									<span>DESTINATION LATITUDE</span>
									<input
										type="number"
										step="0.0001"
										value={destination.lat}
										onChange={(e) =>
											setDestination({ ...destination, lat: e.target.value })
										}
									/>
								</label>
								<label>
									<span>DESTINATION LONGITUDE</span>
									<input
										type="number"
										step="0.0001"
										value={destination.lon}
										onChange={(e) =>
											setDestination({ ...destination, lon: e.target.value })
										}
									/>
								</label>
							</div>
							<label className="failure-toggle">
								<input
									type="checkbox"
									checked={simulateDropFailure}
									onChange={(e) => setSimulateDropFailure(e.target.checked)}
								/>{" "}
								Simulate drop activity failure (test compensation)
							</label>
							<button
								className="dispatch-button"
								disabled={
									busy ||
									!selected.online ||
									["ACTIVE", "PAUSED", "RETURNING"].includes(selected.status)
								}
								onClick={dispatch}
							>
								<Play size={15} fill="currentColor" /> DISPATCH MISSION
							</button>
							<button
								className="abort-button"
								disabled={
									!selectedMissionId ||
									busy ||
									!["ACTIVE", "PAUSED", "RETURNING"].includes(selected.status)
								}
								onClick={abort}
							>
								<CircleAlert size={15} /> EMERGENCY ABORT
							</button>
							<div className="connect-actions">
								<button
									onClick={() => setOnline(false)}
									disabled={!selected.online || busy}
								>
									<WifiOff size={14} /> SIMULATE SIGNAL LOSS
								</button>
								<button
									onClick={() => setOnline(true)}
									disabled={selected.online || busy}
								>
									<Wifi size={14} /> RECONNECT DRONE
								</button>
							</div>
							<div className="mission-heading">
								<span className="eyebrow">MISSION WORKFLOW</span>
								{selectedMissionId &&
									["ACTIVE", "PAUSED", "RETURNING"].includes(
										selected.status,
									) && (
										<button
											onClick={() =>
												api
													.getMission(selectedMissionId)
													.then((r) => r.json())
													.then((x) =>
														setNotice(`${x.currentStep}: ${x.message}`),
													)
											}
										>
											VIEW
										</button>
									)}
							</div>
							<div className="workflow">
								<WorkflowStep
									name="Takeoff"
									current={selected.currentStep === "TAKEOFF"}
									done={[
										"NAVIGATE",
										"DROP_PACKAGE",
										"RETURN_TO_BASE",
										"COMPLETED",
									].includes(selected.currentStep)}
								/>
								<WorkflowStep
									name="Navigate"
									current={selected.currentStep === "NAVIGATE"}
									done={[
										"DROP_PACKAGE",
										"RETURN_TO_BASE",
										"COMPLETED",
									].includes(selected.currentStep)}
								/>
								<WorkflowStep
									name="Drop package"
									current={selected.currentStep === "DROP_PACKAGE"}
									done={["RETURN_TO_BASE", "COMPLETED"].includes(
										selected.currentStep,
									)}
								/>
								<WorkflowStep
									name="Return to base"
									current={selected.currentStep === "RETURN_TO_BASE"}
									done={selected.status === "COMPLETED"}
								/>
							</div>
							<div className="message-box">
								<span>WORKFLOW STATUS</span>
								<p>
									{selected.message ||
										"Select a drone and dispatch a delivery mission."}
								</p>
								{selectedMissionId && <small>{selectedMissionId}</small>}
							</div>
						</>
					) : (
						<div className="empty-detail">
							Select a drone to view its mission details.
						</div>
					)}
				</aside>
			</div>
			<AppFooter backendMode={backendMode} />
			{notice && (
				<button
					className={`toast ${notice.toLowerCase().includes("offline") || notice.toLowerCase().includes("failed") ? "toast--warning" : "toast--success"}`}
					role="status"
					aria-live="polite"
					onClick={() => setNotice("")}
				>
					{notice}
					<X size={14} />
				</button>
			)}
		</main>
	);
}
