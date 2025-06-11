import { MapGrid } from '../../components/common/Map'
import type { BloqueoSimulado } from '../../core/types/bloqueos';

interface PhaseProps {
  data: any;
  currentTime: Date;
  bloqueos: BloqueoSimulado[];
  setSpeedMs: (speed: number) => void;
  setIsPaused: (paused: boolean) => void;
}

export default function SimulationPhase({
  data,
  currentTime,
  bloqueos,
  setSpeedMs,
  setIsPaused,
}: PhaseProps) {
  if (!data || !currentTime) return null;
  
  return (
    <div>     
        <MapGrid minuto={currentTime} data={data} bloqueos={bloqueos} /> 
    </div>
  );
}
