import { MapGrid } from '../../components/common/Map'
import { useState, useEffect, useMemo } from 'react';
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

  const simulacion = DataMapaPrueba.chunks[0].simulacion.slice(0, 2);

  const [indiceActual, setIndiceActual] = useState(0);
  const simData = useMemo(() => simulacion[indiceActual], [simulacion, indiceActual]);

  const parseDateString = (dateString: string): Date => {
    const [datePart, timePart] = dateString.split(" ");
    const [day, month, year] = datePart.split("/");
    return new Date(`${year}-${month}-${day}T${timePart}:00`);
  };

  const [visualDate, setVisualDate] = useState(() =>
    parseDateString(simulacion[0].minuto)
  );

  const TiempoIntervalo = 45000;
  const TiempoSimuladoMs = 60 * 60 * 1000;

  // 👇 Avanza hasta el penúltimo índice
  useEffect(() => {
    const timeout = setTimeout(() => {
      const siguiente = indiceActual + 1;

      if (siguiente < simulacion.length) {
        console.log('🕒 Tiempo agotado, avanzando a índice', siguiente);
        setIndiceActual(siguiente);
        setVisualDate(parseDateString(simulacion[siguiente].minuto));
      } else {
        console.log('🏁 Simulación terminada');
        setSimulacionFinalizada(true);
        setIsPaused(true);
        onOpen();
      }
    }, TiempoIntervalo);

    return () => clearTimeout(timeout);
  }, [indiceActual, simulacion, setIsPaused, onOpen]);

  const SPEED_MS_MAPPING: Record<string, number> = {
    "Velocidad x1": 31250,
    "Velocidad x2": 15625,
  };

  const [displayDate, setDisplayDate] = useState<string>("");
  useEffect(() => {
    if (!visualDate) return;

    let frameId: number;
    const start = performance.now();

    const tick = (now: number) => {
      const elapsed = now - start;
      const progreso = Math.min(elapsed / TiempoIntervalo, 1); // 0 a 1
      const tiempoSimulado = TiempoSimuladoMs * progreso;

      const fechaActualSimulada = new Date(visualDate.getTime() + tiempoSimulado);

      const formateado = `${fechaActualSimulada.toLocaleDateString()} | ${fechaActualSimulada.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      })}`;

      setDisplayDate(formateado);

      if (progreso < 1) {
        frameId = requestAnimationFrame(tick);
      }
    };

    frameId = requestAnimationFrame(tick);

    return () => cancelAnimationFrame(frameId);
  }, [visualDate, TiempoIntervalo]);

  const handleStop = () => {
    setIsPaused(true);
    setSimulacionFinalizada(true);
    onOpen();
  };

  const handleCloseModal = () => {
    console.log('🔒 Cerrar modal');
    setSimulacionFinalizada(false);   // Desactiva flag
    onClose();                         // Cierra modal
    setIndiceActual(0);               // (opcional) reinicia la simulación
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

      {<SimulationCompleteModal
        isOpen={isOpen}
        onClose={handleCloseModal}
        fechaInicio="10/06/2025 08:00"
        fechaFin="12/06/2025 18:00"
        duracion="3 días"
        pedidosEntregados={504}
        consumoPetroleo={456}
        tiempoPlanificacion="00:25:35"
      />}

      <button onClick={() => {
        const siguiente = indiceActual + 1;
        console.log('👉 Botón: Avanzar a', siguiente);
        setIndiceActual(siguiente);
      }}>Avanzar 1 paso</button>
    </div>
  );
}