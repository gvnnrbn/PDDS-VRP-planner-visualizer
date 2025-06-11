import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button } from '@chakra-ui/react'
import { VehiculoForm } from '../../components/VehiculosForm'
import { VehiculoTable } from '../../components/VehiculosTable'
import type { Vehiculo } from '../../core/types/vehiculo'
import { Link } from 'react-router-dom'

export default function VehiculosPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedVehiculo, setSelectedVehiculo] = useState<Vehiculo | null>(null)
  const navigate = useNavigate()

  const handleFormFinish = () => {
    setShowForm(false)
    setSelectedVehiculo(null)
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setSelectedVehiculo(null)
  }

  const handleVehiculoSelect = (vehiculo: Vehiculo) => {
    setSelectedVehiculo(vehiculo)
    setShowForm(true)
  }

  const handlePreviousPhase = () => {
    navigate('/weekly-simulation/incidencias')
  }

  const handleNextPhase = () => {
    navigate('/weekly-simulation/almacen')
  }


  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between">
            <Button 
                onClick={handlePreviousPhase}
                colorScheme="gray" 
                width="15rem"
            >
                Atrás: Incidencias
            </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" flex={1}>
            Gestión de Vehículos
          </Text>
          <Link to={'/almacenes'}>
                      <Button variant='primary'width="15rem">Siguiente: Almacenes</Button>
                    </Link>
        </HStack>

        <HStack justify="flex-end">
          <Button
            colorScheme="blue"
            onClick={() => {
              setSelectedVehiculo(null)
              setShowForm(true)
            }}
          >
            Nuevo Vehículo
          </Button>
        </HStack>

        {showForm ? (
          <VehiculoForm
            vehiculo={selectedVehiculo ?? undefined}
            onFinish={handleFormFinish}
            onCancel={handleFormCancel}
          />
        ) : (
          <VehiculoTable onVehiculoSelect={handleVehiculoSelect} />
        )}
      </VStack>
    </Box>
  )
}