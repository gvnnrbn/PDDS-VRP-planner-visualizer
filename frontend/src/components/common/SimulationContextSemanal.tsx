import {
  createContext,
  useContext,
  useRef,
  useState
} from "react";

import type { ReactNode } from "react";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { VehiculoSimuladoV2 } from "../../core/types/vehiculo";
import type { IndicadoresSimulado } from "../../core/types/indicadores";


export interface MinutoSimulacion {
  minuto: string;
  vehiculos: VehiculoSimuladoV2[];
  pedidos?: PedidoSimulado[];
  almacenes: any[];
  incidencias: any[];
  mantenimientos: any[];
  indicadores: IndicadoresSimulado
}

interface SimulationContextType {
  currentMinuteData: MinutoSimulacion | undefined;
  setCurrentMinuteData: (data: MinutoSimulacion) => void;
  focusOnPedido: (pedido: PedidoSimulado) => void;
  focusOnVehiculo: (vehiculo: VehiculoSimuladoV2) => void;
  highlightedPedidoId: number | null;
}

const SimulationContext = createContext<SimulationContextType | null>(null);

export const useSimulation = () => {
  const ctx = useContext(SimulationContext);
  if (!ctx) throw new Error("useSimulation must be inside SimulationProvider");
  return ctx;
};

interface Props {
  children: ReactNode;
}

export const SimulationProvider = ({
  children,
}: Props) => {
  const [currentMinuteData, _setCurrentMinuteData] = useState<MinutoSimulacion>();
  const [highlightedPedidoId, setHighlightedPedidoId] = useState<number | null>(null);
  const lastSetTimeRef = useRef<number>(0);

  const setCurrentMinuteData = (data: MinutoSimulacion) => {
    // const now = Date.now();
    // if (now - lastSetTimeRef.current >= 5000) {
      _setCurrentMinuteData(data);
    //   lastSetTimeRef.current = now;
    // }
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

  return (
    <SimulationContext.Provider
      value={{ 
        currentMinuteData, 
        setCurrentMinuteData, 
        focusOnPedido,
        focusOnVehiculo,
        highlightedPedidoId
      }}
    >
      {children}
    </SimulationContext.Provider>
  );
};
