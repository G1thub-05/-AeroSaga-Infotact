import { useEffect, useRef } from 'react';
import * as Cesium from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';

const STATIC_DRONES = [
  { id: 'alpha', lat: 23.0225, lon: 72.5714 },
  { id: 'bravo', lat: 23.04, lon: 72.6 },
  { id: 'charlie', lat: 23.06, lon: 72.55 },
  { id: 'delta', lat: 23.01, lon: 72.64 },
  { id: 'echo', lat: 23.08, lon: 72.62 },
];
export default function GlobeView() {
  const containerRef = useRef(null);

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
        STATIC_DRONES.forEach((d) => {
      viewer.entities.add({
        id: d.id,
        position: Cesium.Cartesian3.fromDegrees(d.lon, d.lat, 100),
        point: {
          pixelSize: 14,
          color: Cesium.Color.ORANGE,
          outlineColor: Cesium.Color.WHITE,
          outlineWidth: 2,
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
        label: {
          text: d.id,
          font: '14px sans-serif',
          pixelOffset: new Cesium.Cartesian2(0, -22),
          fillColor: Cesium.Color.WHITE,
          outlineColor: Cesium.Color.BLACK,
          outlineWidth: 3,
          style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        },
      });
    });
    return () => viewer.destroy();
  }, []);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
}