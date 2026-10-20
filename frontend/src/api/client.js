const API_BASE = (
	import.meta.env.VITE_API_URL || "http://localhost:8080"
).replace(/\/$/, "");
const request = (path, options) => fetch(`${API_BASE}${path}`, options);

export const api = {
	fleetSocketUrl: API_BASE.replace(/^http/, "ws") + "/ws/fleet",
	getFleet: (options) => request("/api/fleet", options),
	getHealth: () => request("/api/health"),
	getMission: (missionId) =>
		request(`/api/missions/${encodeURIComponent(missionId)}`),
	post: (path, body) =>
		request(path, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: body === undefined ? undefined : JSON.stringify(body),
		}),
	searchPlace: (place) =>
		fetch(
			`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(place)}`,
			{
				headers: { "Accept-Language": "en" },
			},
		),
};
