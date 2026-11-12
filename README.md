<div align="center">
<!-- Light Mode Header -->
<a href="https://github.com/G1thub-05#gh-light-mode-only">
  <img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:00c6ff,50:0072ff,100:8e2de2&height=260&section=header&text=AeroSaga%20—%20Drone%20Mission%20Control&fontSize=35&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Takeoff%20%7C%20Navigate%20%7C%20Drop%20Package%20%7C%20Return%20to%20Base&descAlignY=58&descSize=18"/>
</a>

<!-- Dark Mode Header -->
<a href="https://github.com/G1thub-05#gh-dark-mode-only">
  <img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:ff512f,50:dd2476,100:ff0000&height=260&section=header&text=AeroSaga%20—%20Drone%20Mission%20Control&fontSize=35&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Takeoff%20%7C%20Navigate%20%7C%20Drop%20Package%20%7C%20Return%20to%20Base&descAlignY=58&descSize=18"/>
</a>
</div>

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

| Mode          | Set `TEMPORAL_ENABLED`   | Start              | Why Choose it                                                                  |
| :------------ | :----------------------- | :----------------- | :----------------------------------------------------------------------------- |
| Full workflow | `TEMPORAL_ENABLED=true`  | MongoDB + Temporal | For durable pause/resume, and Temporal's workflow execution history.           |
| Local demo    | `TEMPORAL_ENABLED=false` | Only MongoDB       | To try the UI and mission simulation with fewer services and without Temporal. |

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

## **Step 2 — Create frontend environment file**

<p align="justify">Create a .env file inside the frontend folder by copying <code>frontend/.env.example</code>. Keep <code>VITE_API_URL=http://localhost:8080</code> as the default API URL. Add a Cesium ion token only if you need optional 3D terrain elevation.</p>

```powershell
# Frontend settings (read with --env-file frontend/.env)
VITE_API_URL            -     http://localhost:8080               -  Backend URL used by the frontend.
VITE_CESIUM_ION_TOKEN  -      your_cesium_ion_token               -  Optional Cesium ion token.
```

## **📁 Where to Put the Environment Files**

```text
-AeroSaga-Infotact/
├── backend/
│   ├── .env                  # Create locally from backend/.env.example
│   └── .env.example          # Backend settings template
└── frontend/
    ├── .env                  # Create locally from frontend/.env.example
    └── .env.example          # Frontend settings template
```

<p align="justify"> <b>Note:</b> The real .env files contain local configuration and should not be committed to Git and are ignored by Git. Keep the .env.example files as templates. Docker Compose can load the backend settings explicitly using --env-file backend/.env when run from the project root; this lets it use the port values shown there.</p>

## **Step 3 — Open the project folder**

<p align="justify"> Open your project folder in PowerShell. If your project folder has a different name or is located in another directory, use its actual path. For the Docker steps, navigate to the <code>backend</code> folder inside your project directory. This folder contains <code>docker-compose.yml</code> and <code>.env</code>. For example if your project is located at <code>D:\-AeroSaga-Infotact</code>, open PowerShell in that folder and run the Docker commands from its <code>backend</code> subfolder. Replace this example path with your project's actual location.</p>

```powershell
cd "D:\-AeroSaga-Infotact"                # Navigate to your project folder
Test-Path .\backend\docker-compose.yml    # Verify the Docker Compose file exists (should print `True`)
cd backend                                # Navigate to the backend folder for Docker steps
```

## **⚙️ Option A — MongoDB + Temporal Setup (with Docker)**

<p align="justify">This mode starts MongoDB and Temporal in the background using Docker Compose. MongoDB stores drone data and mission state, while Temporal manages durable mission workflows and tracks their progress. This setup is recommended when you want to demonstrate complete mission orchestration and workflow recovery after backend interruptions.</p>

```powershell
docker compose up -d mongodb temporal     # Start MongoDB and Temporal using Docker Compose in the background
docker compose ps                         # Shows the status of MongoDB and Temporal container
```

<p align="justify">Wait until both containers show as healthy/running before starting the backend, if health checks are configured. If one is still starting, run <code>docker compose ps</code> again after a short wait. Once the required services are ready, start the backend and frontend using the project's setup instructions. This mode enables workflow history and durable mission execution by running MongoDB and Temporal alongside the application.</p>

## **⚙️ Option B — Only MongoDB Setup for Local demo (with Docker)**

<p align="justify">This mode starts only MongoDB in the background using Docker Compose, without starting Temporal. It is suitable for quick startup and basic UI/API demonstrations where durable workflow orchestration is not required. MongoDB stores drone data and the latest mission state, while active mission progress remains in backend memory and is lost when the backend stops.</p>

```powershell
docker compose up -d mongodb              # Start MongoDB only without Temporal using Docker Compose in the background
docker compose ps                         # Shows the status of MongoDB container
```

<p align="justify">Wait until MongoDB is running and healthy, if health checks are configured, before starting the backend. The MongoDB container should be running and ready for the backend to connect. Ensure TEMPORAL_ENABLED=false is configured in the backend .env file for local demo mode.</p>

## **⚙️ Option C — MongoDB installed directly on Windows (without Docker)**

```text
1. Install **MongoDB Community Server** for Windows. During installation, enable **Install MongoD as a Service** so MongoDB starts as a Windows service.
2. Open the Windows **Services** app, find the MongoDB Server service, and start it if it is not already running.
3. Keep `MONGODB_URI=mongodb://localhost:27017/aerosaga` in `backend/.env`.
4. For Temporal workflow mode, open PowerShell in the `backend` folder and run `docker compose up -d temporal`. MongoDB itself will run from the Windows service.
5. For local demo mode (`TEMPORAL_ENABLED=false`), no Docker service is needed; the Windows MongoDB service is enough.
6. Do not run the Docker MongoDB container and the Windows MongoDB service on the same port (`27017`) at the same time.
7. Choose one database server. Docker-based Temporal still needs Docker when `TEMPORAL_ENABLED=true`.
```

## **Step 4 — Start the backend**

1. In IntelliJ, choose **File → Open** and open `D:\-AeroSaga-Infotact\backend`- the folder containing `pom.xml`.
2. Choose to open/import it as a Maven project when prompted and wait for Maven sync to finish.
3. Under **File → Project Structure → Project**, set the Project SDK to Java 21.
4. By default, the backend connects to MongoDB and Temporal. Keep this terminal open.
5. Open a PowerShell terminal in the `backend` folder and run: `mvn spring-boot:run`
6. Before starting backend, ensure Docker is running and both MongoDB and Temporal are up and healthy.

## **Step 5 — Start the frontend**

1. In VS Code, choose **File → Open** and open `D:\-AeroSaga-Infotact\frontend`- folder containing `package.json`.
2. Open a terminal in Visual Studio Code using Terminal → New Terminal.
3. Ensure the terminal is in the frontend folder and then install the frontend dependencies: `npm install`
4. The `npm install` is only needed the first time or after frontend dependency changes
5. Start the frontend development server: `npm run dev`
6. Open the local URL displayed in the terminal, typically `http://localhost:5173`, in your browser.
7. Ensure the backend is running before testing features that require API access.

### **🌐 Useful Links**

```powershell
1. MongoDB addresses:           mongodb://localhost:27017/aerosaga     # Local MongoDB does not require a username or password in this setup.
2. Temporal Server (gRPC):      http://localhost:7233
3. Temporal Web UI addresses:   http://localhost:18233
4. Backend health addresses:    http://localhost:8080/api/health
5. Fleet API addresses:         http://localhost:8080/api/fleet
6. Saved missions addresses:    http://localhost:8080/api/missions
7. Dashboard addresses:         http://localhost:5173 if blank, try http://127.0.0.1:5173
```

## **Step 6 — Open and try the demo**

1. In **Fleet overview**, select one of the simulated drones around Raipur.
2. Enter a destination by place name and click **Find**, or enter latitude/longitude directly and click **Dispatch mission**.
3. Switch the map style between **Satellite**, **Terrain**, and **Street**.
4. During flight, click **Simulate signal loss**, then reconnect the drone to see the mission pause and resume.
5. Click **Emergency abort** during a mission to request a safe return.
6. To see compensation, enable **Simulate drop activity failure** before dispatch.

<p align="justify">The outbound route appears green and the return route amber. A complete delivery and return takes about 30–35 seconds in the simulator; aborting starts the return immediately, which takes up to about 15 seconds. The drone being returned stays unavailable until it reaches base, but other available drones can be dispatched meanwhile.</p>

## **📘 See the saved MongoDB data in both options**

<p align="justify">The app creates the `aerosaga` database and <code>drones</code> collection during backend startup. The <code.missions> collection appears after the first mission is dispatched. Both server options use the same database and collection names, but their stored data is separate: Docker MongoDB uses the <code>mongodb-data</code> volume, while Windows MongoDB uses its own local data folder. Compass shows whichever server is currently listening on port <code>27017</code>.</p>

```text
1. Open **MongoDB Compass** and connect to `mongodb://localhost:27017` for either Docker MongoDB or Windows MongoDB.
2. In the database list, open **`aerosaga`** and Open **`drones`** to inspect the 50 seeded drones details.
3. Dispatch a mission in AeroSaga, then open or refresh **`missions`** to inspect its latest status and coordinates.
4. The data belongs to Docker's **`mongodb-data`** volume. It remains when you stop the container with `docker compose down`.
5. Deleting volumes with `docker compose down -v` removes it.
```

## **🛠️ Troubleshooting**

### **1. Backend cannot connect to MongoDB or Temporal**

<p align="justify">Check that exactly one MongoDB server is running on port `27017`. For Docker, check <code>docker compose ps</code> and start it with <code>docker compose up -d mongodb</code>. For a Windows installation, open Services and start the MongoDB Server service. Keep <code>MONGODB_URI</code> pointed at the host and port where MongoDB is listening, then restart the backend.

<p align="justify">If the log contains <code>UNAVAILABLE</code>, <code>Connection refused</code>, or <code>localhost:7233</code>, the backend is configured for Temporal mode but the Temporal server is not running. From the <code>backend</code> folder, run <code>docker compose up -d temporal</code> or <code>docker compose up -d mongodb temporal</code> for the full setup, then wait until <code>docker compose ps</code> shows Temporal as healthy. Restart the backend after the service is ready.</p>

<p align="justify">For IntelliJ IDEA, make sure the run configuration's <b>Working directory</b> is the <code>backend</code> folder (Spring Boot loads <code>.env</code> from this folder) so Spring Boot can load <code>.env</code>. If you want to run without Temporal, set <code>TEMPORAL_ENABLED=false</code> in <code>backend/.env</code>, start MongoDB only, and restart the backend.</p>

---

### **2. A port is already in use**

<p align="justify">Default ports are <code>27017</code> (MongoDB), <code>7233</code> and <code>18233</code> (Temporal), <code>8080</code> (backend), and <code>5173</code> (Vite). Stop the other process, or change the matching port in <code>backend/.env</code>. Docker Compose reads those values because the documented commands pass <code>--env-file backend/.env</code>. From the project root, recreate the Temporal container and check its logs:</p>

```powershell
docker compose up -d --force-recreate temporal
docker compose up -d --force-recreate mongodb
docker compose ps
docker compose logs --tail 100 temporal
```

## **📝 Important notes and limitations**

- Drone movement, GPS, battery, and mission actions are simulated.
- MongoDB does not store every GPS waypoint as a separate history record.
- An active simulation will not resume after the backend stops in Local demo mode.
- Use Temporal mode when durable workflow execution and workflow history are needed.
- Cesium ion is optional. Terrain imagery works without a token; 3D ground elevation requires one.

<!-- Footer -->
<div align="center">
<!-- Light Mode Footer -->
<a href="https://github.com/G1thub-05#gh-light-mode-only">
  <img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:00c6ff,50:0072ff,100:8e2de2&height=120&section=footer"/>
</a>

<!-- Dark Mode Footer -->
<a href="https://github.com/G1thub-05#gh-dark-mode-only">
  <img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:ff512f,50:dd2476,100:ff0000&height=120&section=footer"/>
</a>
</div>