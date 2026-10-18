import { useEffect, useRef } from 'react';
import * as Cesium from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';
import { useTelemetry } from '../hooks/useTelemetry';

const STATIC_DRONES = [
  { id: 'alpha', lat: 23.0225, lon: 72.5714 },
  { id: 'bravo', lat: 23.04, lon: 72.6 },
  { id: 'charlie', lat: 23.06, lon: 72.55 },
  { id: 'delta', lat: 23.01, lon: 72.64 },
  { id: 'echo', lat: 23.08, lon: 72.62 },
];

export default function GlobeView() {
  const containerRef = useRef(null);
  const viewerRef = useRef(null);
  const telemetry = useTelemetry();

  // 1. Map Initialization
  useEffect(() => {
    const viewer = new Cesium.Viewer(containerRef.current, {
      baseLayer: new Cesium.ImageryLayer(
        new Cesium.OpenStreetMapImageryProvider({ url: 'https://tile.openstreetmap.org/' })
      ),
      baseLayerPicker: false,
      geocoder: false,
      homeButton: false,
      sceneModePicker: false,
      navigationHelpButton: false,
      animation: false,
      timeline: false,
      fullscreenButton: false,
      infoBox: false,
      selectionIndicator: false,
    });

    viewer.camera.setView({
      destination: Cesium.Cartesian3.fromDegrees(72.6, 23.04, 30000),
    });

    // Pehle static base points render karna
    STATIC_DRONES.forEach((d) => {
      viewer.entities.add({
        id: d.id,
        position: Cesium.Cartesian3.fromDegrees(d.lon, d.lat, 100),
        point: {
          pixelSize: 12,
          color: Cesium.Color.CYAN,
          outlineColor: Cesium.Color.WHITE,
          outlineWidth: 2,
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
        label: {
          text: d.id.toUpperCase(),
          font: 'bold 11px sans-serif',
          pixelOffset: new Cesium.Cartesian2(0, -18),
          fillColor: Cesium.Color.WHITE,
          style: Cesium.LabelStyle.FILL_AND_OUTLINE,
          outlineColor: Cesium.Color.BLACK,
          outlineWidth: 2,
        },
      });
    });

    viewerRef.current = viewer;

    return () => viewer.destroy();
  }, []);

  // 2. Real-Time Progressive Updates (Websocket Listener)
  useEffect(() => {
    if (!viewerRef.current) return;
    const viewer = viewerRef.current;

    Object.values(telemetry).forEach((droneData) => {
      const lat = droneData.lat ?? droneData.latitude;
      const lon = droneData.lon ?? droneData.longitude;

      if (!droneData || !droneData.droneId || lat == null || lon == null) return;

      let entity = viewer.entities.getById(droneData.droneId);
      
      // Agar drone nahi hai, to use generate karein as a distinct, larger entity (22px MAGENTA)
      if (!entity) {
        entity = viewer.entities.add({
          id: droneData.droneId,
          position: Cesium.Cartesian3.fromDegrees(lon, lat, 120), // Thoda high altitude coordinate
          point: {
            pixelSize: 22, 
            color: Cesium.Color.MAGENTA,
            outlineColor: Cesium.Color.WHITE,
            outlineWidth: 3,
            disableDepthTestDistance: Number.POSITIVE_INFINITY,
          },
          label: {
            text: String(droneData.droneId).toUpperCase(),
            font: 'bold 14px sans-serif',
            pixelOffset: new Cesium.Cartesian2(0, -32),
            fillColor: Cesium.Color.YELLOW, // High visible font highlight
            style: Cesium.LabelStyle.FILL_AND_OUTLINE,
            outlineColor: Cesium.Color.BLACK,
            outlineWidth: 3,
          },
        });
      }

      // Smooth step-by-step translation map par visually translate hogi
      entity.position = Cesium.Cartesian3.fromDegrees(lon, lat, 120);
        
      if (droneData.status === 'NAVIGATING') {
          entity.point.color = Cesium.Color.LIME; // Green on run
      } else if (droneData.status === 'DROPPING_PACKAGE') {
          entity.point.color = Cesium.Color.ORANGE; // Package drop visual indicator
      } else if (droneData.status === 'RETURNING') {
          entity.point.color = Cesium.Color.MAGENTA; // Return process highlight
      } else if (droneData.status === 'COMPLETED') {
          entity.point.color = Cesium.Color.GRAY; // Gray on end mission
      }
    });
  }, [telemetry]);

  return (
    <div style={{ position: 'relative', width: '100vw', height: '100vh', overflow: 'hidden' }}>
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />

      {/* Overlay Dashboard */}
      <div style={{
        position: 'absolute',
        top: '20px',
        left: '20px',
        width: '320px',
        background: 'rgba(20, 25, 35, 0.85)',
        backdropFilter: 'blur(10px)',
        borderRadius: '12px',
        padding: '20px',
        color: '#fff',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        zIndex: 10,
        fontFamily: 'sans-serif'
      }}>
        <h2 style={{ margin: '0 0 15px 0', fontSize: '20px', borderBottom: '1px solid #444', paddingBottom: '10px', color: '#00e5ff' }}>
          🚁 AeroSaga Mission Control
        </h2>
        
        {Object.keys(telemetry).length === 0 ? (
          <p style={{ color: '#aaa', fontSize: '14px' }}>Waiting for fleet deployment...</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {Object.values(telemetry).map((drone) => {
              if (!drone || !drone.droneId) return null;
              
              const battery = typeof drone.battery === 'number' ? drone.battery : 0;
              const actualLat = drone.lat ?? drone.latitude;
              const actualLon = drone.lon ?? drone.longitude;
              const latStr = typeof actualLat === 'number' ? actualLat.toFixed(4) : '0.0000';
              const lonStr = typeof actualLon === 'number' ? actualLon.toFixed(4) : '0.0000';

              return (
                <div key={drone.droneId} style={{ background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                    <strong style={{ color: '#fff' }}>{String(drone.droneId).toUpperCase()}</strong>
                    <span style={{ fontSize: '12px', background: battery > 20 ? '#00e676' : '#ff1744', color: '#000', padding: '2px 6px', borderRadius: '4px', fontWeight: 'bold' }}>
                      🔋 {battery}%
                    </span>
                  </div>
                  <div style={{ fontSize: '13px', color: '#bbb' }}>Status: <span style={{ color: '#00e5ff' }}>{drone.status || 'UNKNOWN'}</span></div>
                  <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>
                    Lat: {latStr} | Lon: {lonStr}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}