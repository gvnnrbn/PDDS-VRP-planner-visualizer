import { Box, VStack, Text, HStack, Button } from '@chakra-ui/react'
import { useNavigate } from 'react-router-dom'

export default function SimulationPhase() {
  const navigate = useNavigate()

  const handlePreviousPhase = () => {
    navigate('/weekly-simulation/incidencias')
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
            Atrás: Incidencias
          </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Simulación de Rutas
          </Text>
          <Button 
            colorScheme="blue" 
            width="15rem"
            opacity={0}
          />
        </HStack>
      </VStack>
    </Box>
  )
}
