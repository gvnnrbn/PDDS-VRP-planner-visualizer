import { useState } from 'react'
import { Box, VStack, HStack, Text, Button, useToast } from '@chakra-ui/react'
import { AlmacenForm } from '../../components/AlmacenForm'
import { useNavigate } from 'react-router-dom'
import { AlmacenTable } from '../../components/AlmacenTable'
import { AlmacenService } from '../../core/services/AlmacenService'
import { useQueryClient } from '@tanstack/react-query'

export default function AlmacenPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedAlmacen, setSelectedAlmacen] = useState<any>(null)
  const queryClient = useQueryClient()
  const toast = useToast()
  const navigate = useNavigate()

  const almacenService = new AlmacenService()

  const handleFormFinish = () => {
    setShowForm(false)
    setSelectedAlmacen(null)
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setSelectedAlmacen(null)
  }

  const handleAlmacenSelect = (almacen: any) => {
    setSelectedAlmacen(almacen)
    setShowForm(true)
  }

  const handleSubmitAlmacen = async (data: any) => {
    try {
      if (selectedAlmacen) {
        await almacenService.updateAlmacen(selectedAlmacen.id, data)
        toast({ title: 'Almacén actualizado', status: 'success', duration: 3000, isClosable: true })
      } else {
        await almacenService.createAlmacen(data)
        toast({ title: 'Almacén creado', status: 'success', duration: 3000, isClosable: true })
      }
      queryClient.invalidateQueries({ queryKey: ['almacenes'] })
    } catch (error: any) {
      toast({ title: 'Error', description: error.message, status: 'error', duration: 3000, isClosable: true })
    }
  }
  const handlePreviousPhase = () => {
    navigate('/vehiculos')
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
                Atrás: Vehiculos
            </Button>
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" flex={1}>
            Gestión de Almacenes
          </Text>
        </HStack>

        <HStack justify="flex-end" spacing={4}>
          <Button 
            variant="primary"
            onClick={() => {
              setSelectedAlmacen(null)
              setShowForm(true)
            }}
          >
            Nuevo Almacén
          </Button>
        </HStack>

        {showForm ? (
          <AlmacenForm
            almacen={selectedAlmacen}
            onFinish={handleFormFinish}
            onCancel={handleFormCancel}
            onSubmitAlmacen={handleSubmitAlmacen}
          />
        ) : (
          <AlmacenTable
            onAlmacenSelect={handleAlmacenSelect}
          />
        )}
      </VStack>
    </Box>
  )
} 