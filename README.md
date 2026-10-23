## **🚁 Project overview: AeroSaga — Drone Mission Control**

<p align="justify">
AeroSaga is a full-stack demonstration of a drone fleet control system. Operators can select a simulated drone, plan a delivery, follow its route on a 3D map, and monitor mission progress in real time. The project brings together a <code>`React`</code> dashboard, a <code>`Spring Boot API`</code>, <code>`MongoDB`</code> persistence, </code>, <code>`Temporal`</code> workflows, and </code>, <code>`CesiumJS`</code> maps. It is designed for local development and demonstration. AeroSaga does not connect to or control physical drones. Drone locations, battery readings, and flight activity are simulated. The drones, GPS readings, and flight activities are simulated. The Java SDK is a Maven dependency; Maven downloads it automatically on the first backend run.
</p>

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

## **✨ Features**

- Drone mission workflow: <code>`Takeoff`</code> → <code>`Navigate`</code> → <code>`Drop Package`</code> → <code>`Return to Base`</code>.
- View and search a simulated fleet of 50 mock drones starting around Raipur, Chhattisgarh.
- Select a drone and inspect its status, battery, connectivity, and current location.
- Dispatch a delivery mission using a place search or latitude/longitude coordinates.
- Follow outbound, return routes and switch between <code>`Satellite`</code>, <code>`Terrain`</code>, and <code>`Street`</code> 3D globe styles.
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
<p/>

<p align="justify">
A configured demo failure follows the safe-return path instead. After delivery or after an abort or failed delivery—the drone follows the amber return route to its starting point. It remains unavailable for another mission while its status is <code>`RETURNING`</code>. At base, the mission becomes <code>`COMPLETED`</code>, <code>`ABORTED`</code>, or <code>`FAILED_SAFE`</code>. Once the drone is no longer active and is online, it can be selected for another mission.
</p>

```text
- Step 1. Select an available droneSet and confirm source and destination
- Step 2. Validate coordinates and create mission
- Step 3. Take off → Fly outbound → Drop package
- Step 4. Return to base
```

<p align="justify">
If telemetry is lost during a mission, the workflow changes to <code>`PAUSED`</code> and waits for the drone to reconnect. When telemetry returns, the workflow resumes from its saved progress. An emergency abort stops the outbound mission and starts a safe return; the drone is available again only after reaching base. If package delivery fails, the simulator runs its compensation behavior and returns the drone safely.
</p>

## **⚙️ Temporal Mode and Local Demo Mode**

<p align="justify">
AeroSaga supports two execution modes for managing and simulating drone missions: <code>`Temporal Mode`</code> and <code>`Local Demo Mode`</code>. Both modes follow the same basic mission sequence, including takeoff, navigation, package delivery, and return to base, but differ in how workflow execution and recovery are handled. MongoDB is used in both run modes. Temporal mode runs durable workflows; local demo mode runs the mission simulator inside the backend. Choose a mode before starting the backend.
</p>

<p align="justify">
<code><b><i>Temporal Mode:</i></b></code> In this mode, Temporal manages the mission workflow and tracks its progress durably. If the backend experiences an interruption, Temporal can recover and continue workflow execution based on its persisted history. MongoDB stores the latest state of each drone and mission, while Temporal manages workflow execution and recovery.
</p>

<p align="justify">
<code><b><i>Local Demo Mode:</i></b></code> This mode executes the mission sequence directly in the backend without using Temporal. It is useful for local development, testing, and demonstrations because it requires a simpler setup. MongoDB continues to persist the latest drone and mission state; however, an in-progress simulation does not automatically resume after a backend restart.
</p>

<p align="justify">
<code><b><i>Simulation Telemetry and Data Persistence:</i></b></code> AeroSaga simulates drone movement and streams position updates to the dashboard as telemetry. These updates represent simulated flight activity and do not control physical drones. MongoDB stores the latest state of each drone and mission rather than maintaining a separate historical record for every route waypoint.
</p>

### **⚙️ How the services fit together**

```text
┌────────────────────────────────────────────────────┐
│              React + Cesium dashboard              │
│ Fleet · mission controls · Map · route · telemetry │
└─────────────────────────┬──────────────────────────┘
                 REST + WebSocket
                          ▼
┌────────────────────────────────────────────────────┐
│ Spring Boot backend -- API · fleet · mission logic │
└───────────┬──────────────────────────┬─────────────┘
            ▼                          ▼
   ┌──────────────────┐      ┌────────────────────┐
   │ MongoDB App data │      │ Temporal Workflows │
   └──────────────────┘      └────────────────────┘
```
## **⚙️ Full Setup Guide: Temporal workflow mode**

<p align="justify">This setup runs MongoDB and Temporal to support durable mission orchestration. Temporal keeps tracks workflow progress and allows workflows to resume after backend interruptions. Using Temporal is best for demonstrating durable mission orchestration. Without Temporal, the local simulator starts faster and is suitable for basic UI/API demonstrations. However, active mission progress is stored in memory and will not resume if the backend stops. MongoDB still persists drone data and the latest mission state. The first run needs an internet connection so Docker, Maven, and npm can download the project dependencies and service images. If using IntelliJ IDEA, open <code>backend/pom.xml</code> as a Maven project and set the Project SDK to Java 21. Follow the steps below to configure the environment, start the required services, and run the backend and frontend.</p>

| Mode           | Set `TEMPORAL_ENABLED`    | Start              | Why Choose it                                                                  |
| :------------- | :------------------------ | :----------------- | :----------------------------------------------------------------------------- |
| Full workflow  | `TEMPORAL_ENABLED=true`   | MongoDB + Temporal | For durable pause/resume, and Temporal's workflow execution history.           |
| Local demo     | `TEMPORAL_ENABLED=false`  | Only MongoDB       | To try the UI and mission simulation with fewer services and without Temporal. |

## **Step 1 — Create backend environment file**

<p align="justify">Create a .env file inside the backend folder by copying backend/.env.example. Keep the default MongoDB and Temporal settings for the standard setup. Set <code>TEMPORAL_ENABLED=false</code> only if you want to run the application in local demo mode without Temporal. If <code>backend/.env</code> already exists, keep it and modify only the settings you need.</p>

```powershell
# Backend settings loaded by Spring Boot from this folder.
PORT                    -     8080                                -  Backend HTTP port.
MONGODB_URI             -     mongodb://localhost:27017/aerosaga  -  MongoDB connection string.
TEMPORAL_ENABLED        -     true                                -  Set to `false` for the local simulator.
TEMPORAL_ADDRESS        -     localhost:7233                      -  Temporal server address.
TEMPORAL_NAMESPACE      -     default                             -  Temporal namespace.
TEMPORAL_TASK_QUEUE     -     aerosaga-missions                   -  Workflow task queue.
```

```powershell
# Docker Compose host port settings (read with --env-file backend/.env)
MONGO_BIND_ADDRESS      -     127.0.0.1                           -  Host address for the MongoDB container.
MONGO_PORT              -     27017                               -  Host port mapped to MongoDB.
TEMPORAL_BIND_ADDRESS   -     127.0.0.1                           -  Host address for Temporal ports.
TEMPORAL_GRPC_PORT      -     7233                                -  Host port for Temporal gRPC.
TEMPORAL_UI_PORT        -     18233                               -  Host port for Temporal web UI.
```
