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
  fechaInicio: string;
  fechaFin: string;
  duracion: string;
}

const SimulationCollapsedModal: React.FC<SimulationCollapsedModalProps> = ({
  isOpen,
  onClose,
  fechaInicio,
  fechaFin,
  duracion,
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
          <Text fontSize="2xl" fontWeight="extrabold" color="red.500">
            ❌ Simulación colapsada
          </Text>
        </ModalHeader>
        <hr />
        <ModalBody>
          <VStack align="start" spacing={3} fontSize="md">
            <Text>
              <strong>Inicio:</strong> {formatFecha(fechaInicio)}
            </Text>
            <Text>
              <strong>Colapso detectado en:</strong> {formatFecha(fechaFin)}
            </Text>
            <Text>
              <strong>Duración:</strong> {duracion}
            </Text>
          </VStack>

          <Flex justify="center" mt={6}>
            <Button colorScheme="red" onClick={onClose}>
              Cerrar
            </Button>
          </Flex>
        </ModalBody>
      </ModalContent>
    </Modal>
  );
};

export default SimulationCollapsedModal;