import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Box, Button, VStack, useToast, useDisclosure, Flex, Text } from '@chakra-ui/react';
import { FaTruck, FaWarehouse, FaMapMarkerAlt, FaIndustry } from 'react-icons/fa';
import { renderToStaticMarkup } from 'react-dom/server';
import BottomLeftControls from '../../components/common/MapActions';
import { ModalInsertAveria } from '../../components/common/modals/ModalInsertAveria';

function panelStyles({ left, top }: { left: number; top: number }) {
  return {
    position: 'absolute',
    left,
    top,
    transform: 'translate(-50%, -120%)',
    background: 'white',
    padding: '12px',
    border: '1px solid #ccc',
    borderRadius: '6px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
    zIndex: 1000,
    minWidth: '200px',
  };
}


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
    img.onerror = (error) => { // Añadir manejo de error
      console.error("Error cargando SVG para el ícono:", cacheKey, error);
      // Opcional: Resolver con una imagen de marcador de posición si falla la carga
      resolve(new Image()); // Resuelve con una imagen vacía para no bloquear
    };
  });
}

// --- NUEVA FUNCIÓN: precargar todos los íconos posibles ---
export async function preloadIcons(): Promise<void> {
  const iconSets = [
    { icon: FaWarehouse, colors: ['#444', '#000'], sizes: [32] },
    { icon: FaIndustry, colors: ['#444', '#ff0000', '#00c800'], sizes: [32] },
    { icon: FaMapMarkerAlt, colors: ['#5459EA', '#FFD700'], sizes: [24, 32] }, // Añade los tamaños usados
    { icon: FaTruck, colors: ['#ffc800', '#ff0000', '#ffa500', '#444'], sizes: [32] }, // Añade los colores usados
  ];

  const promises: Promise<HTMLImageElement>[] = [];
  for (const set of iconSets) {
    for (const color of set.colors) {
      for (const size of set.sizes) {
        promises.push(iconToImage(set.icon, color, size));
      }
    }
  }
  await Promise.all(promises);
  console.log("Todos los íconos precargados y cacheados.");
}


//VARIBLES PARA ZOOM Y PAN
export let panX = 0;
export let panY = 0;
export let zoomScale = 1;
export const vehicleHitboxes: { x: number; y: number; size: number; vehiculo: any }[] = [];
export const warehouseHitboxes: { x: number; y: number; size: number; almacen: any }[] = [];
export const pedidoHitboxes: { x: number; y: number; size: number; pedido: any }[] = [];

// Funciones para actualizar pan y zoom desde fuera
export function setPan(x: number, y: number) {
    panX = x;
    panY = y;
}

export function setZoom(scale: number) {
    zoomScale = scale;
}

// Variables globales para el enfoque de pedidos
(window as any).panX = panX;
(window as any).panY = panY;
(window as any).highlightedPedidoId = null;

// Dibuja el estado de la simulación en el canvas usando íconos
export function drawState(canvas: HTMLCanvasElement, data: any): {
  margin: number;
  scaleX: number;
  scaleY: number;
} {
  const ctx = canvas.getContext('2d');
  if (!ctx) return { margin: 0, scaleX: 1, scaleY: 1 };

  // --- Optimización: Limpieza y Transformación al inicio ---
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.save();
  ctx.translate(panX, panY);
  ctx.scale(zoomScale, zoomScale);

  const margin = 40;
  const width = canvas.width;
  const height = canvas.height;
  const gridLength = 70;
  const gridWidth = 50;
  const scaleX = (width - 2 * margin) / gridLength;
  const scaleY = (height - 2 * margin) / gridWidth;

  // --- Dibujar la Cuadrícula ---
  ctx.strokeStyle = 'rgba(0, 0, 0, 0.35)';
  ctx.lineWidth = 1; // Restaurar lineWidth a 1 antes de dibujar la cuadrícula
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

  //Limpieza de hitboxes
  vehicleHitboxes.length = 0;
  warehouseHitboxes.length = 0;
  pedidoHitboxes.length = 0;

  // --- Dibujar Bloqueos ---
  if (data.bloqueos) {
    ctx.strokeStyle = '#F80707';
    ctx.lineWidth = 3;
    ctx.fillStyle = '#F80707'; // Para los círculos de los bloqueos
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
  }

  let mainWHx = 0;
  let mainWHy = 0;

  // --- Dibujar Almacenes ---
  if (data.almacenes) {
    for (const wh of data.almacenes) {
      const x = margin + wh.posicion.posX * scaleX - 16;
      const y = margin + wh.posicion.posY * scaleY - 16;
       warehouseHitboxes.push({
        x,
        y,
        size: 32,
        almacen: wh,
      });

      const icon = wh.isMain ? FaWarehouse : FaIndustry;
      let color = '#444';
      if (wh.isMain) {
        color = '#000';
        mainWHx = wh.posicion.posX;
        mainWHy = wh.posicion.posY;
      } else {
        color = (wh.currentGLP || 0) === 0 ? '#ff0000' : '#00c800';
      }

      // Obtener imagen del caché (ya precargada)
      const cacheKey = `${icon.displayName || icon.name || ''}_${color}_${32}`;
      const img = iconImageCache[cacheKey]; // Ahora no hay `await` aquí

      // Si este almacén está resaltado, dibujar un círculo/borde especial
      if (typeof window !== 'undefined' && (window as any).highlightedWarehouseId === wh.idAlmacen) {
        ctx.save();
        ctx.beginPath();
        ctx.arc(x + 16, y + 16, 24, 0, 2 * Math.PI);
        ctx.strokeStyle = '#805ad5';
        ctx.lineWidth = 5;
        ctx.shadowColor = '#805ad5';
        ctx.shadowBlur = 12;
        ctx.stroke();
        ctx.restore();
      }

      if (img) { // Solo dibujar si la imagen está disponible
        ctx.drawImage(img, x, y, 32, 32);
      } else {
        console.warn(`Icono no encontrado en caché para ${cacheKey}`);
        // Considerar dibujar un placeholder o un círculo si la imagen no está lista
      }

      ctx.fillStyle = '#000';
      ctx.font = '12px Arial';
      ctx.fillText('W' + (wh.idAlmacen || ''), x + 4, y + 50);

      if (!wh.isMain && wh.maxGLP) {
        const perc = wh.currentGLP / wh.maxGLP;
        ctx.fillStyle = '#c8c8c8';
        ctx.fillRect(x + 2, y + 34, 28, 4);
        ctx.fillStyle = '#00c800';
        ctx.fillRect(x + 2, y + 34, 28 * perc, 4);
      }
    }
  }

  // --- Dibujar Nodos de Pedido ---
  if (data.pedidos) {
    for (const node of data.pedidos.filter((pedido: any) => pedido.estado.toUpperCase() !== 'COMPLETADO')) {
      const x = margin + node.posX * scaleX - 12;
      const y = margin + node.posY * scaleY - 24;
      pedidoHitboxes.push({
        x,
        y,
        size: 24,
        pedido: node,
      });

      const isHighlighted = (window as any).highlightedPedidoId === node.idPedido;
      const iconColor = isHighlighted ? '#FFD700' : '#5459EA';
      const iconSize = isHighlighted ? 32 : 24;

      const cacheKey = `${FaMapMarkerAlt.displayName || FaMapMarkerAlt.name || ''}_${iconColor}_${iconSize}`;
      const img = iconImageCache[cacheKey]; // No await

      if (img) {
        if (isHighlighted) {
          ctx.save();
          ctx.shadowColor = '#FFD700';
          ctx.shadowBlur = 15;
          ctx.shadowOffsetX = 0;
          ctx.shadowOffsetY = 0;
          ctx.drawImage(img, x - 4, y - 4, iconSize, iconSize);
          ctx.restore();
        } else {
          ctx.drawImage(img, x, y, iconSize, iconSize);
        }
      }

      ctx.fillStyle = '#000';
      ctx.font = '10px Arial';
      ctx.fillText(`GLP: ${node.glp || 0}`, x + 2, y + 40);
    }
  }

  // --- Dibujar Vehículos ---
  if (data.vehiculos) {
    for (const v of data.vehiculos) {
      if (v.posicionX === mainWHx && v.posicionY === mainWHy) {
        continue;
      }
      let color = '#444'; // Color por defecto
      if (v.estado === 'STUCK') color = '#ff0000';
      else if (v.estado === 'MAINTENANCE') color = '#ffa500';
      else if (v.estado === 'En Ruta' || v.estado === 'MOVIENDOSE' || v.estado === 'RETURNING_TO_BASE') color = '#00c800'; // Color para vehículos en movimiento
      else if (v.estado === 'IDLE') color = '#ffc800'; // Color si está inactivo


      const vx = margin + v.posicionX * scaleX;
      const vy = margin + v.posicionY * scaleY;

      vehicleHitboxes.push({
        x: vx - 16,
        y: vy - 16,
        size: 32,
        vehiculo: v,
      });

      const cacheKey = `${FaTruck.displayName || FaTruck.name || ''}_${color}_${32}`;
      const img = iconImageCache[cacheKey];

      if (img) {
        ctx.save();
        ctx.translate(vx, vy); // centro

        // Lógica de rotación: usa v.rutaActual[0] para el punto actual
        // y v.rutaActual[1] para el siguiente punto en la ruta.
        if (v.rutaActual?.length > 1) {
          const next = v.rutaActual[1]; // El siguiente punto en la ruta
          const dx = next.posX - v.posicionX; // Diferencia con el punto actual del vehículo
          const dy = next.posY - v.posicionY;

          if (Math.abs(dx) > Math.abs(dy)) {
            // Movimiento horizontal
            if (dx < 0) {
              ctx.scale(-1, 1); // Flip horizontal para izquierda
            }
            // Si va a la derecha, no hacemos nada (rotación base)
          } else {
            // Movimiento vertical
            if (dy < 0) {
              ctx.rotate(-Math.PI / 2); // Rotar 90 grados a la izquierda para arriba
            } else if (dy > 0) {
              ctx.rotate(Math.PI / 2); // Rotar 90 grados a la derecha para abajo
            }
          }
        }

        ctx.drawImage(img, -16, -16, 32, 32);
        ctx.restore();
      } else {
        console.warn(`Icono de camión no encontrado en caché para ${cacheKey}`);
      }

      ctx.fillStyle = '#444';
      ctx.font = '12px Arial';
      ctx.fillText(v.placa || v.idVehiculo || '', vx - 16, vy - 21);

      // --- CORRECCIÓN AQUÍ: Dibujar la ruta del vehículo ---
      // Si la ruta tiene más de un punto (es decir, hay un segmento que dibujar)
      if (v.rutaActual?.length > 1 && v.estado !== 'STUCK') {
        ctx.strokeStyle = '#2b2661';
        ctx.lineWidth = 3;
        ctx.setLineDash([5, 10]);
        ctx.beginPath();
        // Iterar directamente sobre v.rutaActual
        v.rutaActual.forEach((p: any, i: number) => {
          const px = margin + p.posX * scaleX;
          const py = margin + p.posY * scaleY;
          // Mover al primer punto, luego dibujar líneas a los siguientes
          i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
        });
        ctx.stroke();
      }
      ctx.lineWidth = 1; // Restaurar el grosor de línea para otros dibujos
    }
  }
  ctx.restore(); // Restaurar el contexto global al final
  return { margin, scaleX, scaleY };
}
// --- Main Control Panel ---
const backend_url = import.meta.env.VITE_ENV_BACKEND_URL;

interface DailyOperationControlPanelProps {
  setData: (data: any) => void;
  data: any;
}

const DailyOperationControlPanel: React.FC<DailyOperationControlPanelProps> = ({ setData, data }) => {
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'error'>('disconnected');
  const [log, setLog] = useState<{ timestamp: string; message: string }[]>([]);
  const stompClient = useRef<Client | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const toast = useToast();
  const [scale, setScale] = useState<{ margin: number; scaleX: number; scaleY: number }>({
    margin: 40,
    scaleX: 1,
    scaleY: 1,
  });
  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
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
        logMessage('✅ Connected to daily operation server');
        client.subscribe('/topic/daily', (message: IMessage) => {
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
    // Only handle SIMULATION_UPDATE and direct data
    if (typeof response === 'object' && response !== null && 'type' in response) {
      const typedResponse = response as { type: string; data: any };
      switch (typedResponse.type) {
        case 'SIMULATION_UPDATE':
          if (canvasRef.current) {
            const result = await drawState(canvasRef.current, typedResponse.data);
            if (result) setScale(result);
          }
          setData(typedResponse.data);
          return;
        case 'ERROR':
          logMessage('❌ ' + typedResponse.data);
          toast({ title: 'Error', description: typedResponse.data, status: 'error', duration: 4000 });
          return;
        default:
          logMessage('📝 ' + JSON.stringify(response));
      }
    } else {
      logMessage('📝 ' + JSON.stringify(response));
    }
    if (canvasRef.current && typeof response === 'object' && response !== null) {
      const result = await drawState(canvasRef.current, response);
      if (result) setScale(result);
    }
  };

  // Función para dibujar el estado actual del canvas
    const redrawCanvas = useCallback(async () => {
      const canvas = canvasRef.current;
      if (canvas && data) {
        // Pasamos los datos originales Y el estado de animación al `drawState`
        // `drawState` usará `vehiclesAnimState.current` para las posiciones
        const result = drawState(canvas, data);
        if (result) setScale(await result);
      }
    }, [data]); 

  useEffect(() => {
      redrawCanvas();
    }, [data, scale.margin, scale.scaleX, scale.scaleY]);

  //Carga de iconos
  useEffect(() => {
    preloadIcons().catch(console.error); // Llama a la función de precarga
  }, []); // Se ejecuta solo una vez al montar  

  //ZOOM Y PAN
    useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;
  
      const resizeCanvas = () => {
        const parent = canvas.parentElement;
        if (parent) {
          canvas.width = parent.offsetWidth;
          canvas.height = parent.offsetHeight;
          redrawCanvas();
        }
      };
  
      resizeCanvas();
      window.addEventListener('resize', resizeCanvas);
  
      let isDragging = false;
      let lastX = 0;
      let lastY = 0;
  
      const handleWheel = (e: WheelEvent) => {
        e.preventDefault();
        const scaleAmount = 1.1;
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
  
        const worldX = (mouseX - panX) / zoomScale;
        const worldY = (mouseY - panY) / zoomScale;
  
        const newZoomScale = e.deltaY < 0 ? zoomScale * scaleAmount : zoomScale / scaleAmount;
        setZoom(Math.min(Math.max(0.25, newZoomScale), 4));
  
        const newPanX = mouseX - worldX * zoomScale;
        const newPanY = mouseY - worldY * zoomScale;
        setPan(newPanX, newPanY);
  
        redrawCanvas();
      };
  
      const handleMouseDown = (e: MouseEvent) => {
        isDragging = true;
        lastX = e.clientX;
        lastY = e.clientY;
      };
  
      const handleMouseMove = (e: MouseEvent) => {
        if (!isDragging) return;
        const dx = e.clientX - lastX;
        const dy = e.clientY - lastY;
        setPan(panX + dx, panY + dy);
        lastX = e.clientX;
        lastY = e.clientY;
        redrawCanvas();
      };
  
      const handleMouseUp = () => {
        isDragging = false;
      };
  
      canvas.addEventListener('wheel', handleWheel);
      canvas.addEventListener('mousedown', handleMouseDown);
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
  
      return () => {
        canvas.removeEventListener('wheel', handleWheel);
        canvas.removeEventListener('mousedown', handleMouseDown);
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseup', handleMouseUp);
        window.removeEventListener('resize', resizeCanvas);
      };
    }, [redrawCanvas]); // Ahora depende de redrawCanvas
  
    //AUXILIAR
    useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;
  
      // Solo una vez al montar
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    }, []);


  // Vehicle panel logic (copied, but can be refactored later)
  const [selectedVehicle, setSelectedVehicle] = useState<any | null>(null);
  const [vehiclePanelPos, setVehiclePanelPos] = useState<{ left: number; top: number } | null>(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
  }, []);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
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
          const { margin, scaleX, scaleY } = scale;
          const vx = margin + box.vehiculo.posicionX * scaleX;
          const vy = margin + box.vehiculo.posicionY * scaleY;
          const screenX = (vx + panX) * zoomScale + rect.left;
          const screenY = (vy + panY) * zoomScale + rect.top;
          setVehiclePanelPos({ left: screenX, top: screenY });
          return;
        }
      }
      setSelectedVehicle(null);
      setVehiclePanelPos(null);
    };
    canvas.addEventListener('click', handleClick);
    return () => {
      canvas.removeEventListener('click', handleClick);
    };
  }, [canvasRef, data, scale]);
  // Modal averia (UI only, no network)
  const { isOpen: isOpenAveria, onOpen: onOpenAveria, onClose: onCloseAveria } = useDisclosure();
  const [estadoVehiculo, setEstadoVehiculo] = useState('');
  const [averiaData, setAveriaData] = useState<any>({
    turno: 'T1',
    tipo: 'Ti1',
    placa: selectedVehicle ? selectedVehicle.placa : '',
  });
  useEffect(() => {
    if (selectedVehicle) {
      switch(selectedVehicle.estado) {
        case 'STUCK':
          setEstadoVehiculo('Inmovilizado');
          break;
        case 'MAINTENANCE':
          setEstadoVehiculo('En Mantenimiento');
          break;
        case 'IDLE':
          setEstadoVehiculo('Sin Programación');
          break;
        case 'ONTHEWAY':
          setEstadoVehiculo('En Ruta');
          break;
        case 'RETURNING_TO_BASE':
          setEstadoVehiculo('Regresando a almacén');
          break;
        case 'FINISHED':
          setEstadoVehiculo('Ruta Finalizada');
          break;
        default:
          setEstadoVehiculo('En Ruta');
          break;
      }
      setAveriaData({
        ...averiaData,
        placa: selectedVehicle.placa,
      });
    }
  }, [selectedVehicle]);
  // --- Render ---
  return (
    <Box borderWidth="1px" borderRadius="md" p={0} mb={0} height="100vh">
      <VStack align="start" spacing={3}>
        <Box position="relative" width="100%" height="100vh">
          <canvas ref={canvasRef} width={1720} height={1080} 
            style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            border: '1px solid #ccc',
            background: '#fff',
            zIndex: 1,}} />
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
            <Button variant={'primary'} size={'sm'} onClick={onOpenAveria} mt={2}>
              Registrar Avería
            </Button>
          </Box>
        )}
        <ModalInsertAveria
          isOpen={isOpenAveria}
          onClose={onCloseAveria}
          onSubmit={() => {}}
          averiaData={averiaData}
          setAveriaData={setAveriaData}
        />
        <BottomLeftControls
          variant="date-pause"
          date={`Fecha: ${data?.minuto || "dd/mm/yyyy"}`}
          onStop={() => {}}
          onIniciarSimulacion={() => {}}
          isSimulating={true}
          extraBoxStyle={{ fontSize: '1.2rem', minWidth: '320px', minHeight: '10px', padding: '14px 32px' }}
        />
      </VStack>
    </Box>
  );
};

export default DailyOperationControlPanel; 