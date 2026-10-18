import { useState, useEffect } from 'react';

export const useTelemetry = () => {
    const [drones, setDrones] = useState({});

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8081/ws/telemetry');

        socket.onopen = () => {
            console.log('Connected to Telemetry WebSocket! 🚀');
        };

        // Yahan 'ssocket' ki jagah 'socket' aayega
        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                setDrones(prevDrones => ({
                    ...prevDrones,
                    [data.droneId]: data
                }));
            } catch (error) {
                console.warn('Ignored incomplete WebSocket data');
            }
        };

        socket.onclose = () => {
            console.log('Disconnected from Telemetry WebSocket');
        };

        return () => {
            socket.close();
        };
    }, []);

    return drones;
};