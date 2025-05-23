import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button } from '@chakra-ui/react'
import { IncidenciaForm } from '../../components/IncidenciaForm'
import { IncidenciaTable } from '../../components/IncidenciaTable'
import type { Incidencia } from '../../core/types/incidencia'
import { useQueryClient } from '@tanstack/react-query'

export default function IncidenciasPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedIncidencia, setSelectedIncidencia] = useState<Incidencia | undefined>(undefined)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const handleFormFinish = () => {
    setShowForm(false)
    setSelectedIncidencia(undefined)
    queryClient.invalidateQueries({ queryKey: ['incidencias'] })
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setSelectedIncidencia(undefined)
  }

  const handleIncidenciaSelect = (incidencia: Incidencia) => {
    setSelectedIncidencia(incidencia)
    setShowForm(true)
  }

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
            onClick={handlePreviousPhase}
            colorScheme="gray" 
            width="15rem"
          >
            Atrás: Pedidos
          </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Incidencias
          </Text>
          <Button 
            onClick={handleNextPhase}
            colorScheme="blue" 
            width="15rem"
          >
            Siguiente: Simulación
          </Button>
        </HStack>

        <HStack justify="flex-end">
          <Button 
            onClick={() => {
              setSelectedIncidencia(undefined)
              setShowForm(true)
            }}
            colorScheme="blue"
          >
            Nueva Incidencia
          </Button>
        </HStack>

        {showForm ? (
          <IncidenciaForm
            incidencia={selectedIncidencia}
            onFinish={handleFormFinish}
            onCancel={handleFormCancel}
          />
        ) : (
          <IncidenciaTable onIncidenciaSelect={handleIncidenciaSelect} />
        )}
      </VStack>
    </Box>
  )
}
