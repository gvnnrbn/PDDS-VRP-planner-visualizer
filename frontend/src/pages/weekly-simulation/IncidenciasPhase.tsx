import { Box, VStack, Text, HStack, Button } from '@chakra-ui/react'
import { useNavigate } from 'react-router-dom'

export default function IncidenciasPhase() {
  const navigate = useNavigate()

  const handlePreviousPhase = () => {
    navigate('/weekly-simulation/pedidos')
  }

  const handleNextPhase = () => {
    navigate('/weekly-simulation/simulacion')
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between" align="center">
          <Button 
            colorScheme="gray" 
            onClick={handlePreviousPhase}
            width="15rem"
          >
            Atrás: Pedidos
          </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Incidencias
          </Text>
          <Button 
            colorScheme="blue" 
            onClick={handleNextPhase}
            width="15rem"
          >
            Siguiente: Simulación
          </Button>
        </HStack>

        <HStack justify="flex-end">
          <Button colorScheme="blue">
            Nueva Incidencia
          </Button>
        </HStack>

      </VStack>
    </Box>
  )
}
