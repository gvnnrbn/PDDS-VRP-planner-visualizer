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
    
  const minutoPrueba= DataMapaPrueba.chunks[0].simulacion[0].minuto;
  const parseDateString = (dateString: string): Date => {
  const [datePart, timePart] = dateString.split(' ');
  const [day, month, year] = datePart.split('/');
  // Formato ISO para Date constructor: YYYY-MM-DDTHH:MM:SS
  const isoDateString = `${year}-${month}-${day}T${timePart}:00`;
  const parsedDate = new Date(isoDateString);
  // Valida si la fecha fue parseada correctamente
  if (isNaN(parsedDate.getTime())) {
    console.error("Fecha inválida parseada:", dateString);
    return new Date(); // Retorna una fecha por defecto si hay un error
  }
  return parsedDate;
  };

  fechaVisual= parseDateString(DataMapaPrueba.chunks[0].simulacion[0].minuto);
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);
  useEffect(() => {
    console.log('Minuto simulation:', minutoPrueba);
  },[minuto])


  const fechaInicio = new Date(jsonData.fechaInicio);
  
  // ➕ Cálculo de fecha actual (usado por BottomLeftControls)
  const fechaActual = new Date(fechaInicio);
  fechaActual.setMinutes(fechaInicio.getMinutes() + minuto * 75);
  
  // ➕ Cálculo de fecha fin
  const fechaFin = new Date(fechaInicio);
  
    
  
  const displayDate = fechaVisual instanceof Date
  ? `${fechaVisual.toLocaleDateString()} | ${fechaVisual.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    })}`
  : 'Fecha inválida';
  
  //Funciones de acció
  const handleStop = () => {
    setIsPaused(true);
    setSimulacionFinalizada(true); // importante aquí también
    onOpen();
  };

  data=DataMapaPrueba.chunks[0].simulacion[0];
  
  const SPEED_MS_MAPPING: Record<string, number> = {
    'Velocidad x1': 31250,
    'Velocidad x2': 15625,
  };

  const handleSpeedChange = (label: string) => {
    const speed = SPEED_MS_MAPPING[label];
    if (speed) {
      setSpeedMs(speed);
    }
  };
  
  return (
    <div>
      <MapGrid minuto={minuto} data={data} speedMs={speedMs}/>
      <BottomLeftControls variant="full" date={displayDate} 
      onSpeedChange={(s) => {
        if (s in SPEED_MS_MAPPING) {
          setSpeedMs(SPEED_MS_MAPPING[s as keyof typeof SPEED_MS_MAPPING]);
        }
      }}
      onStop={handleStop}/>
      {/* ✅ Modal al finalizar */}
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
