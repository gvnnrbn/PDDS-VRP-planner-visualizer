import { MapGrid } from '../../components/common/Map'
import { useState, useEffect } from 'react';
import jsonData from "../../data/simulacion.json";
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
  const [speedMs, setSpeedMs] = useState(5000); // valor inicial
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);

  const totalMinutos = jsonData.simulacion.length;
  const fechaInicio = new Date(jsonData.fechaInicio);

  // ➕ Cálculo de fecha actual (usado por BottomLeftControls)
  const fechaActual = new Date(fechaInicio);
  fechaActual.setDate(fechaInicio.getDate() + minuto);

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

  const displayDate = `Día ${minuto + 1} | ${formatDateTime(fechaActual)} | 11:00`;

  // ➕ Simulación automática
  useEffect(() => {
    console.log(minuto);
    if (isPaused || minuto >= totalMinutos - 1) return;

    const interval = setTimeout(() => {
      setMinuto((prev) => prev + 1);
    }, speedMs);

    return () => clearTimeout(interval);
  }, [minuto, speedMs, isPaused]);

  useEffect(() => {
    if (minuto >= totalMinutos - 1 && !isOpen && !simulacionFinalizada) {
      setSimulacionFinalizada(true);
      onOpen(); // solo una vez
    }
  }, [minuto, totalMinutos, isOpen, simulacionFinalizada]);

  //Funciones de acción

  const handleSpeedChange = (newSpeed: string) => {
    if (newSpeed === "Velocidad x1") {
      setSpeedMs(5000);
    } else if (newSpeed === "Velocidad x2") {
      setSpeedMs(2500);
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
