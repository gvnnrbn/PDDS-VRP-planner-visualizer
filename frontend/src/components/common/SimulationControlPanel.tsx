import React, { useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Box, Button, Input, VStack, HStack, useToast, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, FormControl, FormLabel, useDisclosure, Flex, Accordion, AccordionItem, AccordionButton, AccordionPanel, AccordionIcon, Text } from '@chakra-ui/react';
import { FaTruck, FaWarehouse, FaMapMarkerAlt } from 'react-icons/fa';
import { renderToStaticMarkup } from 'react-dom/server';

interface LogEntry {
  timestamp: string;
  message: string;
}

const defaultServerUrl = 'http://localhost:8080';

// Cache global para imágenes de íconos
const iconImageCache: Record<string, HTMLImageElement> = {};

// Helper para convertir un ícono de react-icons a imagen para canvas, usando cache
function iconToImage(IconComponent: React.ElementType, color: string, size = 32): Promise<HTMLImageElement> {
  const cacheKey = `${IconComponent.displayName || IconComponent.name || ''}_${color}_${size}`;
  if (iconImageCache[cacheKey]) {
    return Promise.resolve(iconImageCache[cacheKey]);
  }
  const svgString = encodeURIComponent(
    renderToStaticMarkup(<IconComponent color={color} size={size} />)
  );
  const img = new window.Image();
  img.src = `data:image/svg+xml;utf8,${svgString}`;
  return new Promise((resolve) => {
    img.onload = () => {
      iconImageCache[cacheKey] = img;
      resolve(img);
    };
  });
}

// Type guard para SIMULATION_STOPPED
function isSimulationStopped(response: unknown): response is { type: string; data: string } {
  return (
    typeof response === 'object' &&
    response !== null &&
    'type' in response &&
    (response as { type?: unknown }).type === 'SIMULATION_STOPPED' &&
    'data' in response &&
    typeof (response as { data?: unknown }).data === 'string'
  );
}

// Dibuja el estado de la simulación en el canvas usando íconos
async function drawState(canvas: HTMLCanvasElement, data: any) {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const margin = 40;
  const width = canvas.width;
  const height = canvas.height;
  const gridLength = 140;
  const gridWidth = 100;
  const scaleX = (width - 2 * margin) / gridLength;
  const scaleY = (height - 2 * margin) / gridWidth;

  // Draw current time
  ctx.fillStyle = '#000';
  ctx.font = '18px Arial';
  ctx.fillText('Time: ' + (data.minuto || ''), 20, 30);

  // Draw grid
  ctx.strokeStyle = '#dcdcdc';
  for (let x = 0; x <= gridLength; x += 10) {
    const sx = margin + x * scaleX;
    ctx.beginPath(); ctx.moveTo(sx, margin); ctx.lineTo(sx, height - margin); ctx.stroke();
  }
  for (let y = 0; y <= gridWidth; y += 10) {
    const sy = margin + y * scaleY;
    ctx.beginPath(); ctx.moveTo(margin, sy); ctx.lineTo(width - margin, sy); ctx.stroke();
  }

  // Draw blockages (as connected black lines)
  if (data.bloqueos) {
    ctx.strokeStyle = '#000';
    ctx.lineWidth = 3;
    data.bloqueos.forEach((blockage: any) => {
      if (blockage.segmentos && blockage.segmentos.length > 1) {
        ctx.beginPath();
        blockage.segmentos.forEach((v: any, i: number) => {
          const x = margin + v.posX * scaleX;
          const y = margin + v.posY * scaleY;
          if (i === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }
        });
        ctx.stroke();
        // Draw vertices as small filled circles
        blockage.segmentos.forEach((v: any) => {
          const x = margin + v.posX * scaleX;
          const y = margin + v.posY * scaleY;
          ctx.beginPath();
          ctx.arc(x, y, 4, 0, 2 * Math.PI);
          ctx.fill();
        });
      }
    });
    ctx.lineWidth = 1;
  }

  // Draw warehouses (as icons)
  if (data.almacenes) {
    for (const wh of data.almacenes) {
      const x = margin + wh.posicion.posX * scaleX - 16;
      const y = margin + wh.posicion.posY * scaleY - 16;
      const img = await iconToImage(FaWarehouse, '#444', 32);
      ctx.drawImage(img, x, y, 32, 32);
      // ID
      ctx.fillStyle = '#000';
      ctx.font = '12px Arial';
      ctx.fillText('W' + (wh.idAlmacen || ''), x + 8, y + 40);
      // Capacity bar
      if (wh.maxGLP) {
        const perc = wh.currentGLP / wh.maxGLP;
        ctx.fillStyle = '#c8c8c8';
        ctx.fillRect(x + 2, y + 34, 28, 4);
        ctx.fillStyle = '#00c800';
        ctx.fillRect(x + 2, y + 34, 28 * perc, 4);
      }
    }
  }

  // Draw delivery nodes (as icons)
  if (data.pedidos) {
    for (const node of data.pedidos) {
      const x = margin + node.posX * scaleX - 12;
      const y = margin + node.posY * scaleY - 24;
      const img = await iconToImage(FaMapMarkerAlt, '#ff2d2d', 24);
      ctx.drawImage(img, x, y, 24, 24);
    }
  }

  // Draw vehicles (as icons)
  if (data.vehiculos) {
    for (const v of data.vehiculos) {
      let color = '#ffc800';
      if (v.estado === 'STUCK') color = '#ff0000';
      else if (v.estado === 'MAINTENANCE') color = '#ffa500';
      else color = '#444';
      const vx = margin + v.posicionX * scaleX - 16;
      const vy = margin + v.posicionY * scaleY - 16;
      const img = await iconToImage(FaTruck, color, 32);
      ctx.drawImage(img, vx, vy, 32, 32);
      ctx.fillStyle = '#000';
      ctx.font = '12px Arial';
      ctx.fillText(v.placa || v.idVehiculo || '', vx, vy - 5);
      // Draw path if available
      if (v.rutaActual && v.rutaActual.length > 1 && v.estado !== 'STUCK') {
        ctx.strokeStyle = 'rgba(0,180,0,0.7)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        v.rutaActual.forEach((p: any, i: number) => {
          const px = margin + p.posX * scaleX;
          const py = margin + p.posY * scaleY;
          if (i === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
        });
        ctx.stroke();
        ctx.lineWidth = 1;
      }
    }
  }
}

const SimulationControlPanel: React.FC = () => {
  const [serverUrl, setServerUrl] = useState(defaultServerUrl);
  const [initialTime, setInitialTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  });
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'error'>('disconnected');
  const [log, setLog] = useState<LogEntry[]>([]);
  const [isSimulating, setIsSimulating] = useState(false);
  const stompClient = useRef<Client | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const toast = useToast();
  const { isOpen, onOpen, onClose } = useDisclosure();

  const logMessage = (message: string) => {
    setLog((prev) => [
      ...prev,
      { timestamp: new Date().toLocaleTimeString(), message },
    ]);
  };

  const updateStatus = (s: typeof status) => {
    setStatus(s);
  };

  const connect = () => {
    if (connected) {
      disconnect();
    }
    logMessage(`🔌 Connecting to ${serverUrl}...`);
    const client = new Client({
      brokerURL: undefined,
      webSocketFactory: () => new SockJS(`${serverUrl}/ws`),
      debug: () => {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        updateStatus('connected');
        logMessage('✅ Connected to simulation server');
        client.subscribe('/topic/simulation', (message: IMessage) => {
          try {
            handleMessage(JSON.parse(message.body));
          } catch (error: unknown) {
            if (error instanceof Error) {
              logMessage('❌ Error parsing message: ' + error.message);
            } else {
              logMessage('❌ Error parsing message');
            }
          }
        });
      },
      onStompError: (frame) => {
        setConnected(false);
        updateStatus('error');
        logMessage('❌ Connection error: ' + frame.headers['message']);
      },
      onWebSocketClose: () => {
        setConnected(false);
        updateStatus('disconnected');
        logMessage('🔌 Disconnected');
      },
    });
    stompClient.current = client;
    client.activate();
  };

  const disconnect = () => {
    if (stompClient.current) {
      stompClient.current.deactivate();
      stompClient.current = null;
    }
    setConnected(false);
    updateStatus('disconnected');
    logMessage('🔌 Disconnected');
  };

  const handleMessage = (response: unknown) => {
    if (isSimulationStopped(response)) {
      logMessage('⏹️ ' + response.data);
      setIsSimulating(false);
      const ctx = canvasRef.current?.getContext('2d');
      if (ctx && canvasRef.current) {
        ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
      }
      return;
    }
    logMessage('📝 ' + JSON.stringify(response));
    // Visualización: dibujar en canvas
    if (canvasRef.current && typeof response === 'object' && response !== null) {
      if ('type' in response && (response as any).type === 'SIMULATION_UPDATE' && 'data' in response) {
        drawState(canvasRef.current, (response as any).data);
      } else {
        drawState(canvasRef.current, response);
      }
    }
  };

  const startSimulation = () => {
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    // Convertir initialTime a objeto Time
    const date = new Date(initialTime);
    const timeObj = {
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      day: date.getDate(),
      hour: date.getHours(),
      minute: date.getMinutes(),
    };
    stompClient.current.publish({
      destination: '/app/init',
      body: JSON.stringify({ initialTime: timeObj }),
    });
    logMessage(`🚀 Starting simulation with initial time: ${initialTime}`);
    setIsSimulating(true);
    onClose();
  };

  const stopSimulation = () => {
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    stompClient.current.publish({ destination: '/app/stop', body: '{}' });
    logMessage('⏹️ Sending stop simulation request...');
    setIsSimulating(false);
  };

  const clearLog = () => setLog([]);

  return (
    <Box borderWidth="1px" borderRadius="md" p={4} mb={4}>
      <VStack align="start" spacing={3}>
        <Flex gap={4} align="center">
          <Button colorScheme="green" size="md" onClick={onOpen} isDisabled={isSimulating}>
            Iniciar Simulación
          </Button>
          {isSimulating && (
            <Button colorScheme="red" size="md" onClick={stopSimulation}>
              Detener Simulación
            </Button>
          )}
        </Flex>
        <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
          <ModalOverlay />
          <ModalContent borderRadius="lg" p={2}>
            <ModalHeader fontWeight="bold" fontSize="2xl" color="gray.700">Iniciar Simulación</ModalHeader>
            <ModalBody>
              <VStack spacing={4} align="stretch">
                {!connected && (
                  <FormControl>
                    <FormLabel>Server URL</FormLabel>
                    <HStack>
                      <Input value={serverUrl} onChange={e => setServerUrl(e.target.value)} placeholder="Server URL" />
                      <Button colorScheme="blue" onClick={connect} isDisabled={connected}>Conectar</Button>
                    </HStack>
                    <Text color={status === 'connected' ? 'green.500' : status === 'error' ? 'red.500' : 'gray.500'} fontWeight="bold" mt={2}>
                      Estado: {status.charAt(0).toUpperCase() + status.slice(1)}
                    </Text>
                  </FormControl>
                )}
                <FormControl mt={2}>
                  <FormLabel>Fecha y hora de inicio</FormLabel>
                  <Input type="datetime-local" value={initialTime} onChange={e => setInitialTime(e.target.value)} />
                </FormControl>
              </VStack>
            </ModalBody>
            <ModalFooter>
              <Button onClick={onClose} variant="ghost" mr={3}>Cancelar</Button>
              <Button colorScheme="green" onClick={startSimulation} isDisabled={!connected}>Simular</Button>
            </ModalFooter>
          </ModalContent>
        </Modal>
        <Box>
          <canvas ref={canvasRef} width={1100} height={600} style={{ border: '1px solid #ccc', background: '#fff' }} />
        </Box>
        <Accordion allowToggle w="100%" defaultIndex={[]}> 
          <AccordionItem borderWidth={0}>
            <AccordionButton px={0} _hover={{ bg: 'gray.100' }}>
              <Box flex="1" textAlign="left" fontWeight="semibold">Log</Box>
              <AccordionIcon />
            </AccordionButton>
            <AccordionPanel px={0} pb={2}>
              <Button size="sm" onClick={clearLog} mb={2}>Clear Log</Button>
              <Box id="log" h="150px" overflowY="scroll" borderWidth="1px" p={2} bg="gray.50">
                {log.map((entry, idx) => (
                  <Text key={idx} fontSize="sm">[{entry.timestamp}] {entry.message}</Text>
                ))}
              </Box>
            </AccordionPanel>
          </AccordionItem>
        </Accordion>
      </VStack>
    </Box>
  );
};

export default SimulationControlPanel; 