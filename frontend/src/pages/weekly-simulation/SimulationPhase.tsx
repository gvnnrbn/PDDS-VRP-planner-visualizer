import { MapGrid } from '../../components/common/Map'
import { useState, useEffect } from 'react';
import jsonData from "../../data/simulacionV2.json";
import DataMapaPrueba from "../../data/chunks.json";
import BottomLeftControls from '../../components/common/MapActions';
import {
  useDisclosure, 
} from "@chakra-ui/react";
import SimulationCompleteModal from '../../components/common/SimulationCompletionModal';

interface PhaseProps {
  minuto: number
  // setMinuto: (min: number) => void
  data: any 
  speedMs: number
  setSpeedMs: (speed: number) => void
  // isPaused: boolean
  setIsPaused: (paused: boolean) => void
  fechaVisual: Date
}

export default function SimulationPhase(
  { 
    minuto, 
    // setMinuto,
    data,
    speedMs,
    setSpeedMs,
    // isPaused,
    setIsPaused,
    fechaVisual // valor por defecto si no se pasa 
  } : PhaseProps) {
    
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);

  // Estado actual del índice de simulación
  const [indiceActual, setIndiceActual] = useState(0);

  // Dataset completo
  const simulacion = DataMapaPrueba.chunks[0].simulacion;

  // Datos del paso actual
  const simData = simulacion[indiceActual];
  const minutoPrueba = indiceActual; // puede ser otro valor si tu lógica lo requiere

  const parseDateString = (dateString: string): Date => {
    const [datePart, timePart] = dateString.split(" ");
    const [day, month, year] = datePart.split("/");
    const isoDateString = `${year}-${month}-${day}T${timePart}:00`;
    const parsedDate = new Date(isoDateString);
    if (isNaN(parsedDate.getTime())) {
      console.error("Fecha inválida parseada:", dateString);
      return new Date();
    }
    return parsedDate;
  };

  const [visualDate, setVisualDate] = useState(parseDateString(simData.minuto));

  const TiempoIntervalo=6000;

  // ⏱️ Avanzar al siguiente paso cada 6000 ms
  useEffect(() => {
    const interval = setInterval(() => {
      setIndiceActual((prev) => {
        const siguiente = prev + 1;
        if (siguiente < simulacion.length) {
          setVisualDate(parseDateString(simulacion[siguiente].minuto));
          return siguiente;
        } else {
          setSimulacionFinalizada(true);
          setIsPaused(true);
          onOpen();
          clearInterval(interval);
          return prev; // no avanza más
        }
      });
    }, TiempoIntervalo);

    return () => clearInterval(interval);
  }, [setIsPaused, simulacion, onOpen]);

  const SPEED_MS_MAPPING: Record<string, number> = {
    "Velocidad x1": 31250,
    "Velocidad x2": 15625,
  };

  const displayDate =
    visualDate instanceof Date
      ? `${visualDate.toLocaleDateString()} | ${visualDate.toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        })}`
      : "Fecha inválida";

  const handleStop = () => {
    setIsPaused(true);
    setSimulacionFinalizada(true);
    onOpen();
  };

  return (
    <div>
      <MapGrid minuto={minuto} data={simData} speedMs={TiempoIntervalo} />

      <BottomLeftControls
        variant="full"
        date={displayDate}
        onSpeedChange={(s) => {
          if (s in SPEED_MS_MAPPING) {
            setSpeedMs(SPEED_MS_MAPPING[s as keyof typeof SPEED_MS_MAPPING]);
          }
        }}
        onStop={handleStop}
      />

      <SimulationCompleteModal
        isOpen={isOpen}
        onClose={onClose}
        fechaInicio="10/06/2025 08:00"
        fechaFin="12/06/2025 18:00"
        duracion="3 días"
        pedidosEntregados={504}
        consumoPetroleo={456}
        tiempoPlanificacion="00:25:35"
      />
    </div>
  );
}