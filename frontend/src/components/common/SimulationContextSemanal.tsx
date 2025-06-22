import {
  createContext,
  useContext,
  useState
} from "react";

import type { ReactNode } from "react";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { VehiculoSimuladoV2 } from "../../core/types/vehiculo";


export interface MinutoSimulacion {
  minuto: string;
  vehiculos: VehiculoSimuladoV2[];
  pedidos?: PedidoSimulado[];
  almacenes: any[];
  incidencias: any[];
  mantenimientos: any[];
}

interface SimulationContextType {
  currentMinuteData: MinutoSimulacion | undefined;
  setCurrentMinuteData: (data: MinutoSimulacion) => void;
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
  const [currentMinuteData, setCurrentMinuteData] = useState<MinutoSimulacion>();
  

  return (
    <SimulationContext.Provider
      value={{ currentMinuteData, setCurrentMinuteData }}
    >
      {children}
    </SimulationContext.Provider>
  );
};
