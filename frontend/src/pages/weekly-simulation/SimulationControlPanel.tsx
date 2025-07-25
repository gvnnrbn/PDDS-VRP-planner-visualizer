import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useNavigate } from 'react-router-dom';
import { Box, Button, Input, useToast, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, FormControl, FormLabel, useDisclosure, Flex, Accordion, AccordionItem, AccordionButton, AccordionPanel, AccordionIcon, Text, VStack } from '@chakra-ui/react';
import { FaTruck, FaWarehouse, FaMapMarkerAlt, FaIndustry } from 'react-icons/fa';
import { renderToStaticMarkup } from 'react-dom/server';
import BottomLeftControls from '../../components/common/MapActions';
import SimulationCompleteModal from '../../components/common/SimulationCompletionModal';
import { ModalInsertAveria } from '../../components/common/modals/ModalInsertAveria';
import type { AlmacenSimulado } from '../../core/types/almacen';
import type { BloqueoSimulado } from '../../core/types/bloqueos';
import type { IncidenciaSimulada } from '../../core/types/incidencia';
import type { MantenimientoSimulado } from '../../core/types/manetenimiento';
import type { PedidoSimulado } from '../../core/types/pedido';
import type { VehiculoSimulado, VehiculoSimuladoV2 } from '../../core/types/vehiculo';
import type { IndicadoresSimulado } from '../../core/types/indicadores';
import AlmacenModal from '../../components/common/modals/ModalAlmacen';
import { format, parseISO, differenceInSeconds, parse } from 'date-fns';


interface LogEntry {
  timestamp: string;
  message: string;
}

const backend_url = import.meta.env.VITE_API_URL;

// Cache global para imágenes de íconos
const iconImageCache: Record<string, HTMLImageElement> = {};

// Helper para obtener un identificador único del ícono
function getIconIdentifier(IconComponent: React.ElementType): string {
  // Intentar obtener displayName o name, si no existe usar el nombre de la función
  return (IconComponent as any).displayName || 
         (IconComponent as any).name || 
         IconComponent.toString().split(' ')[1] || 
         'unknown';
}

// Helper para convertir un ícono de react-icons a imagen para canvas, usando cache
function iconToImage(IconComponent: React.ElementType, color: string, size = 32): Promise<HTMLImageElement> {
  const cacheKey = `${getIconIdentifier(IconComponent)}_${color}_${size}`;
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
    { icon: FaIndustry, colors: ['#444', '#ff0000', '#00c800', '#ffae00'], sizes: [32] },
    { icon: FaMapMarkerAlt, colors: ['#5459EA', '#FFD700'], sizes: [24, 32] }, // Añade los tamaños usados
    { icon: FaTruck, colors: ['#ffc800', '#ff0000', '#ffa500', '#444', '#00c800', '#666565'], sizes: [32] }, // Añade todos los colores usados
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
(window as any).highlightedVehicleId = null;


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
        const glp = wh.currentGLP || 0;
        const perc = wh.maxGLP ? glp / wh.maxGLP : 1;

        if (glp === 0) {
          color = '#ff0000'; // rojo
        } else if (perc <= 0.25) {
          color = '#ffae00'; // ámbar
        } else {
          color = '#00c800'; // verde
        }
      }

      // Obtener imagen del caché (ya precargada)
      const cacheKey = `${getIconIdentifier(icon)}_${color}_${32}`;
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
      // ctx.fillText('W' + (wh.idAlmacen || ''), x + 4, y + 50);

      if (!wh.isMain && wh.maxGLP) {
        const perc = wh.currentGLP / wh.maxGLP;
        ctx.fillStyle = '#c8c8c8';
        ctx.fillRect(x + 2, y + 34, 28, 4);

        if (perc <= 0.25) {
          ctx.fillStyle = '#ffae00'; // ámbar
        } else {
          ctx.fillStyle = '#00c800'; // verde
        }

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

      const cacheKey = `${getIconIdentifier(FaMapMarkerAlt)}_${iconColor}_${iconSize}`;
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
      if (v.estado === 'STUCK' || v.estado === 'REPAIR' || v.estado === 'RETURNING_TO_BASE') color = '#ff0000';
      else if (v.estado === 'MAINTENANCE') color = '#ffa500';
      else if (v.estado === 'ONTHEWAY' ) color = '#444'; // Color para vehículos en movimiento
      else if (v.estado === 'IDLE') color = '#444'; // Color si está inactivo

      const vx = margin + v.posicionX * scaleX;
      const vy = margin + v.posicionY * scaleY;

      vehicleHitboxes.push({
        x: vx - 16,
        y: vy - 16,
        size: 32,
        vehiculo: v,
      });

      // Si este vehículo está resaltado, dibujar un círculo/borde especial
      if (typeof window !== 'undefined' && (window as any).highlightedVehicleId === v.idVehiculo) {
        ctx.save();
        ctx.beginPath();
        ctx.arc(vx, vy, 24, 0, 2 * Math.PI);
        ctx.strokeStyle = '#805ad5';
        ctx.lineWidth = 5;
        ctx.shadowColor = '#805ad5';
        ctx.shadowBlur = 12;
        ctx.setLineDash([]); // Asegurar línea sólida
        ctx.stroke();
        ctx.restore();
      }

      const cacheKey = `${getIconIdentifier(FaTruck)}_${color}_${32}`;
      const img = iconImageCache[cacheKey];

      if (img) {
        ctx.save();
        ctx.translate(vx, vy); // centro

        // Lógica de rotación: usa v.rutaActual[0] para el punto actual
        // y v.rutaActual[1] para el siguiente punto en la ruta.
        if (v.rutaActual?.length > 1) {
          const next = v.rutaActual[1];
          const dx = next.posX - v.posicionX;
          const dy = next.posY - v.posicionY;

          if (Math.abs(dx) > Math.abs(dy)) {
            if (dx < 0) {
              ctx.scale(-1, 1);
              ctx.drawImage(img, -16, -16, 32, 32); // flip horizontal
            } else {
              ctx.drawImage(img, -16, -16, 32, 32); // right
            }
          } else {
            if (dy < 0) {
              ctx.rotate(-Math.PI / 2);
            } else if (dy > 0) {
              ctx.rotate(Math.PI / 2);
            }
            ctx.drawImage(img, -16, -16, 32, 32); // up or down
          }
        } else {
          ctx.drawImage(img, -16, -16, 32, 32); // no route, default
        }

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


export interface SimulacionMinuto {
  minuto: string;
  almacenes: AlmacenSimulado[];
  bloqueos: BloqueoSimulado[];
  incidencias: IncidenciaSimulada[];
  mantenimientos: MantenimientoSimulado[]; 
  pedidos: PedidoSimulado[];
  vehiculos: VehiculoSimulado[];
  indicadores: IndicadoresSimulado;
}

// Interfaz para el estado de animación de cada vehículo
interface VehicleAnimationState {
  prevPosX: number;
  prevPosY: number;
  targetPosX: number;
  targetPosY: number;
  transitionStartTime: number;
  transitionDuration: number;
}

interface SimulationControlPanelProps {
  setData: (data: SimulacionMinuto) => void;
  data: SimulacionMinuto;
  startDate: string; // <-- nuevo
}

const SimulationControlPanel: React.FC<SimulationControlPanelProps & { onVehiculosPorAlmacenUpdate?: (map: Record<number, Record<string, number>>) => void }> = ({ setData, data, startDate, onVehiculosPorAlmacenUpdate }) => {
  const navigate = useNavigate();
  const [initialTime, setInitialTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  });
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState<'connected' | 'disconnected' | 'error'>('disconnected');
  const [isSimulating, setIsSimulating] = useState(false);
  const stompClient = useRef<Client | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const toast = useToast();
  const { isOpen, onOpen, onClose } = useDisclosure();
  
  // Para el resumen de simulación
  const [simulationSummary, setSimulationSummary] = useState<any>(null);
  

  const [scale, setScale] = useState<{ margin: number; scaleX: number; scaleY: number }>({
    margin: 40,
    scaleX: 1,
    scaleY: 1,
  });

  // Estado para guardar el primer valor de fecha/hora de la simulación
  const [simStartDate, setSimStartDate] = useState('');
  const [simEndDate, setSimEndDate] = useState('');

  // Sincroniza isSimulating con el primer data?.minuto recibido
  // simStartDate se establece con initialTime cuando se inicia la simulación
  useEffect(() => {
    if (data?.minuto) {
      if (!isSimulating) {
        setIsSimulating(true);
      }
    }
  }, [data?.minuto, isSimulating]);

  // Cuando se detiene la simulación, limpia el valor
  useEffect(() => {
    if (!isSimulating) {
      setSimStartDate('');
      setSimEndDate(''); // También limpiar la fecha final
    }
  }, [isSimulating]);

  //Carga de iconos
  useEffect(() => {
    preloadIcons().catch(console.error); // Llama a la función de precarga
  }, []); // Se ejecuta solo una vez al montar

  useEffect(() => {
    if (!connected) {
      connect();
    }
  }, []);

  const updateStatus = (s: typeof status) => {
    setStatus(s);
  };

  const connect = () => {
    if (connected) {
      disconnect();
    }
    const client = new Client({
      brokerURL: undefined,
      webSocketFactory: () => new SockJS(`${backend_url}/ws`),
      debug: () => {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        updateStatus('connected');
        client.subscribe('/topic/simulation', (message: IMessage) => {
          try {
            handleMessage(JSON.parse(message.body));
          } catch (error: unknown) {
          }
        });
      },
      onStompError: (frame) => {
        setConnected(false);
        updateStatus('error');
      },
      onWebSocketClose: () => {
        setConnected(false);
        updateStatus('disconnected');
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
  };

  const handleMessage = async (response: unknown) => {
    if (isSimulationStopped(response)) {
      setIsSimulating(false);
      const ctx = canvasRef.current?.getContext('2d');
      if (ctx && canvasRef.current) {
        ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
      }
      return;
    }
    
    // Handle different message types
    if (typeof response === 'object' && response !== null && 'type' in response) {
      const typedResponse = response as { type: string; data: any };
      
      switch (typedResponse.type) {
        case 'SIMULATION_LOADING':
          return;
        case 'SIMULATION_STARTED':
          setIsSimulating(true);
          return;
        case 'SIMULATION_ERROR':
          setIsSimulating(false);
          return;
        case 'STATE_UPDATED':
          return;
        case 'SIMULATION_UPDATE':
          // Handle simulation update data
          if (canvasRef.current) {
            const result = await drawState(canvasRef.current, typedResponse.data);
            if (result) setScale(result);
          }
          setData(typedResponse.data);
          return;
        case 'SIMULATION_STATE':
          if (typeof typedResponse.data === 'boolean') {
            if (typedResponse.data === true) {
            setIsSimulating(true);
            }
          }
          setData(typedResponse.data);
          return;
        case 'SIMULATION_SUMMARY':
          console.log('SimulationControlPanel - Summary data received:', typedResponse.data);
          setSimEndDate(data?.minuto || ''); // Guardar la fecha final antes de detener
          setIsSimulating(false);
          setIsSummaryOpen(true);
          return;
        default:
      }
    } else {
    }
    
    // Visualización: dibujar en canvas
    if (canvasRef.current && typeof response === 'object' && response !== null) {
      if ('type' in response && (response as any).type === 'SIMULATION_UPDATE' && 'data' in response) {
        const result = await drawState(canvasRef.current, (response as any).data);
        if (result) setScale(result);
      } else {
        const result = await drawState(canvasRef.current, response);
        if (result) setScale(result);

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
    setSimStartDate(initialTime); // Establecer la fecha de inicio real
    setIsSimulating(true);
    onClose();
  };

  const stopSimulation = () => {
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    stompClient.current.publish({ destination: '/app/stop', body: '{}' });
  };

  const onIniciarSimulacion = () => {
    if (!connected) {
      connect();
    }
    onOpen();
  };

  // NUEVO: Ref para almacenar el estado de animación de los vehículos
  const vehiclesAnimState = useRef<Record<string, VehicleAnimationState>>({});
  const animationFrameId = useRef<number | null>(null);

   // Función para dibujar el estado actual del canvas
  const redrawCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (canvas && data) {
      // Pasamos los datos originales Y el estado de animación al `drawState`
      // `drawState` usará `vehiclesAnimState.current` para las posiciones
      const result = drawState(canvas, data);
      if (result) setScale(result);
    }
  }, [data]); 

  // Se dispara cada vez que `data` (los datos de simulación) cambian
  useEffect(() => {
    redrawCanvas();
  }, [data, scale.margin, scale.scaleX, scale.scaleY]); // Añadir dependencias de `scale` para re-render si cambian

  // Efecto para iniciar y detener el bucle de animación
  useEffect(() => {
    const animateLoop = () => {
      // Solo dibuja si hay datos Y si la simulación está activa O si algún vehículo está en transición
      if (canvasRef.current && data && (isSimulating || Object.values(vehiclesAnimState.current).some(vAnim => {
        const elapsed = performance.now() - vAnim.transitionStartTime;
        return elapsed < vAnim.transitionDuration;
      }))) {
        redrawCanvas();
      }
      animationFrameId.current = requestAnimationFrame(animateLoop);
    };

    animationFrameId.current = requestAnimationFrame(animateLoop);

    return () => {
      if (animationFrameId.current) {
        cancelAnimationFrame(animationFrameId.current);
      }
    };
  }, [isSimulating, data, redrawCanvas]); // Depende de `isSimulating`, `data` (para vehículos) y `redrawCanvas`


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


  //CLICK VEHICULO
  const [selectedVehicle, setSelectedVehicle] = useState<any | null>(null);
  const [vehiclePanelPos, setVehiclePanelPos] = useState<{ left: number; top: number } | null>(null);
  const [selectedWarehouse, setSelectedWarehouse] = useState<any | null>(null);
  const [warehousePanelPos, setWarehousePanelPos] = useState<{ left: number; top: number } | null>(null);

  const [selectedPedido, setSelectedPedido] = useState<PedidoSimulado | null>(null);
  const [pedidoPanelPos, setPedidoPanelPos] = useState<{ left: number; top: number } | null>(null);

  const clearAllSelections = () => {
    setSelectedVehicle(null);
    setVehiclePanelPos(null);
    setSelectedWarehouse(null);
    setWarehousePanelPos(null);
    setSelectedPedido(null);
    setPedidoPanelPos(null);
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const handleClick = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;

      const canvasX = (clickX - panX) / zoomScale;
      const canvasY = (clickY - panY) / zoomScale;

      const screenPos = (box: { x: number; y: number; size: number }) => ({
        left: (box.x * zoomScale) + panX + rect.left + (box.size / 2) * zoomScale,
        top: (box.y * zoomScale) + panY + rect.top + (box.size / 2) * zoomScale,
      });

      // Vehículos
      for (const box of vehicleHitboxes) {
        if (
          canvasX >= box.x && canvasX <= box.x + box.size &&
          canvasY >= box.y && canvasY <= box.y + box.size
        ) {
          setSelectedVehicle(box.vehiculo);
          setVehiclePanelPos(screenPos(box));
          setSelectedWarehouse(null);
          setSelectedPedido(null);
          return;
        }
      }

      // Almacenes
      for (const box of warehouseHitboxes) {
        if (
          canvasX >= box.x && canvasX <= box.x + box.size &&
          canvasY >= box.y && canvasY <= box.y + box.size
        ) {
          setSelectedWarehouse(box.almacen);
          setWarehousePanelPos(screenPos(box));
          setSelectedVehicle(null);
          setSelectedPedido(null);
          return;
        }
      }

      // Pedidos
      for (const box of pedidoHitboxes) {
        if (
          canvasX >= box.x && canvasX <= box.x + box.size &&
          canvasY >= box.y && canvasY <= box.y + box.size
        ) {
          setSelectedPedido(box.pedido);
          setPedidoPanelPos(screenPos(box));
          setSelectedVehicle(null);
          setSelectedWarehouse(null);
          return;
        }
      }

      clearAllSelections(); // Clic fuera
    };

    canvas.addEventListener('click', handleClick);
    return () => {
      canvas.removeEventListener('click', handleClick);
    };
  }, [canvasRef, data, panX, panY, zoomScale]);


  //Modal averia
  const { isOpen: isOpenAveria, onOpen: onOpenAveria, onClose: onCloseAveria } = useDisclosure();
  const [estadoVehiculo, setEstadoVehiculo] = useState('');
  const [averiaData, setAveriaData] = useState<any>({
      tipo: 'Ti1',
      placa: selectedVehicle ? selectedVehicle.placa : '',
      turno: 'T1',
  })
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
      const [horaStr, minutoStr] = data.minuto.split(" ")[1].split(":");

      const hora = Number(horaStr);
      let turnoActual = 'T1';
      if( hora >= 0 && hora < 8) {
        turnoActual = 'T1';
      }
      else if( hora >= 8 && hora < 16) {
        turnoActual = 'T2';
      }
      else if( hora >= 16 && hora < 24) {
        turnoActual = 'T3';
      }
      setAveriaData({
        ...averiaData,
        placa: selectedVehicle.placa,
        turno: turnoActual,
      });
    }
  }, [selectedVehicle]);
  const registerAveria = () =>{
    if (!connected || !stompClient.current) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    stompClient.current.publish({ 
      destination: '/app/update-failures', 
      body: JSON.stringify({
        vehiclePlaque: averiaData.placa,
        type: averiaData.tipo,
        shiftOccurredOn: averiaData.turno,
      })
    });
    onCloseAveria();
  }

  //Modal almacén
  const { isOpen: isOpenAlmacenRutas, onOpen: onOpenAlmacenRutas, onClose: onCloseAlmacenRutas} = useDisclosure();


  //Modal final
  const [isSummaryOpen, setIsSummaryOpen] = useState(false);


  const handleStopAndShowSummary = () => {
    stopSimulation(); // sigue deteniendo la simulación
  };

  //Conteo de dias
  const [simulatedMinutes, setSimulatedMinutes] = useState(0);
  useEffect(() => {
    if (data?.minuto) {
      setSimulatedMinutes(prev => prev + 1);
    }
  }, [data?.minuto]);
  const diaSimulado = Math.floor(simulatedMinutes / 1440) + 1;

  // Utilidad para parsear fecha de forma robusta
  function safeParse(dateStr: string) {
    let parsed;
    try {
      parsed = parseISO(dateStr);
      if (isNaN(parsed.getTime())) throw new Error('Invalid');
    } catch {
      try {
        parsed = parse(dateStr, 'dd/MM/yyyy HH:mm', new Date());
      } catch {
        parsed = new Date(dateStr);
      }
    }
    return parsed;
  }

  // Función para limpiar el estado y el canvas
  function resetSimulationState() {
    setIsSimulating(false);
    setSimStartDate('');
    setSimEndDate('');
    setData(undefined as any); // o el valor inicial vacío según tu tipo
    if (canvasRef.current) {
      const ctx = canvasRef.current.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
    }
  }

  // Estado para mapear vehículos por almacén
  const [vehiculosPorAlmacen, setVehiculosPorAlmacen] = useState<Record<number, Record<string, number>>>({});
  // Ref para evitar recrear objetos en cada render
  const vehiculosPorAlmacenRef = useRef<Record<number, Record<string, number>>>({});
  // Ref para guardar la última posición conocida de cada vehículo
  const ultimaPosicionVehiculoRef = useRef<Record<string, { posX: number, posY: number } | null>>({});

  useEffect(() => {
    if (!data?.almacenes || !data?.vehiculos) return;
    const nuevoMap: Record<number, Record<string, number>> = { ...vehiculosPorAlmacenRef.current };
    const almacenesPos = Object.fromEntries(data.almacenes.map(a => [a.idAlmacen, a.posicion]));
    for (const vehiculo of data.vehiculos) {
      const placa = vehiculo.placa;
      const posActual = { posX: vehiculo.posicionX, posY: vehiculo.posicionY };
      const posAnterior = ultimaPosicionVehiculoRef.current[placa];
      // Para cada almacén, verifica si el vehículo acaba de entrar
      for (const [idAlmacen, posAlmacen] of Object.entries(almacenesPos)) {
        if (!posAlmacen) continue;
        const mismoAhora = posActual.posX === posAlmacen.posX && posActual.posY === posAlmacen.posY;
        const estabaAntes = posAnterior && posAnterior.posX === posAlmacen.posX && posAnterior.posY === posAlmacen.posY;
        if (mismoAhora && !estabaAntes) {
          if (!nuevoMap[Number(idAlmacen)]) nuevoMap[Number(idAlmacen)] = {};
          if (!nuevoMap[Number(idAlmacen)][placa]) {
            nuevoMap[Number(idAlmacen)][placa] = 1;
          } else {
            nuevoMap[Number(idAlmacen)][placa] += 1;
          }
        }
      }
      // Actualiza la última posición conocida
      ultimaPosicionVehiculoRef.current[placa] = posActual;
    }
    vehiculosPorAlmacenRef.current = nuevoMap;
    setVehiculosPorAlmacen({ ...nuevoMap });
    if (onVehiculosPorAlmacenUpdate) onVehiculosPorAlmacenUpdate({ ...nuevoMap });
  }, [data?.minuto]);

  // Limpia el registro de vehículos por almacén y posiciones al iniciar una nueva simulación
  useEffect(() => {
    if (isSimulating && simStartDate) {
      setVehiculosPorAlmacen({});
      vehiculosPorAlmacenRef.current = {};
      ultimaPosicionVehiculoRef.current = {};
    }
  }, [isSimulating, simStartDate]);

  // Función para calcular duración entre dos fechas
  const calcularDuracion = (fechaInicio: string, fechaFin: string): string => {
    if (!fechaInicio || !fechaFin) return '--:--:--';
    
    try {
      let inicio = safeParse(fechaInicio);
      let actual = safeParse(fechaFin);
      let diff = Math.max(0, actual.getTime() - inicio.getTime());
      const dias = Math.floor(diff / (1000 * 60 * 60 * 24));
      diff -= dias * (1000 * 60 * 60 * 24);
      const horas = Math.floor(diff / (1000 * 60 * 60));
      diff -= horas * (1000 * 60 * 60);
      const minutos = Math.floor(diff / (1000 * 60));
      return `${dias > 0 ? dias + 'd' : ''}${String(horas).padStart(2, '0')}h${String(minutos).padStart(2, '0')}m`;
    } catch (error) {
      return '--:--:--';
    }
  };

  // Calcular duración para mostrar en tiempo real
  let duracionStr = '--:--:--';
  const fechaFin = simEndDate || data?.minuto; 
  if (simStartDate && fechaFin) {
    duracionStr = calcularDuracion(simStartDate, fechaFin);
  }

  // Usar datos reales del resumen de simulación desde el estado global data
  const resumenData = {
    fechaInicio: simStartDate || initialTime,
    fechaFin: simEndDate || data?.minuto || new Date().toISOString().slice(0, 16),
    duracion: calcularDuracion(simStartDate || initialTime, simEndDate || data?.minuto || ''),
    pedidosEntregados: Math.max(0, data?.indicadores?.completedOrders || 0),
    consumoPetroleo: Number((data?.indicadores?.fuelCounterTotal || 0).toFixed(2)),
    tiempoPlanificacion: "00:00:15", 
  };

  // Estado para la posición de la tarjeta de pedido (draggable)
  const [pedidoCardPos, setPedidoCardPos] = useState<{ x: number; y: number } | null>(null);
  const [draggingPedido, setDraggingPedido] = useState(false);
  const dragOffset = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  // Cuando se selecciona un pedido, inicializa la posición en el centro o cerca de la posición original
  useEffect(() => {
    if (selectedPedido && pedidoPanelPos) {
      setPedidoCardPos({ x: pedidoPanelPos.left, y: pedidoPanelPos.top });
    }
  }, [selectedPedido, pedidoPanelPos]);

  // Handlers para drag
  const handlePedidoMouseDown = (e: React.MouseEvent) => {
    if (!pedidoCardPos) return;
    setDraggingPedido(true);
    dragOffset.current = {
      x: e.clientX - pedidoCardPos.x,
      y: e.clientY - pedidoCardPos.y,
    };
    e.preventDefault();
  };

  useEffect(() => {
    if (!draggingPedido) return;
    const handleMouseMove = (e: MouseEvent) => {
      setPedidoCardPos(pos => pos ? ({ x: e.clientX - dragOffset.current.x, y: e.clientY - dragOffset.current.y }) : pos);
    };
    const handleMouseUp = () => setDraggingPedido(false);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingPedido]);

  // Estado para la posición de la tarjeta de vehículo (draggable)
  const [vehicleCardPos, setVehicleCardPos] = useState<{ x: number; y: number } | null>(null);
  const [draggingVehicle, setDraggingVehicle] = useState(false);
  const dragOffsetVehicle = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  useEffect(() => {
    if (selectedVehicle && vehiclePanelPos) {
      setVehicleCardPos({ x: vehiclePanelPos.left, y: vehiclePanelPos.top });
    }
  }, [selectedVehicle, vehiclePanelPos]);

  const handleVehicleMouseDown = (e: React.MouseEvent) => {
    if (!vehicleCardPos) return;
    setDraggingVehicle(true);
    dragOffsetVehicle.current = {
      x: e.clientX - vehicleCardPos.x,
      y: e.clientY - vehicleCardPos.y,
    };
    e.preventDefault();
  };

  useEffect(() => {
    if (!draggingVehicle) return;
    const handleMouseMove = (e: MouseEvent) => {
      setVehicleCardPos(pos => pos ? ({ x: e.clientX - dragOffsetVehicle.current.x, y: e.clientY - dragOffsetVehicle.current.y }) : pos);
    };
    const handleMouseUp = () => setDraggingVehicle(false);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingVehicle]);

  // Estado para la posición de la tarjeta de almacén (draggable)
  const [warehouseCardPos, setWarehouseCardPos] = useState<{ x: number; y: number } | null>(null);
  const [draggingWarehouse, setDraggingWarehouse] = useState(false);
  const dragOffsetWarehouse = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  useEffect(() => {
    if (selectedWarehouse && warehousePanelPos) {
      setWarehouseCardPos({ x: warehousePanelPos.left, y: warehousePanelPos.top });
    }
  }, [selectedWarehouse, warehousePanelPos]);

  const handleWarehouseMouseDown = (e: React.MouseEvent) => {
    if (!warehouseCardPos) return;
    setDraggingWarehouse(true);
    dragOffsetWarehouse.current = {
      x: e.clientX - warehouseCardPos.x,
      y: e.clientY - warehouseCardPos.y,
    };
    e.preventDefault();
  };

  useEffect(() => {
    if (!draggingWarehouse) return;
    const handleMouseMove = (e: MouseEvent) => {
      setWarehouseCardPos(pos => pos ? ({ x: e.clientX - dragOffsetWarehouse.current.x, y: e.clientY - dragOffsetWarehouse.current.y }) : pos);
    };
    const handleMouseUp = () => setDraggingWarehouse(false);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingWarehouse]);

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
        {selectedVehicle && vehiclePanelPos && vehicleCardPos && (
          <Box
            style={{
              position: 'absolute',
              left: vehicleCardPos.x,
              top: vehicleCardPos.y,
              zIndex: 2000,
              minWidth: 220,
              background: 'white',
              border: '1px solid #ccc',
              borderRadius: 8,
              boxShadow: '0 2px 12px rgba(0,0,0,0.18)',
              cursor: draggingVehicle ? 'grabbing' : 'grab',
              userSelect: 'none',
            }}
          >
            <Flex justify="space-between" align="center" mb={0} onMouseDown={handleVehicleMouseDown} style={{ cursor: 'grab', padding: 4, borderBottom: '1px solid #eee', borderTopLeftRadius: 8, borderTopRightRadius: 8, background: '#f7f7fa' }}>
              <Text fontWeight="bold">Vehículo {selectedVehicle.placa}</Text>
              <Button size="xs" onClick={() => setSelectedVehicle(null)} variant="ghost" colorScheme="red">
                ✕
              </Button>
            </Flex>
            <Box p={3} pt={0}>
              <Text color={'purple.100'}>{estadoVehiculo}</Text>
              <Text>Combustible: {selectedVehicle.combustible} Gal.</Text>
              <Text>GLP: {selectedVehicle.currGLP>0?selectedVehicle.currGLP:0} m3</Text>
              <Button variant={'primary'} size={'sm'} onClick={onOpenAveria} mt={2}>
                Registrar Avería
              </Button>
            </Box>
          </Box>
        )}
        {selectedWarehouse && warehousePanelPos && warehouseCardPos && (
          <Box
            style={{
              position: 'absolute',
              left: warehouseCardPos.x,
              top: warehouseCardPos.y,
              zIndex: 2000,
              minWidth: 220,
              background: 'white',
              border: '1px solid #ccc',
              borderRadius: 8,
              boxShadow: '0 2px 12px rgba(0,0,0,0.18)',
              cursor: draggingWarehouse ? 'grabbing' : 'grab',
              userSelect: 'none',
            }}
          >
            <Flex justify="space-between" align="center" mb={0} onMouseDown={handleWarehouseMouseDown} style={{ cursor: 'grab', padding: 4, borderBottom: '1px solid #eee', borderTopLeftRadius: 8, borderTopRightRadius: 8, background: '#f7f7fa' }}>
              {selectedWarehouse.isMain ? (
                <Text fontWeight="bold">Almacén principal</Text>
              ) : (
                <Text fontWeight="bold">
                  {selectedWarehouse.posicion.posX === 42 && selectedWarehouse.posicion.posY === 42
                    ? 'Almacén Norte'
                    : selectedWarehouse.posicion.posX === 63 && selectedWarehouse.posicion.posY === 3
                    ? 'Almacén Este'
                    : 'Almacén Intermedio'}
                </Text>
              )}
              <Button
                size="xs"
                onClick={() => setSelectedWarehouse(null)}
                variant="ghost"
                colorScheme="red"
                mt={2}
              >
                ✕
              </Button>
            </Flex>
            <Box p={3} pt={0}>
              {selectedWarehouse.isMain ? (
                <Text>Capacidad: Infinita</Text>
              ) : (
                <Text>GLP: {selectedWarehouse.currentGLP>0?selectedWarehouse.currentGLP:0}/{selectedWarehouse.maxGLP}</Text>
              )}
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  (window as any).focusAlmacenCard(selectedWarehouse.idAlmacen);
                  setSelectedWarehouse(null);
                }}
                mt={2}
              >
                Rutas de Almacén
              </Button>
            </Box>
          </Box>
        )}
        <AlmacenModal
          isOpen={isOpenAlmacenRutas}
          onClose={onCloseAlmacenRutas}
          almacen={selectedWarehouse}
          onOpenRutas={() => selectedWarehouse && (window as any).focusAlmacenCard(selectedWarehouse.idAlmacen)}
        />

        {selectedPedido && pedidoPanelPos && pedidoCardPos && (
          <Box
            style={{
              position: 'absolute',
              left: pedidoCardPos.x,
              top: pedidoCardPos.y,
              zIndex: 2000,
              minWidth: 220,
              background: 'white',
              border: '1px solid #ccc',
              borderRadius: 8,
              boxShadow: '0 2px 12px rgba(0,0,0,0.18)',
              cursor: draggingPedido ? 'grabbing' : 'grab',
              userSelect: 'none',
            }}
          >
            <Flex justify="space-between" align="center" mb={0} onMouseDown={handlePedidoMouseDown} style={{ cursor: 'grab', padding: 4, borderBottom: '1px solid #eee', borderTopLeftRadius: 8, borderTopRightRadius: 8, background: '#f7f7fa' }}>
              {selectedPedido && (
                <Text fontWeight="bold">
                  Pedido {`PE${selectedPedido.idPedido.toString().padStart(3, '0')}`}
                </Text>
              )}
              <Button size="xs" onClick={() => setSelectedPedido(null)} variant="ghost" colorScheme="red">
                ✕
              </Button>
            </Flex>
            <Box p={3} pt={0}>
              <Text color={'purple.100'}>{selectedPedido.estado}</Text>
              <Text>GLP: {selectedPedido.glp}</Text>
              <Text>Entregar antes de: {selectedPedido.fechaLimite}</Text>
            </Box>
          </Box>
        )}
        <ModalInsertAveria
          isOpen={isOpenAveria}
          onClose={onCloseAveria}
          onSubmit={registerAveria}
          averiaData={averiaData}
          setAveriaData={setAveriaData}
        />
        {/* Controles inferiores (Detener + Fecha) */}
        {(
          <BottomLeftControls
            variant="date-pause"
            date={`Inicio: ${simStartDate ? format(safeParse(simStartDate), 'dd/MM/yyyy HH:mm') : 'dd/mm/yyyy HH:mm'}\nFecha actual: ${data?.minuto ? format(safeParse(data.minuto), 'dd/MM/yyyy HH:mm') : 'dd/mm/yyyy'}\nTiempo Simulado: ${duracionStr}`}
            onStop={handleStopAndShowSummary}
            onIniciarSimulacion={onIniciarSimulacion}
            isSimulating={isSimulating}
            extraBoxStyle={{ fontSize: '1.2rem', minWidth: '320px', minHeight: '80px', padding: '18px 24px' }}
          />
        )}
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

        <SimulationCompleteModal
          isOpen={isSummaryOpen}
          onClose={() => {
            setIsSummaryOpen(false);
            resetSimulationState();
          }}
          onViewDetails={() => {
            setIsSummaryOpen(false);
            resetSimulationState();
            // --- Lógica híbrida para tamaño ---
            const json = JSON.stringify(simulationSummary);
            const sizeInMB = json.length / (1024 * 1024);
            let dataToSend;
            if (sizeInMB < 2) {
              dataToSend = simulationSummary;
            } else {
              const { simulacionCompleta, ...resumenSinDetalle } = simulationSummary || {};
              dataToSend = { ...resumenSinDetalle, historialReducido: true };
            }
            navigate('/weekly-simulation/details', {
              state: { simulationData: dataToSend }
            });
          }}
          {...resumenData}
        />

        {/* <Accordion allowToggle w="100%" defaultIndex={[]}> 
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
        </Accordion> */}
      </VStack>
    </Box>
  );
};

export default SimulationControlPanel; 