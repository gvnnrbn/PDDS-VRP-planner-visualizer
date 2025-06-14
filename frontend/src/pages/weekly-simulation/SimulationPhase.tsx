import { MapGrid } from '../../components/common/Map'
import { useState, useEffect } from 'react';
import jsonData from "../../data/simulacionV2.json";
import BottomLeftControls from '../../components/common/MapActions';
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalBody,
  Text,
  useDisclosure,
  ModalHeader, VStack, Flex,
  Button
} from "@chakra-ui/react";
import { formatDateTime } from '../../utils/dateFormatter';

interface PhaseProps {
  minuto: number
  // setMinuto: (min: number) => void
  data: any 
  // speedMs: number
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
    // speedMs,
    setSpeedMs,
    // isPaused,
    setIsPaused,
    fechaVisual = new Date(jsonData.fechaInicio) // valor por defecto si no se pasa 
  } : PhaseProps) {
    
    const { isOpen, onOpen, onClose } = useDisclosure();
    const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);
    useEffect(() => {
      console.log('Minuto simulation:', minuto);
    },[minuto])
    const totalMinutos = jsonData.simulacion.length;
    const fechaInicio = new Date(jsonData.fechaInicio);
  
    // ➕ Cálculo de fecha actual (usado por BottomLeftControls)
    const fechaActual = new Date(fechaInicio);
    fechaActual.setMinutes(fechaInicio.getMinutes() + minuto * 75);
  
    // ➕ Cálculo de fecha fin
    const fechaFin = new Date(fechaInicio);
    fechaFin.setDate(fechaInicio.getDate() + totalMinutos - 1);
  
    
    useEffect(() => {
      // console.log(`Minuto actual ${minuto} y total de minutos ${totalMinutos}`);
      if (minuto >= totalMinutos  && !isOpen && !simulacionFinalizada) {
        setSimulacionFinalizada(true);
        onOpen(); // solo una vez
      }
    }, [minuto, totalMinutos, isOpen, simulacionFinalizada]);
  
    const displayDate = `${fechaVisual.toLocaleDateString()} | ${fechaVisual.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    })}`;
  
    //Funciones de acción
  
    const handleSpeedChange = (newSpeed: string) => {
      if (newSpeed === "Velocidad x1") {
        setSpeedMs(31250);
      } else if (newSpeed === "Velocidad x2") {
        setSpeedMs(15625);
      }
    };
  
    const handleStop = () => {
      setIsPaused(true);
      setSimulacionFinalizada(true); // importante aquí también
      onOpen();
    };
  
  return (
    <div>
      <MapGrid minuto={minuto} data={data} />
      <BottomLeftControls variant="full" date={displayDate} onSpeedChange={handleSpeedChange} onStop={handleStop}/>

      {/* ✅ Modal al finalizar */}
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="xl">
        <ModalOverlay bg="blackAlpha.700" />
        <ModalContent bg="white" textAlign="center" p={8}>
          <ModalHeader>
            <Text fontSize="2xl" fontWeight="extrabold">
              ✅ Simulación completada
            </Text>
          </ModalHeader>
          <hr />
          <ModalBody>
            <VStack align="start" spacing={3} fontSize="md">
              <Text>
                <strong>Fecha y Hora de inicio:</strong> {formatDateTime(fechaInicio)}
              </Text>
              <Text>
                <strong>Fecha y Hora de fin:</strong> {formatDateTime(fechaFin)}
              </Text>
              <Text>
                <strong>Duración:</strong> {totalMinutos} días
              </Text>
              <Text>
                <strong>Pedidos entregados:</strong> 504 {/* reemplazar dinámico si deseas */}
              </Text>
              <Text>
                <strong>Consumo en petróleo:</strong> 456 {/* reemplazar dinámico si deseas */}
              </Text>
              <Text>
                <strong>Tiempo de planificación:</strong> 00:25:35 {/* opcional */}
              </Text>
            </VStack>

            <Flex justify="flex-end" mt={6}>
              <Button colorScheme="purple" onClick={onClose}>
                Aceptar
              </Button>
            </Flex>
          </ModalBody>
        </ModalContent>
      </Modal>
    </div>
  );
}
