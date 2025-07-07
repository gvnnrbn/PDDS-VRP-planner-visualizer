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
  extraBoxStyle?: React.CSSProperties;
}

const BottomLeftControls: React.FC<BottomLeftControlsProps> = ({
    variant = "full",
    date = "Día 1 | 02/04/2025 | 13:00",
    onStop,
    onSpeedChange,
    onIniciarSimulacion,
    isSimulating,
    extraBoxStyle = {},
    }) => {
    const panelBg = useColorModeValue("white", "gray.800");

    const showSpeed = variant === "full";
    const showDate = variant === "full" || variant === "date-pause";
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
                    style={extraBoxStyle}
                >
                {date.split('\n').map((line, idx) => (
                  <Text key={idx} fontWeight="bold" color="purple.800">{line}</Text>
                ))}
                </Box>
            )}
            <Box mt={20}>

                {isSimulating && (
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
                    <Button
                        colorScheme="green"
                        size="s"
                        p={3}
                        onClick={onIniciarSimulacion}
                    >
                        Iniciar Simulación
                    </Button>
                    )
                }
            </Box>

            {showSpeed && (
                <Box
                    bg={panelBg}
                    borderRadius="md"
                    p={0}
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
                    border="none"
                    _focus={{ outline: 'none' }}
                    display="flex"
                    justifyContent="space-between"
                    alignItems="center"
                    px={4}
                >
                    <Text>{speed}</Text>
                    {isOpen ? <ChevronUpIcon /> : <ChevronDownIcon />}
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