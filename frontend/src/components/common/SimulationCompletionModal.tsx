import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  Text,
  VStack,
  Flex,
  Button
} from '@chakra-ui/react';

interface SimulationCompleteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onViewDetails?: () => void;
  fechaInicio: string;
  fechaFin: string;
  duracion: string;
  pedidosEntregados?: number;
  consumoPetroleo?: number;
  tiempoPlanificacion?: string;
}

const SimulationCompleteModal: React.FC<SimulationCompleteModalProps> = ({
  isOpen,
  onClose,
  onViewDetails,
  fechaInicio,
  fechaFin,
  duracion,
  pedidosEntregados = 0,
  consumoPetroleo = 0,
  tiempoPlanificacion = "00:00:00"
}) => {
  return (
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
              <strong>Fecha y Hora de inicio:</strong> {fechaInicio}
            </Text>
            <Text>
              <strong>Fecha y Hora de fin:</strong> {fechaFin}
            </Text>
            <Text>
              <strong>Duración:</strong> {duracion}
            </Text>
            <Text>
              <strong>Pedidos entregados:</strong> {pedidosEntregados}
            </Text>
            <Text>
              <strong>Consumo en petróleo:</strong> {consumoPetroleo}
            </Text>
            <Text>
              <strong>Tiempo de planificación:</strong> {tiempoPlanificacion}
            </Text>
          </VStack>

          <Flex justify="space-between" mt={6}>
            {onViewDetails && (
              <Button colorScheme="blue" onClick={onViewDetails}>
                Ver Detalles Completos
              </Button>
            )}
            <Button colorScheme="purple" onClick={onClose}>
              Aceptar
            </Button>
          </Flex>
        </ModalBody>
      </ModalContent>
    </Modal>
  );
};

export default SimulationCompleteModal;