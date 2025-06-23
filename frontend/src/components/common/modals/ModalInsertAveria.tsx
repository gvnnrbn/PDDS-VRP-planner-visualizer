import { Box, Button, FormControl, FormLabel, HStack, Modal, ModalBody, ModalContent, ModalHeader, ModalOverlay, Select, VStack } from "@chakra-ui/react";
import IncidenciaForm from "../../IncidenciaForm";
import { useState } from "react";
import { on } from "events";

interface ModalInsertAveriaProps{
    isOpen: boolean;
    onClose: () => void;
    onSubmit: () => void;
    averiaData: any;
    setAveriaData: (data: any) => void;
}
export const ModalInsertAveria = ({ 
    isOpen,
    onClose,
    onSubmit,
    averiaData,
    setAveriaData,
}: ModalInsertAveriaProps) => {
    
  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
            <ModalHeader>Registrar Avería para {averiaData.placa}</ModalHeader>
            <ModalBody>
                <Box p={4}>
                    <VStack spacing={4} align="stretch">
                        <FormControl>
                            <FormLabel>Turno en el que puede ocurrir</FormLabel>
                            <Select
                                value={averiaData.turno}
                                onChange={(e) => setAveriaData({ ...averiaData, turno: e.target.value as 'T1' | 'T2' | 'T3' })}
                                >
                                <option value="T1">Turno 00:00 - 07:59</option>
                                <option value="T2">Turno 08:00 - 15:59</option>
                                <option value="T3">Turno 16:00 - 23:59</option>
                            </Select>
                        </FormControl>
                        <FormControl>
                            <FormLabel>Tipo de avería</FormLabel>
                            <Select
                                value={averiaData.tipo}
                                onChange={(e) => setAveriaData({ ...averiaData, tipo: e.target.value as 'Ti1' | 'Ti2' | 'Ti3' })}
                            >
                                <option value="T1">Tipo 1</option>
                                <option value="T2">Tipo 2</option>
                                <option value="T3">Tipo 3</option>
                            </Select>
                        </FormControl>
                        <HStack spacing={4}>
                            <Button variant={'primary'} onClick={onSubmit}>
                                Registrar
                            </Button>
                            <Button onClick={onClose}>Cancelar</Button>
                        </HStack>
                    </VStack>
                </Box>
            </ModalBody>
        </ModalContent>
    </Modal>
  );
}