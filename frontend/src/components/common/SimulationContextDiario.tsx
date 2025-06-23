import {
  createContext,
  useContext,
  useState
} from "react";

import type { ReactNode } from "react";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { VehiculoSimuladoV2 } from "../../core/types/vehiculo";

export interface MinutoOperacion {
  minuto: string;
  vehiculos: VehiculoSimuladoV2[];
  pedidos?: PedidoSimulado[];
  almacenes: any[];
  incidencias: any[];
  mantenimientos: any[];
}

interface OperacionContextType {
  currentMinuteData: MinutoOperacion | undefined;
  setCurrentMinuteData: (data: MinutoOperacion) => void;
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
  const [currentMinuteData, setCurrentMinuteData] = useState<MinutoOperacion>();

  return (
    <OperacionContext.Provider
      value={{ currentMinuteData, setCurrentMinuteData }}
    >
      {children}
    </OperacionContext.Provider>
  );
}; 