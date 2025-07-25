import { Box, Button, Collapse, Flex, Text, useDisclosure } from "@chakra-ui/react";
import { faArrowsToDot, faChevronDown, faChevronUp } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

interface Punto {
  posX: number;
  posY: number;
}

interface Bloqueo {
  idBloqueo: number;
  fechaInicio: string;
  fechaFin: string;
  segmentos: Punto[];
}

interface BloqueoCardProps {
  bloqueo: Bloqueo;
  onClick: () => void;
}

export const BloqueoCard = ({ bloqueo, onClick }: BloqueoCardProps) => {
  const { isOpen, onToggle } = useDisclosure();

  // Convertir puntos a segmentos como pares consecutivos
  const segmentos = bloqueo.segmentos
    .slice(1)
    .map((p, i) => ({
      from: bloqueo.segmentos[i],
      to: p
    }));

  return (
    <Flex direction="column" bg="#FFE5E5" borderRadius="10px" py={3} px={4} mx={-1} gap={2}>
      <Flex justify="space-between" align="center">
        <Text fontWeight={600} fontSize={18} color="red.500">
          Bloqueo #{bloqueo.idBloqueo}
        </Text>
        <Button size="sm" variant="primary" onClick={onClick}>
          Enfocar <FontAwesomeIcon icon={faArrowsToDot} />
        </Button>
      </Flex>
      <Text fontSize={14}>Inicio: {bloqueo.fechaInicio}</Text>
      <Text fontSize={14}>Fin: {bloqueo.fechaFin}</Text>
      <Button
        onClick={onToggle}
        size="sm"
        variant="ghost"
        colorScheme="red"
        rightIcon={<FontAwesomeIcon icon={isOpen ? faChevronUp : faChevronDown} />}
      >
        {isOpen ? "Ocultar Segmentos" : `Segmentos (${segmentos.length})`}
      </Button>
      <Collapse in={isOpen} animateOpacity>
        <Box pl={4} mt={2}>
          {segmentos.map((seg, idx) => (
            <Text key={idx} fontSize={13}>
              • ({seg.from.posX},{seg.from.posY}) → ({seg.to.posX},{seg.to.posY})
            </Text>
          ))}
        </Box>
      </Collapse>
    </Flex>
  );
};