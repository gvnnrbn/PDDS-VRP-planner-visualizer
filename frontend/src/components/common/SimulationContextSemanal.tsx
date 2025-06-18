import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
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
  currentMinuteData: MinutoSimulacion;
  simulatedNow: Date;
  setMinuteData: (data: MinutoSimulacion) => void;
}

const SimulationContext = createContext<SimulationContextType | null>(null);

export const useSimulation = () => {
  const ctx = useContext(SimulationContext);
  if (!ctx) throw new Error("useSimulation must be inside SimulationProvider");
  return ctx;
};

interface Props {
  initialData: MinutoSimulacion;
  tiempoIntervaloMs?: number; // default 30000ms
  tiempoSimuladoMs?: number; // default 75min en ms
  children: ReactNode;
}

export const SimulationProvider = ({
  initialData,
  tiempoIntervaloMs = 60000,
  tiempoSimuladoMs = 75 * 60 * 1000,
  children,
}: Props) => {
  const [currentMinuteData, setCurrentMinuteData] = useState(initialData);
  const [simulatedNow, setSimulatedNow] = useState<Date>(
    parseDate(currentMinuteData.minuto)
  );

  const startDateRef = useRef<Date>(parseDate(currentMinuteData.minuto));
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    let rafId: number;

    const tick = (now: number) => {
      if (!startTimeRef.current) startTimeRef.current = now;
      const elapsed = now - startTimeRef.current;

      const progreso = Math.min(elapsed / tiempoIntervaloMs, 1);
      const nuevaFecha = new Date(
        startDateRef.current.getTime() + tiempoSimuladoMs * progreso
      );
      setSimulatedNow(nuevaFecha);

      if (progreso < 1) {
        rafId = requestAnimationFrame(tick);
      }
    };

    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [currentMinuteData]);

  const setMinuteData = (data: MinutoSimulacion) => {
    setCurrentMinuteData(data);
    const newStartDate = parseDate(data.minuto);
    startDateRef.current = newStartDate;
    startTimeRef.current = null;
    setSimulatedNow(newStartDate);
  };

  return (
    <SimulationContext.Provider
      value={{ currentMinuteData, simulatedNow, setMinuteData }}
    >
      {children}
    </SimulationContext.Provider>
  );
};

const parseDate = (dateStr: string): Date => {
  const [date, time] = dateStr.split(" ");
  const [day, month, year] = date.split("/");
  return new Date(`${year}-${month}-${day}T${time}:00`);
};