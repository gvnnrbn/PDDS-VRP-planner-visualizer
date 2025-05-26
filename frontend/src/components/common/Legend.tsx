import {
  Box,
  Text,
  SimpleGrid,
  HStack,
  Collapse,
  Icon,
  Flex,
  useDisclosure,
  useColorModeValue,
} from "@chakra-ui/react";
import { ChevronUpIcon, ChevronDownIcon } from "@chakra-ui/icons";
import React from "react";

interface LegendPanelProps {
  isSidebarCollapsed: boolean;
}

const LegendPanel: React.FC<LegendPanelProps> = ({ isSidebarCollapsed }) => {
  const { isOpen, onToggle } = useDisclosure({ defaultIsOpen: false });

  const bg = useColorModeValue("white", "gray.800");
  const boxShadow = useColorModeValue("md", "dark-lg");

  return (
    <Box
      position="absolute"
      bottom="20px"
      right={isSidebarCollapsed ? "80px" : "32%"}
      bg={bg}
      boxShadow={boxShadow}
      borderRadius="md"
      overflow="hidden"
      zIndex={1000}
      transition="right 0.3s ease"
      minW="200px"
    >
      {/* Header clickable */}
      <Flex
        px={4}
        py={2}
        align="center"
        justify="space-between"
        cursor="pointer"
        onClick={onToggle}
        borderBottom={isOpen ? "1px solid" : "none"}
        borderColor={useColorModeValue("gray.200", "gray.600")}
      >
        <Text fontWeight="bold" color="purple.800">
          LEYENDA
        </Text>
        <Icon
          as={isOpen ? ChevronUpIcon : ChevronDownIcon}
          boxSize={5}
          color="purple.800"
        />
      </Flex>

      {/* Collapsible Content */}
      <Collapse in={isOpen} animateOpacity>
        <Box px={16} py={2}>
            <SimpleGrid columns={2} spacingX={5} spacingY={3}>
                <HStack>
                    <Box bg="black" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Vehículos</Text>
                </HStack>
                <HStack>
                    <Box bg="black" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Almacén Principal</Text>
                </HStack>
                
                <HStack>
                    <Box bg="red.500" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Averiado</Text>
                </HStack>
                <HStack>
                    <Box bg="red.400" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Tanque Cerrado</Text>
                </HStack>
                <HStack>
                    <Box bg="yellow.400" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Mantenimiento</Text>
                </HStack>
                <HStack>
                    <Box bg="green.400" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Tanque disponible</Text>
                </HStack>
                <HStack>
                    <Box bg="black" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Bloqueo</Text>
                </HStack>
                <HStack>
                    <Box bg="red.400" w="12px" h="12px" borderRadius="2px" />
                    <Text fontSize="sm">Destino</Text>
                </HStack>
            </SimpleGrid>
        </Box>
      </Collapse>
    </Box>
  );
};

export default LegendPanel;