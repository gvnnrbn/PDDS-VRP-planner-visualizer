import { useState, useEffect, useRef, useCallback } from 'react';
import { Client, type Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const useCollapseStomp = (url: string) => {
  const [client, setClient] = useState(null);
  const [connected, setConnected] = useState(false);
  const subscriptions = useRef(new Map());

  const connect = useCallback(() => {
    if (client) return;
    const socket = new SockJS(url, null, 
      // @ts-ignore
      { withCredentials: true });
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('STOMP connected');
        setConnected(true);
        subscriptions.current.forEach((sub, destination) => {
          if (typeof sub === 'function') {
            const stompSub = stompClient.subscribe(destination, sub);
            subscriptions.current.set(destination, stompSub);
          }
        });
      },
      onWebSocketClose: (evt) => {
        console.warn('WebSocket closed', evt);
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
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
    (destination: string, body: string, headers = {}) => {
      if (client && connected) {
        client.publish({ destination, body, headers });
      } else {
        console.warn("Tried to publish before STOMP was connected", { destination });
      }
    },
    [client, connected]
  );

  useEffect(() => {
    connect();
  }, []);

  return { connected, subscribe, unsubscribe, publish };
};

export default useCollapseStomp; 