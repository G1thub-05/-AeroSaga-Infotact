import GlobeView from './components/GlobeView';
import './App.css';

export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <h2>AeroSaga - Fleet Control</h2>
      </header>
      <main className="map">
        <GlobeView />
      </main>
    </div>
  );
}