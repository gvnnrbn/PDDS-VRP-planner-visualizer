import {
  Box, Button, Flex, Text,
  Modal, ModalOverlay, ModalContent,
  ModalHeader, ModalBody, ModalFooter, VStack
} from '@chakra-ui/react';
import React from 'react';

interface AlmacenModalProps {
  isOpen: boolean;
  onClose: () => void;
  almacen: any;
  onOpenRutas: () => void;
}

const AlmacenModal: React.FC<AlmacenModalProps> = ({
  isOpen,
  onClose,
  almacen,
  onOpenRutas,
}) => {
  if (!isOpen || !almacen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="md">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Información del almacén {almacen.idAlmacen}</ModalHeader>
        <ModalBody>
          <VStack spacing={4} align="start">
            <Text><strong>Rutas del Almacén</strong></Text>
            <Text>Falta definir que es lo que se hace :v</Text>
            {/*Aqui va la info de las rutas a definir*/}
          </VStack>
        </ModalBody>
        <ModalFooter justifyContent="space-between">
          <Button colorScheme="purple" onClick={onClose}>
            Cerrar
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default AlmacenModal;