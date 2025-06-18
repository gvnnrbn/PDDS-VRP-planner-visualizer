import { Box } from '@chakra-ui/react'

interface MapGridProps {
  data?: {
    simulacion: Array<{
      minuto: string;
      [key: string]: unknown;
    }>;
  }
}

export const MapGridV2 = ({ data }: MapGridProps) => {
  return (
    <Box
      w="full"
      h="600px"
      bg="gray.100"
      borderRadius="md"
      display="flex"
      alignItems="center"
      justifyContent="center"
    >
      {/* TODO: Implement map visualization */}
      <Box>Mapa de simulación</Box>
    </Box>
  )
} 