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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTruck, faWarehouse, faIndustry, faSquare, faLocationDot } from '@fortawesome/free-solid-svg-icons';
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
      border="1px solid"
      borderColor="blue.600"
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
        <Box px={10} py={2}>
            <SimpleGrid columns={2} spacingX={5} spacingY={3}>
              <HStack>
                <FontAwesomeIcon icon={faTruck} size="lg" />
                <Text fontSize="sm">Vehículos</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faWarehouse} size="lg" />
                <Text fontSize="sm">Almacén Principal</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faTruck} size="lg" color="red" />
                <Text fontSize="sm">Averiado</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faIndustry} size="lg" color="red" />
                <Text fontSize="sm">Tanque Cerrado</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faTruck} size="lg" color="yellow" />
                <Text fontSize="sm">Mantenimiento</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faIndustry} size="lg" color="green" />
                <Text fontSize="sm">Tanque disponible</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faSquare} size="lg" />
                <Text fontSize="sm">Bloqueo</Text>
              </HStack>
              <HStack>
                <FontAwesomeIcon icon={faLocationDot} size="lg" color="red" />
                <Text fontSize="sm">Destino</Text>
              </HStack>
            </SimpleGrid>
        </Box>
      </Collapse>
    </Box>
  );
};

export default LegendPanel;