import React, { useState} from "react";
import { Box,Flex,Button,Collapse,Text,useColorModeValue,VStack } from "@chakra-ui/react";
import { ChevronDownIcon, ChevronUpIcon } from '@chakra-ui/icons';


type BottomControlsVariant = "date-pause" | "full";

interface BottomLeftControlsProps {
  variant?: BottomControlsVariant;
  date?: string;
  onStop?: () => void;
  speed?: string;
  onSpeedChange?: (value: string) => void;
  onIniciarSimulacion: () => void;
  isSimulating?: boolean;
}

const BottomLeftControls: React.FC<BottomLeftControlsProps> = ({
    variant = "full",
    date = "Día 1 | 02/04/2025 | 13:00",
    onStop,
    onSpeedChange,
    onIniciarSimulacion,
    isSimulating,
    }) => {
    const panelBg = useColorModeValue("white", "gray.800");

    const showSpeed = variant === "full";
    const showDate = variant === "full" || variant === "date-pause";
    const showPause = variant === "full" || variant === "date-pause";
    const boxShadow = useColorModeValue("md", "dark-lg");
    const [isOpen, setIsOpen] = useState(false);
    const [speed, setSpeed] = useState("Velocidad x1");
    const togglePanel = () => {
        setIsOpen(!isOpen);
    };

    const handleSpeedChange = (newSpeed: string) => {
        setSpeed(newSpeed);
        onSpeedChange?.(newSpeed); // ← llamada al padre
        setIsOpen(false);
    };

    return (
        <Flex
            position="absolute"
            bottom="20px"
            left="20px"
            align="center"
            justify="space-between"
            gap={4}
            zIndex={1000}
            >
            {showDate && (
                <Box
                    bg={panelBg}
                    borderRadius="md"
                    px={4}
                    py={2}
                    boxShadow={boxShadow}
                    minW="170px"
                    border="1px solid"
                    borderColor="blue.600"
                >
                <Text fontWeight="bold" color="purple.800">{date}</Text>
                </Box>
            )}

            {isSimulating && (
                // <Box
                //     bg={panelBg}
                //     borderRadius="md"
                //     px={4}
                //     py={2}
                //     boxShadow={boxShadow}
                //     display="flex"
                //     alignItems="center"
                //     justifyContent="center"
                //     border="1px solid"
                //     borderColor="blue.600"
                //     _hover={{ bg: "gray.100" }} // Cambia el color al pasar el mouse
                // >
                //     <Button size="xs" colorScheme="red" onClick={onStop}/>
                
                // </Box>
                <Button
                    colorScheme="red"
                    size="s"
                    p={3}
                    onClick={onStop}
                >
                    Detener Simulación
                </Button>
            )}
            {!isSimulating &&
                (
                // <Box
                //     bg={panelBg}
                //     borderRadius="md"
                //     boxShadow={boxShadow}
                //     display="flex"
                //     alignItems="center"
                //     justifyContent="center"
                //     border="1px solid"
                //     borderColor="blue.600"
                //     _hover={{ bg: "gray.100" }} // Cambia el color al pasar el mouse
                // >
                    <Button
                        colorScheme="green"
                        size="s"
                        p={3}
                        onClick={onIniciarSimulacion}
                    >
                        Iniciar Simulación
                    </Button>
                // </Box>
                )
            }

            {showSpeed && (
                <Box
                    bg={panelBg}
                    borderRadius="md"
                    p={0} // Sin relleno para evitar espacio alrededor del botón
                    boxShadow="md"
                    position="relative"
                    border="1px solid"
                    borderColor="blue.600"
                >
                <Button 
                    onClick={togglePanel}
                    variant="outline"
                    fontWeight="bold"
                    color="purple.800"
                    w="100%"
                    border="none" // Eliminamos el borde blanco
                    _focus={{ outline: 'none' }} // Sin borde al hacer clic
                    display="flex" // Usar flex para alinear el ícono
                    justifyContent="space-between"
                    alignItems="center"
                    px={4} // Para añadir algún espacio lateral
                >
                    <Text>{speed}</Text>
                    {isOpen ? <ChevronUpIcon /> : <ChevronDownIcon />} {/* Ícono de flecha */}
                </Button>
                <Collapse in={isOpen}>
                    <VStack spacing={0} mt={2} w="100%" position="absolute" bottom="100%" left={0} 
                            borderTopWidth={1} borderRightWidth={1} borderLeftWidth={1} borderColor="blue.600">
                        <Button variant="solid" bg={panelBg} width="100%" borderTopRadius={0} fontWeight="bold" color="purple.800" onClick={() => handleSpeedChange("Velocidad x2")}>
                            Velocidad x2
                        </Button>
                        <Button variant="solid" bg={panelBg} width="100%"  borderBottomRadius={0} fontWeight="bold" color="purple.800" onClick={() => handleSpeedChange("Velocidad x1")}>
                            Velocidad x1
                        </Button>
                    </VStack>
                </Collapse>
                </Box>
            )}
    </Flex>
    );
};

export default BottomLeftControls;