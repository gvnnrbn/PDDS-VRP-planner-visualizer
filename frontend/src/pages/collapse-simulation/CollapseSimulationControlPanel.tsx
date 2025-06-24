import React, { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Box, Button, Input, VStack, useToast, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, FormControl, FormLabel, useDisclosure, Flex, Text } from '@chakra-ui/react';
import { FaTruck, FaWarehouse, FaMapMarkerAlt, FaIndustry } from 'react-icons/fa';
import { renderToStaticMarkup } from 'react-dom/server';
import BottomLeftControls from '../../components/common/MapActions';

const backend_url = import.meta.env.VITE_ENV_BACKEND_URL;

const iconImageCache: Record<string, HTMLImageElement> = {};
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

let panX = 0;
let panY = 0;
let zoomScale = 1;
export const vehicleHitboxes: { x: number; y: number; size: number; vehiculo: any }[] = [];

export async function drawState(canvas: HTMLCanvasElement, data: any): Promise<{
  margin: number;
  scaleX: number;
  scaleY: number;
}> {
  const ctx = canvas.getContext('2d');
  if (!ctx) return { margin: 0, scaleX: 1, scaleY: 1 };
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const margin = 40;
  const width = canvas.width;
  const height = canvas.height;
  const gridLength = 70;
  const gridWidth = 50;
  const scaleX = (width - 2 * margin) / gridLength;
  const scaleY = (height - 2 * margin) / gridWidth;
  ctx.clearRect(0, 0, width, height);
  ctx.save();
  ctx.translate(panX, panY);
  ctx.scale(zoomScale, zoomScale);
  ctx.strokeStyle = 'rgba(220, 220, 220, 0.55)';
  for (let x = 0; x <= gridLength; x++) {
    const sx = margin + x * scaleX;
    ctx.beginPath();
    ctx.moveTo(sx, margin);
    ctx.lineTo(sx, height - margin);
    ctx.stroke();
  }
  for (let y = 0; y <= gridWidth; y++) {
    const sy = margin + y * scaleY;
    ctx.beginPath();
    ctx.moveTo(margin, sy);
    ctx.lineTo(width - margin, sy);
    ctx.stroke();
  }
  if (data.bloqueos) {
    ctx.strokeStyle = '#000';
    ctx.lineWidth = 3;
    data.bloqueos.forEach((blockage: any) => {
      if (blockage.segmentos?.length > 1) {
        ctx.beginPath();
        blockage.segmentos.forEach((v: any, i: number) => {
          const x = margin + v.posX * scaleX;
          const y = margin + v.posY * scaleY;
          i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
        });
        ctx.stroke();
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
  if (data.almacenes) {
    for (const wh of data.almacenes) {
      const x = margin + wh.posicion.posX * scaleX - 16;
      const y = margin + wh.posicion.posY * scaleY - 16;
      const icon = wh.isMain ? FaWarehouse : FaIndustry;
      let color = '#444';
      if (!wh.isMain) {
        color = (wh.currentGLP || 0) === 0 ? '#ff0000' : '#00c800';
      }
      const img = await iconToImage(icon, color, 32);
      ctx.drawImage(img, x, y, 32, 32);
      ctx.fillStyle = '#000';
      ctx.font = '12px Arial';
      ctx.fillText('W' + (wh.idAlmacen || ''), x + 8, y + 40);
      if (wh.maxGLP) {
        const perc = wh.currentGLP / wh.maxGLP;
        ctx.fillStyle = '#c8c8c8';
        ctx.fillRect(x + 2, y + 34, 28, 4);
        ctx.fillStyle = '#00c800';
        ctx.fillRect(x + 2, y + 34, 28 * perc, 4);
      }
    }
  }
  if (data.pedidos) {
    for (const node of data.pedidos.filter((pedido: any) => pedido.estado.toUpperCase() !== 'COMPLETADO')) {
      const x = margin + node.posX * scaleX - 12;
      const y = margin + node.posY * scaleY - 24;
      const img = await iconToImage(FaMapMarkerAlt, '#ff2d2d', 24);
      ctx.drawImage(img, x, y, 24, 24);
    }
  }
  vehicleHitboxes.length = 0;
  if (data.vehiculos) {
    for (const v of data.vehiculos) {
      let color = '#ffc800';
      if (v.estado === 'STUCK') color = '#ff0000';
      else if (v.estado === 'MAINTENANCE') color = '#ffa500';
      else color = '#444';
      const vx = margin + v.posicionX * scaleX;
      const vy = margin + v.posicionY * scaleY;
      vehicleHitboxes.push({
        x: vx - 16,
        y: vy - 16,
        size: 32,
        vehiculo: v,
      });
      const img = await iconToImage(FaTruck, color, 32);
      ctx.save();
      ctx.translate(vx, vy);
      if (v.rutaActual?.length > 1) {
        const next = v.rutaActual[1];
        const dx = next.posX - v.posicionX;
        const dy = next.posY - v.posicionY;
        if (Math.abs(dx) > Math.abs(dy)) {
          if (dx < 0) {
            ctx.scale(-1, 1);
          }
        } else {
          if (dy < 0) {
            ctx.rotate(-Math.PI / 2);
          } else{
            ctx.rotate(Math.PI /2);
          }
        }
      }
      ctx.drawImage(img, -16, -16, 32, 32);
      ctx.restore();
      ctx.fillStyle = '#000';
      ctx.font = '12px Arial';
      ctx.fillText(v.placa || v.idVehiculo || '', vx - 16, vy - 21);
      if (v.rutaActual?.length > 1 && v.estado !== 'STUCK') {
        ctx.strokeStyle = 'rgba(46, 0, 252, 0.7)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        v.rutaActual.forEach((p: any, i: number) => {
          const px = margin + p.posX * scaleX;
          const py = margin + p.posY * scaleY;
          i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
        });
        ctx.stroke();
        ctx.lineWidth = 1;
      }
    }
  }
  ctx.restore();
  return { margin, scaleX, scaleY };
}

export interface CollapseSimulacionMinuto {
  minuto: string;
  almacenes: any[];
  bloqueos: any[];
  incidencias: any[];
  mantenimientos: any[];
  pedidos: any[];
  vehiculos: any[];
}

interface CollapseSimulationControlPanelProps {
  setData?: (data: CollapseSimulacionMinuto) => void;
  data?: CollapseSimulacionMinuto;
}

const CollapseSimulationControlPanel: React.FC<CollapseSimulationControlPanelProps> = ({ setData, data }) => {
  const [initialTime, setInitialTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  });
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'error'>('disconnected');
  const [log, setLog] = useState<{ timestamp: string; message: string }[]>([]);
  const [isSimulating, setIsSimulating] = useState(false);
  const stompClient = useRef<Client | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const toast = useToast();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [scale, setScale] = useState<{ margin: number; scaleX: number; scaleY: number }>({
    margin: 40,
    scaleX: 1,
    scaleY: 1,
  });
  const scaleRef = useRef<{ margin: number; scaleX: number; scaleY: number }>({ margin: 40, scaleX: 1, scaleY: 1 });
  const [selectedVehicle, setSelectedVehicle] = useState<any | null>(null);
  const [vehiclePanelPos, setVehiclePanelPos] = useState<{ left: number; top: number } | null>(null);
  const [estadoVehiculo, setEstadoVehiculo] = useState('');

  useEffect(() => {
    if (!connected) {
      connect();
    }
  }, []);

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
    logMessage(`🔌 Connecting to ${backend_url}...`);
    const client = new Client({
      brokerURL: undefined,
      webSocketFactory: () => new SockJS(`${backend_url}/ws`),
      debug: () => {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        updateStatus('connected');
        logMessage('✅ Connected to simulation server');
        client.subscribe('/topic/collapse-simulation', (message: IMessage) => {
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

  const handleMessage = async (response: unknown) => {
    if (isSimulationStopped(response)) {
      logMessage('⏹️ ' + response.data);
      setIsSimulating(false);
      const ctx = canvasRef.current?.getContext('2d');
      if (ctx && canvasRef.current) {
        ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
      }
      return;
    }
    if (typeof response === 'object' && response !== null && 'type' in response) {
      const typedResponse = response as { type: string; data: any };
      switch (typedResponse.type) {
        case 'SIMULATION_LOADING':
          logMessage('🔄 ' + typedResponse.data);
          return;
        case 'SIMULATION_STARTED':
          logMessage('✅ ' + typedResponse.data);
          setIsSimulating(true);
          return;
        case 'SIMULATION_ERROR':
          logMessage('❌ ' + typedResponse.data);
          setIsSimulating(false);
          return;
        case 'STATE_UPDATED':
          logMessage('🔄 ' + typedResponse.data);
          return;
        case 'SIMULATION_UPDATE':
          if (canvasRef.current) {
            const result = await drawState(canvasRef.current, typedResponse.data);
            if (result) { setScale(result); scaleRef.current = result; }
          }
          setData && setData(typedResponse.data);
          return;
        case 'SIMULATION_STATE':
          if (typeof typedResponse.data === 'boolean') {
            if (typedResponse.data === true) {
              setIsSimulating(true);
            }
          }
          setData && setData(typedResponse.data);
          logMessage('📝 ' + JSON.stringify(response));
          return;
        default:
          logMessage('📝 ' + JSON.stringify(response));
      }
    } else {
      logMessage('📝 ' + JSON.stringify(response));
    }
    if (canvasRef.current && typeof response === 'object' && response !== null) {
      if ('type' in response && (response as any).type === 'SIMULATION_UPDATE' && 'data' in response) {
        const result = await drawState(canvasRef.current, (response as any).data);
        if (result) { setScale(result); scaleRef.current = result; }
      } else {
        const result = await drawState(canvasRef.current, response);
        if (result) { setScale(result); scaleRef.current = result; }
      }
    }
  };

  const startSimulation = () => {
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    const date = new Date(initialTime);
    const timeObj = {
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      day: date.getDate(),
      hour: date.getHours(),
      minute: date.getMinutes(),
    };
    stompClient.current.publish({
      destination: '/app/init-collapse',
      body: JSON.stringify({ initialTime: timeObj }),
    });
    logMessage(`🚀 Starting collapse simulation with initial time: ${initialTime}`);
    setIsSimulating(true);
    onClose();
  };

  const stopSimulation = () => {
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    stompClient.current.publish({ destination: '/app/stop-collapse', body: '{}' });
    logMessage('⏹️ Sending stop collapse simulation request...');
    setIsSimulating(false);
  };

  const clearLog = () => setLog([]);

  const onIniciarSimulacion = () => {
    if (!connected) {
      connect();
    }
    onOpen();
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let isDragging = false;
    let startX = 0;
    let startY = 0;
    let panStartX = 0;
    let panStartY = 0;
    const handleWheel = async (e: WheelEvent) => {
      e.preventDefault();
      const zoomIntensity = 0.1;
      const delta = e.deltaY < 0 ? 1 + zoomIntensity : 1 - zoomIntensity;
      zoomScale = Math.min(Math.max(0.25, zoomScale * delta), 4);
      const result = await drawState(canvas, data);
      if (result) { setScale(result); scaleRef.current = result; }
    };
    const handleMouseDown = (e: MouseEvent) => {
      isDragging = true;
      startX = e.clientX;
      startY = e.clientY;
      panStartX = panX;
      panStartY = panY;
    };
    const handleMouseMove = async (e: MouseEvent) => {
      if (!isDragging) return;
      panX = panStartX + (e.clientX - startX);
      panY = panStartY + (e.clientY - startY);
      const result = await drawState(canvas, data);
      if (result) { setScale(result); scaleRef.current = result; }
    };
    const handleMouseUp = () => {
      isDragging = false;
    };
    const handleClick = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const scaleXcss = canvas.width / rect.width;
      const scaleYcss = canvas.height / rect.height;
      const rawX = (e.clientX - rect.left) * scaleXcss;
      const rawY = (e.clientY - rect.top) * scaleYcss;
      const canvasX = (rawX - panX) / zoomScale;
      const canvasY = (rawY - panY) / zoomScale;
      for (const box of vehicleHitboxes) {
        if (
          canvasX >= box.x &&
          canvasX <= box.x + box.size &&
          canvasY >= box.y &&
          canvasY <= box.y + box.size
        ) {
          setSelectedVehicle(box.vehiculo);
          const { margin, scaleX, scaleY } = scaleRef.current;
          const vx = margin + box.vehiculo.posicionX * scaleX;
          const vy = margin + box.vehiculo.posicionY * scaleY;
          const screenX = (vx + panX) * zoomScale + rect.left;
          const screenY = (vy + panY) * zoomScale + rect.top;
          setVehiclePanelPos({ left: screenX, top: screenY });
          switch(box.vehiculo.estado) {
            case 'STUCK': setEstadoVehiculo('Inmovilizado'); break;
            case 'MAINTENANCE': setEstadoVehiculo('En Mantenimiento'); break;
            case 'IDLE': setEstadoVehiculo('Sin Programación'); break;
            case 'ONTHEWAY': setEstadoVehiculo('En Ruta'); break;
            case 'RETURNING_TO_BASE': setEstadoVehiculo('Regresando a almacén'); break;
            case 'FINISHED': setEstadoVehiculo('Ruta Finalizada'); break;
            default: setEstadoVehiculo('En Ruta'); break;
          }
          return;
        }
      }
      setSelectedVehicle(null);
      setVehiclePanelPos(null);
    };
    canvas.addEventListener('wheel', handleWheel);
    canvas.addEventListener('mousedown', handleMouseDown);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    canvas.addEventListener('click', handleClick);
    return () => {
      canvas.removeEventListener('wheel', handleWheel);
      canvas.removeEventListener('mousedown', handleMouseDown);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
      canvas.removeEventListener('click', handleClick);
    };
  }, [data]);

  return (
    <Box borderWidth="1px" borderRadius="md" p={4} mb={4}>
      <VStack align="start" spacing={3}>
        <Box position="relative" width="100%" height="calc(100vh - 64px)">
          <canvas ref={canvasRef} width={1720} height={1080}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              border: '1px solid #ccc',
              background: '#fff',
              zIndex: 1,
            }}
          />
        </Box>
        {selectedVehicle && vehiclePanelPos && (
          <Box
            position="absolute"
            left={vehiclePanelPos.left}
            top={vehiclePanelPos.top}
            transform="translate(-50%, -120%)"
            bg="white"
            p={3}
            border="1px solid #ccc"
            borderRadius="md"
            boxShadow="lg"
            zIndex={1000}
            minW="200px"
          >
            <Flex justify="space-between" align="center" mb={2}>
              <Text fontWeight="bold">Vehículo</Text>
              <Button size="xs" onClick={() => setSelectedVehicle(null)} variant="ghost" colorScheme="red">
                ✕
              </Button>
            </Flex>
            <Text>ID: {selectedVehicle.idVehiculo}</Text>
            <Text>Placa: {selectedVehicle.placa}</Text>
            <Text>Estado: {estadoVehiculo}</Text>
          </Box>
        )}
        {/* Controles inferiores (Detener + Fecha) */}
        <BottomLeftControls
          variant="date-pause"
          date={`Tiempo: ${data?.minuto || "dd/mm/yyyy"}`}
          onStop={stopSimulation}
          onIniciarSimulacion={onIniciarSimulacion}
          isSimulating={isSimulating}
        />
        <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
          <ModalOverlay />
          <ModalContent borderRadius="lg" p={2}>
            <ModalHeader fontWeight="bold" fontSize="2xl" color="gray.700">Iniciar Simulación</ModalHeader>
            <ModalBody>
              <VStack spacing={4} align="stretch">
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
      </VStack>
    </Box>
  );
};

export default CollapseSimulationControlPanel; 