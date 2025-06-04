import { useState, useEffect, useRef, useCallback } from 'react';
import { Client, type Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const useStomp = (url) => {
  const [client, setClient] = useState(null);
  const [connected, setConnected] = useState(false);
  const subscriptions = useRef(new Map());

  const connect = useCallback(() => {
  if (client || connected) return;

  const socket = new SockJS(url, null, 
    // @ts-ignore
    { withCredentials: true });

    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000, // intenta reconectar cada 5s (si quieres mantener reconexión)
      onConnect: () => {
        console.log('STOMP connected');
        setConnected(true);
        subscriptions.current.forEach((callback, destination) => {
          stompClient.subscribe(destination, callback);
        });
      },
      onWebSocketClose: (evt) => {
        console.warn('WebSocket closed', evt);
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
      debug: (str) => console.log('[STOMP]', str), // más visibilidad
    });

  stompClient.activate();
  setClient(stompClient);
}, [url, client, connected]);

  const disconnect = useCallback(() => {
    if (client) {
      client.deactivate();
      setClient(null);
      setConnected(false);
      subscriptions.current.clear();
    }
  }, [client]);

  const subscribe = useCallback(
    (destination, callback) => {
      if (client && connected) {
        const sub = client.subscribe(destination, callback);
        subscriptions.current.set(destination, sub);
      } else {
        subscriptions.current.set(destination, callback);
      }
    },
    [client, connected]
  );

  const unsubscribe = useCallback((destination) => {
    if (client && connected) {
      const sub = subscriptions.current.get(destination);
      if (sub) {
        sub.unsubscribe();
        subscriptions.current.delete(destination);
      }
    } else {
      subscriptions.current.delete(destination);
    }
  }, [client, connected]);

  const publish = useCallback(
    (destination, body, headers = {}) => {
      if (client && connected) {
        client.publish({ destination, body, headers });
      }
    },
    [client, connected]
  );

useEffect(() => {
  // Solo conectar una vez al montar
  connect();
  return () => {
    disconnect();
  };
  // ⛔️ NO incluir connect ni disconnect en deps
}, []); // ← importante: arreglo vacío

  return { connected, subscribe, unsubscribe, publish };
};

export default useStomp;