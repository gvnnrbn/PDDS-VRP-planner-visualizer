import {
  createContext,
  useContext,
  useState
} from "react";

import type { ReactNode } from "react";

export interface CollapseMinutoSimulacion {
  minuto: string;
  vehiculos: any[];
  pedidos?: any[];
  almacenes: any[];
  incidencias: any[];
  mantenimientos: any[];
}

interface CollapseSimulationContextType {
  currentMinuteData: CollapseMinutoSimulacion | undefined;
  setCurrentMinuteData: (data: CollapseMinutoSimulacion) => void;
}

const CollapseSimulationContext = createContext<CollapseSimulationContextType | null>(null);

export const useCollapseSimulation = () => {
  const ctx = useContext(CollapseSimulationContext);
  if (!ctx) throw new Error("useCollapseSimulation must be inside CollapseSimulationProvider");
  return ctx;
};

interface Props {
  children: ReactNode;
}

export const CollapseSimulationProvider = ({
  children,
}: Props) => {
  const [currentMinuteData, setCurrentMinuteData] = useState<CollapseMinutoSimulacion>();
  return (
    <CollapseSimulationContext.Provider
      value={{ currentMinuteData, setCurrentMinuteData }}
    >
      {children}
    </CollapseSimulationContext.Provider>
  );
}; 