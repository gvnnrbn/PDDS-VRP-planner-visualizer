import { MapGrid } from '../../components/common/Map'
import { useState, useEffect, useMemo } from 'react';
import DataMapaPrueba from "../../data/chunks.json";
import BottomLeftControls from '../../components/common/MapActions';
import {
  useDisclosure, 
} from "@chakra-ui/react";
import SimulationCompleteModal from '../../components/common/SimulationCompletionModal';
import type { PedidoSimulado } from '../../core/types/pedido';

import { useSimulation } from '../../components/common/SimulationContextSemanal';

interface PhaseProps {
  //minuto: number
  // setMinuto: (min: number) => void
  data: any 
  speedMs: number
  setSpeedMs: (speed: number) => void
  // isPaused: boolean
  setIsPaused: (paused: boolean) => void
  //fechaVisual: Date
}

export default function SimulationPhase(
  { 
    //minuto, 
    // setMinuto,
    data,
    speedMs,
    setSpeedMs,
    // isPaused,
    setIsPaused,
    //fechaVisual // valor por defecto si no se pasa 
  } : PhaseProps) {
  const { currentMinuteData, simulatedNow } = useSimulation();
  const [pedidosVisibles, setPedidosVisibles] = useState(currentMinuteData.pedidos || []);

  useEffect(() => {
    const visibles = (currentMinuteData.pedidos || []).filter((pedido) => {
      const etas = pedido.vehiculosAtendiendo || [];

      const maxEta = etas.reduce((acc, v) => {
        const etaDate = new Date(v.eta);
        return !acc || etaDate > acc ? etaDate : acc;
      }, null as Date | null);

      if (!maxEta) return true;

      const maxEtaPlus15 = new Date(maxEta.getTime() + 15 * 60 * 1000);
      return simulatedNow < maxEtaPlus15;
    });

    setPedidosVisibles(visibles);
  }, [simulatedNow, currentMinuteData.pedidos]);

  const formattedDate = `${simulatedNow.toLocaleDateString()} | ${simulatedNow.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  })}`;

  return (
    <>
      <MapGrid data={{ ...currentMinuteData, pedidos: pedidosVisibles }} speedMs={speedMs} />
      <BottomLeftControls
        date={formattedDate}
        variant="full"
        onSpeedChange={(label) => {
          const map = { "Velocidad x1": 31250, "Velocidad x2": 15625 };
          if (label in map) setSpeedMs(map[label as keyof typeof map]);
        }}
        onStop={() => {
          setIsPaused(true);
        }}
      />
    </>
  );
}