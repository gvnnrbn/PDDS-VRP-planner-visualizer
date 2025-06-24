import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button, Modal, ModalOverlay, ModalContent, ModalBody } from '@chakra-ui/react'
import { IncidenciaForm } from '../../components/IncidenciaForm'
import { IncidenciaTable } from '../../components/IncidenciaTable'
import type { Incidencia } from '../../core/types/incidencia'
import { useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

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
    navigate('/pedidos')
  }

  const handleNextPhase = () => {
    navigate('/vehiculos')
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between" align="center">
          <Button 
            onClick={handlePreviousPhase}
            variant='primary'
            width="15rem"
          >
            Atrás: Pedidos
          </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Incidencias
          </Text>
          <Link to={'/vehiculos'}>
            <Button variant='primary'width="15rem">Siguiente: Vehículos</Button>
          </Link>
        </HStack>

        <IncidenciaTable 
          onIncidenciaSelect={handleIncidenciaSelect}
          onNuevaIncidencia={() => {
            setSelectedIncidencia(undefined)
            setShowForm(true)
          }}
        />

        <Modal isOpen={showForm} onClose={handleFormCancel} isCentered size="lg">
          <ModalOverlay />
          <ModalContent>
            <ModalBody>
              <IncidenciaForm
                incidencia={selectedIncidencia}
                onFinish={handleFormFinish}
                onCancel={handleFormCancel}
              />
            </ModalBody>
          </ModalContent>
        </Modal>
      </VStack>
    </Box>
  )
} 