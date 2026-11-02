import { useEffect, useRef } from "react";
import {
	Cartesian3,
	Color,
	createWorldTerrainAsync,
	EllipsoidTerrainProvider,
	Ion,
	PolylineArrowMaterialProperty,
	UrlTemplateImageryProvider,
	VerticalOrigin,
	Viewer,
} from "cesium";

import "cesium/Build/Cesium/Widgets/widgets.css";

export default function Globe({
	drones,
	selectedId,
	onSelect,
	track,
	mapStyle = "satellite",
}) {
	const element = useRef(null),
		viewer = useRef(null),
		entities = useRef(new Map()),
		routeEntities = useRef(new Map());

	useEffect(() => {
		let active = true;
		const token = import.meta.env.VITE_CESIUM_ION_TOKEN;

		if (token) Ion.defaultAccessToken = token;
		const instance = new Viewer(element.current, {
			animation: false,
			timeline: false,

			// MULTI MAP CONTROLS
			baseLayerPicker: false,
			geocoder: false,
			homeButton: false,
			sceneModePicker: false,
			navigationHelpButton: false,

			fullscreenButton: false,
			infoBox: false,
			selectionIndicator: false,
			terrainProvider: new EllipsoidTerrainProvider(),
			baseLayer: false,
			scene3DOnly: true,
			shouldAnimate: true,
		});

		viewer.current = instance;

		instance.scene.globe.baseColor = Color.fromCssColorString("#0b1724");
		instance.scene.backgroundColor = Color.fromCssColorString("#08121d");
		instance.camera.setView({
			destination: Cartesian3.fromDegrees(81.6296, 21.2514, 42000),
		});

		instance.screenSpaceEventHandler.setInputAction((movement) => {
			const hit = instance.scene.pick(movement.position);
			if (hit?.id?.properties?.droneId)
				onSelect(hit.id.properties.droneId.getValue());
		}, 1);

		if (token)
			createWorldTerrainAsync()
				.then((terrain) => {
					if (active && !instance.isDestroyed())
						instance.terrainProvider = terrain;
				})
				.catch((error) => {
					console.error("Cesium terrain loading failed:", error);
				});

		return () => {
			active = false;
			instance.destroy();
			viewer.current = null;
			entities.current.clear();
			routeEntities.current.clear();
		};
	}, []);

	useEffect(() => {
		const v = viewer.current;
		if (!v || v.isDestroyed()) return;
		v.imageryLayers.removeAll();

		const add = (url, credit) => {
			const provider = new UrlTemplateImageryProvider({
				url,
				credit,
				maximumLevel: 19,
			});

			provider.errorEvent.addEventListener((error) => {
				console.error("Imagery tile error:", error);
			});

			const layer = v.imageryLayers.addImageryProvider(provider);
			console.log("Added layer:", url);
			console.log("Current layer count:", v.imageryLayers.length);
			return layer;
		};

		if (mapStyle === "satellite") {
			add(
				"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
				"Tiles © Esri",
			);
			add(
				"https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}",
				"Labels © Esri",
			);
		} else if (mapStyle === "terrain") {
			add(
				"https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}",
				"Tiles © Esri",
			);
		} else {
			add(
				"https://tile.openstreetmap.org/{z}/{x}/{y}.png",
				"© OpenStreetMap contributors",
			);
		}

		console.log("Final imagery layer count:", v.imageryLayers.length);
	}, [mapStyle]);

	useEffect(() => {
		const focusDrone = (event) => {
			const v = viewer.current,
				drone = event.detail;
			if (v && drone)
				v.camera.flyTo({
					destination: Cartesian3.fromDegrees(
						drone.longitude,
						drone.latitude,
						9000,
					),
					duration: 1.2,
				});
		};
		window.addEventListener("focus-drone", focusDrone);
		return () => window.removeEventListener("focus-drone", focusDrone);
	}, []);

	useEffect(() => {
		const v = viewer.current;
		if (!v) return;
		const keep = new Set();
		for (const drone of drones) {
			keep.add(drone.id);
			const color =
				drone.status === "FAILED_SAFE"
					? "#f77b7b"
					: drone.status === "PAUSED" || !drone.online
						? "#f5a742"
						: drone.status === "RETURNING" ||
							  drone.currentStep === "RETURN_TO_BASE"
							? "#f5ad4f"
							: drone.status === "ACTIVE"
								? "#42e0bd"
								: drone.status === "COMPLETED"
									? "#8ca6ff"
									: "#8293a7";
			const existing = entities.current.get(drone.id);
			if (existing) {
				existing.position = Cartesian3.fromDegrees(
					drone.longitude,
					drone.latitude,
					420 + (drone.status === "ACTIVE" ? 140 : 0),
				);
				existing.billboard.image = droneSvg(color, drone.id === selectedId);
				existing.billboard.width = drone.id === selectedId ? 52 : 42;
				existing.billboard.height = drone.id === selectedId ? 52 : 42;
				existing.label.text = drone.id === selectedId ? drone.name : "";
			} else {
				const entity = v.entities.add({
					id: drone.id,
					position: Cartesian3.fromDegrees(
						drone.longitude,
						drone.latitude,
						420,
					),
					billboard: {
						image: droneSvg(color, drone.id === selectedId),
						width: drone.id === selectedId ? 52 : 42,
						height: drone.id === selectedId ? 52 : 42,
						verticalOrigin: VerticalOrigin.CENTER,
						disableDepthTestDistance: Number.POSITIVE_INFINITY,
					},
					label: {
						text: "",
						font: "bold 13px Inter, sans-serif",
						fillColor: Color.WHITE,
						outlineColor: Color.BLACK,
						outlineWidth: 3,
						style: 2,
						pixelOffset: new Cartesian3(0, -29, 0),
						disableDepthTestDistance: Number.POSITIVE_INFINITY,
					},
					properties: { droneId: drone.id },
				});
				entities.current.set(drone.id, entity);
			}
		}
		for (const [id, entity] of entities.current)
			if (!keep.has(id)) {
				v.entities.remove(entity);
				entities.current.delete(id);
			}
	}, [drones, selectedId]);
	useEffect(() => {
		const v = viewer.current;
		if (!v) return;
		for (const [missionId, routes] of routeEntities.current)
			if (missionId !== track?.missionId) {
				v.entities.remove(routes.outward);
				v.entities.remove(routes.home);
				routeEntities.current.delete(missionId);
			}
		if (!track?.start || !track?.destination) return;
		const outbound =
			track.outbound?.length > 1
				? track.outbound
				: [track.start, track.destination];
		const inbound =
			track.returning?.length > 1
				? track.returning
				: [track.destination, track.start];
		const positions = (points, height) =>
			points.map((p) =>
				Cartesian3.fromDegrees(p.longitude, p.latitude, height),
			);
		let routes = routeEntities.current.get(track.missionId);
		if (!routes) {
			const outward = v.entities.add({
				id: `${track.missionId}-outbound`,
				polyline: {
					positions: positions(outbound, 260),
					width: 4,
					material: new PolylineArrowMaterialProperty(
						Color.fromCssColorString("#42e0bd"),
					),
				},
			});
			const home = v.entities.add({
				id: `${track.missionId}-return`,
				polyline: {
					positions: positions(inbound, 430),
					width: 5,
					material: new PolylineArrowMaterialProperty(
						Color.fromCssColorString("#f5ad4f"),
					),
				},
			});
			routes = { outward, home };
			routeEntities.current.set(track.missionId, routes);
		} else {
			routes.outward.polyline.positions = positions(outbound, 260);
			routes.home.polyline.positions = positions(inbound, 430);
		}
	}, [track]);
	const selectedDrone = drones.find((drone) => drone.id === selectedId);
	const liveFix = track?.location || selectedDrone;
	return (
		<div className="globe" ref={element}>
			<div className="live-position">
				<i />
				<div>
					<span>LIVE GPS · {selectedDrone?.name || "FLEET"}</span>
					<strong>
						{liveFix
							? `${liveFix.latitude.toFixed(5)}° N  ${liveFix.longitude.toFixed(5)}° E`
							: "Waiting for telemetry"}
					</strong>

					<small>
						{liveFix?.updatedAt
							? new Date(liveFix.updatedAt).toLocaleTimeString()
							: "LIVE TELEMETRY"}
					</small>
				</div>
			</div>
		</div>
	);
}

function droneSvg(color, selected) {
	const outline = color;
	const halo = selected
		? '<circle cx="48" cy="48" r="43" fill="#07131dcc" stroke="#ffe082" stroke-width="2.5"/><circle cx="48" cy="48" r="37" fill="none" stroke="#ffffff55" stroke-width="1" stroke-dasharray="2 5"/>'
		: '<circle cx="48" cy="48" r="41" fill="#07131dcc" stroke="#ffffff30" stroke-width="1.5"/>';

	const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">${halo}<g fill="none" stroke="${outline}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"><path d="m37 37-17-17m39 17 17-17m-39 39L20 76m39-17 17 17"/><circle cx="18" cy="18" r="10"/><circle cx="78" cy="18" r="10"/><circle cx="18" cy="78" r="10"/><circle cx="78" cy="78" r="10"/><path d="M36 39 42 33h12l6 6v18l-6 6H42l-6-6z" fill="#0b1a27"/><path d="M42 63v5m12-5v5" stroke-width="3"/></g><path d="M45 43h6l3 5-3 5h-6l-3-5z" fill="${color}"/><circle cx="48" cy="48" r="2" fill="#f5fff9"/></svg>`;

	return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}