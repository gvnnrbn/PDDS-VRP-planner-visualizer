import { Box, Button, FormControl, FormLabel, HStack, Modal, ModalBody, ModalContent, ModalHeader, ModalOverlay, VStack, Input, Select } from "@chakra-ui/react";

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
            <ModalHeader>Registrar Avería {averiaData.placa ? `para ${averiaData.placa}` : ''}</ModalHeader>
            <ModalBody>
                <Box p={4}>
                    <VStack spacing={4} align="stretch">
                        <FormControl>
                            <FormLabel>Placa del vehículo</FormLabel>
                            <Input
                                placeholder="Ej: TD01, TA02, etc."
                                value={averiaData.placa}
                                onChange={(e) => setAveriaData({ ...averiaData, placa: e.target.value })}
                            />
                        </FormControl>
                        
                        <FormControl>
                            <FormLabel>Turno actual de la simulación</FormLabel>
                            <Input
                                value={averiaData.turno === 'T1' ? 'Turno 00:00 - 07:59' : 
                                       averiaData.turno === 'T2' ? 'Turno 08:00 - 15:59' : 
                                       averiaData.turno === 'T3' ? 'Turno 16:00 - 23:59' : 'Turno actual'}
                                isReadOnly
                                bg="gray.50"
                                color="gray.700"
                                fontWeight="medium"
                            />
                        </FormControl>
                        
                        <FormControl>
                            <FormLabel>Tipo de avería</FormLabel>
                            <Select
                                value={averiaData.tipo}
                                onChange={(e) => setAveriaData({ ...averiaData, tipo: e.target.value as 'Ti1' | 'Ti2' | 'Ti3' })}
                            >
                                <option value="Ti1">Tipo 1</option>
                                <option value="Ti2">Tipo 2</option>
                                <option value="Ti3">Tipo 3</option>
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