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

export default function SimulationPhase() {
  const [minuto, setMinuto] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [speedMs, setSpeedMs] = useState(38250); // valor inicial
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);
  const [fechaVisual, setFechaVisual] = useState(new Date(jsonData.fechaInicio));

  const totalMinutos = jsonData.simulacion.length;
  const fechaInicio = new Date(jsonData.fechaInicio);

  // ➕ Cálculo de fecha actual (usado por BottomLeftControls)
  const fechaActual = new Date(fechaInicio);
  fechaActual.setMinutes(fechaInicio.getMinutes() + minuto * 75);

  // ➕ Cálculo de fecha fin
  const fechaFin = new Date(fechaInicio);
  fechaFin.setDate(fechaInicio.getDate() + totalMinutos - 1);

  // ➕ Formateador reutilizable
  const formatDateTime = (fecha: Date) => {
    const dd = String(fecha.getDate()).padStart(2, "0");
    const mm = String(fecha.getMonth() + 1).padStart(2, "0");
    const yyyy = fecha.getFullYear();
    const hh = String(fecha.getHours()).padStart(2, "0");
    const min = String(fecha.getMinutes()).padStart(2, "0");
    const ss = String(fecha.getSeconds()).padStart(2, "0");
    return `${dd}/${mm}/${yyyy} ${hh}:${min}:${ss}`;
  };

  // ➕ Simulación automática
  useEffect(() => {
    const totalMinutos = jsonData.simulacion.length;
    if (minuto >= totalMinutos || isPaused) return;

    // Avanza minuto real
    const interval = setTimeout(() => {
      setMinuto((prev) => prev + 1);
    }, speedMs);

    // Animar tiempo visual
    const fechaInicio = new Date(jsonData.fechaInicio);
    const from = new Date(fechaInicio);
    from.setMinutes(from.getMinutes() + minuto * 75);

    const to = new Date(fechaInicio);
    to.setMinutes(to.getMinutes() + (minuto + 1) * 75);

    const animSteps = 30;
    let step = 0;

    const animInterval = setInterval(() => {
      step++;
      const interpolatedTime = new Date(from.getTime() + ((to.getTime() - from.getTime()) * (step / animSteps)));
      setFechaVisual(interpolatedTime);
      if (step >= animSteps) clearInterval(animInterval);
    }, speedMs / animSteps);

    return () => {
      clearTimeout(interval);
      clearInterval(animInterval);
    };
  }, [minuto, speedMs, isPaused]);

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
      <MapGrid minuto={minuto} data={jsonData} />
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
