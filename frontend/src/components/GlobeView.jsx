import { useEffect, useRef } from 'react';
import * as Cesium from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';

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
    return () => viewer.destroy();
  }, []);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
}