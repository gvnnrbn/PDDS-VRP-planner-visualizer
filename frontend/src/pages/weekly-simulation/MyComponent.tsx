import { useState, useEffect } from 'react';
import useStomp from './useStomp';
import { Button } from '@chakra-ui/react';

const MyComponent = () => {
  const { connected, subscribe, unsubscribe, publish } = useStomp('http://localhost:8080/ws');
  const [log, setLog] = useState<string[]>([]);

  useEffect(() => {
    if (!connected) return;

    const handleSimulacion = (message: any) => {
      const payload = JSON.parse(message.body);
      const minuto = payload.simulacion?.[0]?.minuto ?? '?';
      setLog(prev => [...prev, `📦 Recibido plan en minuto ${minuto}`]);
    };

    subscribe('/topic/simulacion', handleSimulacion);
    return () => {
      unsubscribe('/topic/simulacion');
    };
  }, [connected]);

  const iniciarSimulacion = () => {
    const fechaActual = new Date().toISOString();
    publish('/app/simulacion-test', JSON.stringify({ timestamp: fechaActual }));
    setLog(prev => [...prev, `🚀 Enviada solicitud de simulación: ${fechaActual}`]);
  };

  return (
    <div>
      <p>🔌 Conexión: {connected ? 'Conectado ✅' : 'Desconectado ❌'}</p>
      <Button variant="outline" onClick={iniciarSimulacion} disabled={!connected}>
        Iniciar simulación
      </Button>
      <pre style={{ marginTop: '1rem', background: '#f5f5f5', padding: '1rem', color: 'black' }}>
        {log.map((linea, i) => (
          <div key={i}>{linea}</div>
        ))}
      </pre>
    </div>
  );
};

export default MyComponent;
