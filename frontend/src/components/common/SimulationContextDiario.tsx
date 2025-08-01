import {
  createContext,
  useContext,
  useState
} from "react";

import type { ReactNode } from "react";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { VehiculoSimuladoV2 } from "../../core/types/vehiculo";
import type { IndicadoresSimulado } from "../../core/types/indicadores";
import type { AlmacenSimulado } from "../../core/types/almacen";
import type { BloqueoSimulado } from "../../core/types/bloqueos";

export interface MinutoOperacion {
  minuto: string;
  vehiculos: VehiculoSimuladoV2[];
  pedidos?: PedidoSimulado[];
  almacenes: AlmacenSimulado[];
  incidencias: any[];
  mantenimientos: any[];
  indicadores: IndicadoresSimulado;
  bloqueos: BloqueoSimulado [];
}

interface OperacionContextType {
  operationData: MinutoOperacion | undefined;
  setOperationData: (data: MinutoOperacion) => void;
  focusOnPedido: (pedido:PedidoSimulado) => void;
  focusOnVehiculo: (vehiculo: VehiculoSimuladoV2) => void;
  focusOnBloqueo: (bloqueo: BloqueoSimulado) => void;
  highlightedPedidoId: number | null;
}

const OperacionContext = createContext<OperacionContextType | null>(null);

export const useOperacion = () => {
  const ctx = useContext(OperacionContext);
  if (!ctx) throw new Error("useOperacion must be inside OperacionProvider");
  return ctx;
};

interface Props {
  children: ReactNode;
}

export const OperacionProvider = ({
  children,
}: Props) => {
  const [operationData, _setOperationData] = useState<MinutoOperacion>();

  const [highlightedPedidoId, setHighlightedPedidoId] = useState<number | null>(null);

const setOperationData = (data: MinutoOperacion) => {
    // const now = Date.now();
    // if (now - lastSetTimeRef.current >= 5000) {
      _setOperationData(data);
    //   lastSetTimeRef.current = now;
    // }
    //console.dir(data, { depth: null });
  };  

  const focusOnPedido = (pedido: PedidoSimulado) => {
    // Centrar el mapa en la posición del pedido
    const margin = 40;
    const canvasWidth = 1720;
    const canvasHeight = 1080;
    const gridLength = 70;
    const gridWidth = 50;
    const scaleX = (canvasWidth - 2 * margin) / gridLength;
    const scaleY = (canvasHeight - 2 * margin) / gridWidth;
    
    // Calcular la posición del pedido en el canvas
    const pedidoX = margin + pedido.posX * scaleX;
    const pedidoY = margin + pedido.posY * scaleY;
    
    // Centrar el mapa en esa posición
    const centerX = canvasWidth / 2;
    const centerY = canvasHeight / 2;
    
    // Calcular el pan necesario para centrar
    const newPanX = centerX - pedidoX;
    const newPanY = centerY - pedidoY;
    
    // Actualizar las variables globales de pan de forma no bloqueante
    (window as any).globalPanX = newPanX;
    (window as any).globalPanY = newPanY;
    
    // Resaltar el pedido temporalmente
    setHighlightedPedidoId(pedido.idPedido);
    (window as any).highlightedPedidoId = pedido.idPedido;
    
    // Remover el resaltado después de 3 segundos
    setTimeout(() => {
      setHighlightedPedidoId(null);
      (window as any).highlightedPedidoId = null;
    }, 3000);
  };

  const focusOnVehiculo = (vehiculo: VehiculoSimuladoV2) => {
    // Centrar el mapa en la posición del vehículo
    const margin = 40;
    const canvasWidth = 1720;
    const canvasHeight = 1080;
    const gridLength = 70;
    const gridWidth = 50;
    const scaleX = (canvasWidth - 2 * margin) / gridLength;
    const scaleY = (canvasHeight - 2 * margin) / gridWidth;
    
    // Calcular la posición del vehículo en el canvas
    const vehiculoX = margin + vehiculo.posicionX * scaleX;
    const vehiculoY = margin + vehiculo.posicionY * scaleY;
    
    // Centrar el mapa en esa posición
    const centerX = canvasWidth / 2;
    const centerY = canvasHeight / 2;
    
    // Calcular el pan necesario para centrar
    const newPanX = centerX - vehiculoX;
    const newPanY = centerY - vehiculoY;
    
    // Actualizar las variables globales de pan de forma no bloqueante
    (window as any).globalPanX = newPanX;
    (window as any).globalPanY = newPanY;
    
    // Resaltar el vehículo temporalmente
    (window as any).highlightedVehicleId = vehiculo.idVehiculo;
    
    // Remover el resaltado después de 3 segundos
    setTimeout(() => {
      (window as any).highlightedVehicleId = null;
    }, 3000);
  };

  const focusOnBloqueo = (bloqueo: BloqueoSimulado) => {
    const margin = 40;
    const canvasWidth = 1720;
    const canvasHeight = 1080;
    const gridLength = 70;
    const gridWidth = 50;
    const scaleX = (canvasWidth - 2 * margin) / gridLength;
    const scaleY = (canvasHeight - 2 * margin) / gridWidth;

    const punto = bloqueo.segmentos[0]; // primer punto del bloqueo
    const bloqueoX = margin + punto.posX * scaleX;
    const bloqueoY = margin + punto.posY * scaleY;

    const centerX = canvasWidth / 2;
    const centerY = canvasHeight / 2;

    const newPanX = centerX - bloqueoX;
    const newPanY = centerY - bloqueoY;

    (window as any).globalPanX = newPanX;
    (window as any).globalPanY = newPanY;

    // Resaltar por 3 segundos
    (window as any).highlightedBlockId = bloqueo.idBloqueo;
    setTimeout(() => {
      (window as any).highlightedBlockId = null;
    }, 3000);
  };

  return (
    <OperacionContext.Provider
      value={{ 
        operationData, 
        setOperationData, 
        focusOnPedido,
        focusOnVehiculo,
        focusOnBloqueo,
        highlightedPedidoId
      }}
    >
      {children}
    </OperacionContext.Provider>
  );
}; 