import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  VStack,
  Text,
  Button,
  Flex,
} from "@chakra-ui/react";
import { format, parseISO } from "date-fns";

interface SimulationCollapsedModalProps {
  isOpen: boolean;
  onClose: () => void;
  onViewDetails?: () => void;
  fechaInicio: string;
  fechaFin: string;
  duracion: string;
  tipo: 'error' | 'stopped' | null;
}

const SimulationCollapsedModal: React.FC<SimulationCollapsedModalProps> = ({
  isOpen,
  onClose,
  onViewDetails,
  fechaInicio,
  fechaFin,
  duracion,
  tipo,
}) => {
  function formatFecha(fecha: string) {
    try {
      return format(parseISO(fecha), "dd/MM/yyyy HH:mm");
    } catch {
      return fecha || "";
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="xl">
      <ModalOverlay bg="blackAlpha.700" />
      <ModalContent bg="white" textAlign="center" p={8}>
        <ModalHeader>
          <Text fontSize="2xl" fontWeight="extrabold">
            {tipo === 'error' ? '❌ Simulación Colapsada' : 'Simulación Detenida'}
          </Text>
        </ModalHeader>
        <hr />
        <ModalBody>
          <VStack align="start" spacing={3} fontSize="md">
            <Text>
              <strong>Fecha y Hora de Inicio:</strong> {formatFecha(fechaInicio)}
            </Text>
            <Text>
              <strong>{tipo === 'error' ? 'Colapso detectado el día:' : 'Simulación detenida el día:'}</strong> {formatFecha(fechaFin)}
            </Text>
            <Text>
              <strong>Duración:</strong> {duracion}
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

export default SimulationCollapsedModal;