
## \# AeroSaga — Temporal.io Drone Mission Control

AeroSaga is a full-stack demo for durable autonomous drone missions. Temporal stores and runs the Java workflow, Spring Boot provides the REST and WebSocket APIs, and the React/CesiumJS frontend shows a 50-drone fleet on a 3D globe.

### \# Features

- Drone mission workflow: \*\*Takeoff → Navigate → Drop Package → Return to Base\*\*.
- Durable pause and resume when a drone loses and restores its connection.
- Emergency abort and compensating return-to-base flow when package drop fails.
- Live fleet updates over WebSockets, plus mission APIs.
- 50 mock drones starting around Raipur, Chhattisgarh, on a CesiumJS globe centered on the fleet.
- Optional local simulation mode if you do not want to start a Temporal server. Simulation state is in memory and is lost when the backend stops.

The drones, GPS readings, and flight activities are simulated. This project does not connect to or control physical drones. The Java SDK is a Maven dependency; Maven downloads it automatically on the first backend run. Cesium uses OpenStreetMap tiles; a Cesium ion token is optional.

### \# Requirements

- Java JDK 21
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop with Docker Compose (for real Temporal workflows)
- IntelliJ IDEA is optional; its built-in Terminal can run all commands below.

### Open in IntelliJ

1. In IntelliJ, choose **File → Open** and open `D:\Infotact\AeroSaga` (the folder containing `pom.xml`).
2. Choose to open/import it as a Maven project when prompted and wait for Maven sync to finish.
3. Under **File → Project Structure → Project**, set the Project SDK to Java 21.
