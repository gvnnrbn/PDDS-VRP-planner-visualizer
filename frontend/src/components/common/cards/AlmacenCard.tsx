import { Box, Text, Badge, Button, Flex } from "@chakra-ui/react";
import type { AlmacenSimulado } from "../../../core/types/almacen";

interface AlmacenCardProps {
  almacen: AlmacenSimulado;
  onFocus?: () => void;
  vehiculos?: Record<string, number>;
  highlighted?: boolean;
  id?: string;
}

export const AlmacenCard = ({ almacen, onFocus, vehiculos = {}, highlighted = false, id }: AlmacenCardProps) => {
  const tieneVehiculos = Object.keys(vehiculos).length > 0;
  return (
    <Box
      id={id}
      bg="white"
      borderRadius="10px"
      py={3}
      px={4}
      mb={2}
      border={highlighted ? '3px solid #805ad5' : '1px solid #e2e8f0'}
      transition="border 0.3s, box-shadow 0.3s"
      boxShadow={highlighted ? '0 0 0 4px #e9d8fd' : 'md'}
    >
      <Flex align="center" justify="space-between" mb={1}>
        <Text fontWeight={600} fontSize={18} color="purple.700">
          Almacén {almacen.idAlmacen} {almacen.isMain && <Badge colorScheme="purple">Principal</Badge>}
        </Text>
        <Button size="sm" colorScheme="purple" variant="solid" onClick={onFocus}>
          Enfocar
        </Button>
      </Flex>
      <Text>GLP Actual: {almacen.isMain ? "Ilimitada" : (almacen.currentGLP ?? "—")}</Text>
      <Text>Capacidad Máx: {almacen.isMain ? "Ilimitada" : (almacen.maxGLP ?? "—")}</Text>
      <Text>Posición: {almacen.posicion ? `${almacen.posicion.posX}, ${almacen.posicion.posY}` : "—"}</Text>
      {tieneVehiculos && (
        <Box mt={3} maxHeight="120px" overflowY="auto" bg="gray.50" borderRadius="md" p={2}>
          {Object.entries(vehiculos).map(([placa, count]) => (
            <Box key={placa} fontSize="sm" py={1}>
              <b>{placa}</b> — {count} {count === 1 ? 'vez' : 'veces'}
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
};

export default AlmacenCard; 