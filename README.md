## **🚁 Project overview: AeroSaga — Drone Mission Control**

<p align="justify">AeroSaga is a full-stack demonstration of a drone fleet control system. Operators can select a simulated drone, plan a delivery, follow its route on a 3D map, and monitor mission progress in real time. The project brings together a **React** dashboard, a **Spring Boot** API, **MongoDB** persistence, **Temporal** workflows, and **CesiumJS** maps. It is designed for local development and demonstration. AeroSaga does not connect to or control physical drones. Drone locations, battery readings, and flight activity are simulated. The drones, GPS readings, and flight activities are simulated. The Java SDK is a Maven dependency; Maven downloads it automatically on the first backend run. </p>

## **🧰 Technologies used**

```text
Frontend        - React, Vite, JavaScript`      - Operator dashboard and mission controls
3D map          - CesiumJS                      - Globe, drone markers, routes, and map styles
Backend         - Java 21, Spring Boot          - REST API, fleet state, and mission coordination
Live updates    - WebSocket                     - Sends fleet and mission updates to the dashboard
Database        - MongoDB, Spring Data MongoDB  - Stores drone state and latest mission state
Workflow engine - Temporal                      - Runs durable mission workflows in full mode
Local services  - Docker Compose                - Starts MongoDB and Temporal for development
```

## **📁 Project structure**

```text
-AeroSaga-Infotact/
├── .gitignore                      # Ignores local secrets and generated files
├── README.md                       # Project overview and setup/run guide
├── WORK_SUMMARY.md                 # Four-week manager report
├── backend/
│   ├── .env.example                # Backend environment template
│   ├── docker-compose.yml          # MongoDB and Temporal services
│   ├── pom.xml                     # Java and Spring Boot build configuration
│   ├── .mvn/                       # Maven settings
│   ├── .env                        # Create locally from backend/.env.example
│   └── src/main/
│       ├── java/com/aerosaga/      # API, demo, persistence, telemetry, workflows
│       └── resources/              # Spring Boot configuration
└── frontend/
    ├── .env.example                # Frontend settings template
    ├── package.json                # Frontend dependencies and scripts
    ├── package-lock.json           # Locked dependency versions
    ├── index.html                  # Web app entry page
    ├── vite.config.js              # Vite and Cesium build configuration
    └── src/
        ├── api/                    # Central frontend API client
        ├── components/             # React UI components
        ├── css/                    # Global, layout, and component stylesheets
        ├── App.jsx                 # Dashboard composition and state
        ├── Globe.jsx               # Cesium globe, markers, and map styles
        └── main.jsx                # React entry point
```

### **✨ Features**

- Drone mission workflow: \*\*Takeoff → Navigate → Drop Package → Return to Base\*\*.
- View and search a simulated fleet of 50 mock drones starting around Raipur, Chhattisgarh.
- Select a drone and inspect its status, battery, connectivity, and current location.
- Dispatch a delivery mission using a place search or latitude/longitude coordinates.
- Follow outbound, return routes and switch between **Satellite**, **Terrain**, and **Street** 3D globe styles.
- Demonstrate signal loss, recovery, emergency abort, compensating return-to-base flow when package drop fails.
- Run missions through Temporal or use local demo mode if you do not want to start a Temporal server.
- Watch live fleet and mission updates arrive through WebSockets, plus mission APIs.
- Save drone state and each mission's latest state in MongoDB.

## **⚙️ Prerequisites Setup**

```bash
1. Java JDK 21 — verify with `java -version`.
2. Maven 3.9 or newer — verify with `mvn -version`. Maven output should show Java 21.
3. Node.js 20 or newer and npm — verify with `node -v` and `npm -v`.
4. Docker Desktop with Docker Compose — required for Temporal and optional for MongoDB. Verify with `docker --version` and `docker compose version`.
6. Optional: a Cesium ion token for 3D ground elevation. Map imagery styles work without it.
7. IntelliJ IDEA is optional; its built-in Terminal can run all commands below.
```

## **⚙️ ️Mission flow theory**

<p align="justify">
A mission is a controlled sequence where an operator selects an available online drone that is not already active, specifies a destination by place name or coordinates, and starts a mission, if no custom source is entered, the drone's current location is used. The backend checks the coordinates, creates a mission record, and starts either a Temporal workflow or the local simulator, which handles takeoff, navigation, package delivery, and return to base. The simulated drone moves along the outbound route. Its status and coordinates are updated as it moves; the outbound route and drone marker use mint/green. At the destination, the simulator completes the package-drop activity.

A configured demo failure follows the safe-return path instead. After delivery or after an abort or failed delivery—the drone follows the amber return route to its starting point. It remains unavailable for another mission while its status is `RETURNING`. At base, the mission becomes `COMPLETED`, `ABORTED`, or `FAILED_SAFE`. Once the drone is no longer active and is online, it can be selected for another mission.

</p>

```text
- Step 1. Select an available droneSet and confirm source and destination
- Step 2. Validate coordinates and create mission
- Step 3. Take off → Fly outbound → Drop package
- Step 4. Return to base
```

<p align="justify">If telemetry is lost during a mission, the workflow changes to `PAUSED` and waits for the drone to reconnect. When telemetry returns, the workflow resumes from its saved progress. An emergency abort stops the outbound mission and starts a safe return; the drone is available again only after reaching base. If package delivery fails, the simulator runs its compensation behavior and returns the drone safely.</p>
