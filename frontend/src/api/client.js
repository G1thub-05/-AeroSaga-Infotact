const API_BASE = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");

// Combines the API base URL with the provided endpoint path.
const request = (path, options) => fetch(`${API_BASE}${path}`, options);

export const api = {
	// WebSocket URL for receiving real-time fleet updates and Converts HTTP to WS and HTTPS to WSS: http://localhost:8080  - ws://localhost:8080/ws/fleet
	fleetSocketUrl: API_BASE.replace(/^http/, "ws") + "/ws/fleet",

	// Fetches fleet information from the backend.
	getFleet: (options) => request("/api/fleet", options),

	// Checks the backend health status.
	getHealth: () => request("/api/health"),

	// Fetches mission details using the provided mission ID.
	getMission: (missionId) =>
		  						request(`/api/missions/${encodeURIComponent(missionId)}`),
								post: (path, body) => request(path, {
															method: "POST",
															headers: { "Content-Type": "application/json" },
															body: body === undefined ? undefined : JSON.stringify(body),
														}),

	// Searches for a place using the OpenStreetMap Nominatim API.
	searchPlace: (place) =>
		fetch(
			`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(place)}`,
			{
				headers: { "Accept-Language": "en" },
			},
		),
};
