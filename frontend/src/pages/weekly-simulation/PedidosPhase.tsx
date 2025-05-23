import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button } from '@chakra-ui/react'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoTable } from '../../components/PedidoTable'

export default function PedidosPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedPedido, setSelectedPedido] = useState<any>(null)
  const navigate = useNavigate()

  const handleFormFinish = () => {
    setShowForm(false)
    setSelectedPedido(null)
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setSelectedPedido(null)
  }

  const handlePedidoSelect = (pedido: any) => {
    setSelectedPedido(pedido)
    setShowForm(true)
  }

  const handleNextPhase = () => {
    navigate('/weekly-simulation/incidencias')
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between" align="center">
          <Button 
            colorScheme="blue" 
            width="15rem"
            opacity={0}
          />
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Pedidos
          </Text>
          <Button 
            onClick={handleNextPhase}
            colorScheme="blue" 
            width="15rem"
          >
            Siguiente: Incidencias
          </Button>
        </HStack>

        <HStack justify="flex-end">
          <Button 
            colorScheme="blue" 
            onClick={() => {
              setSelectedPedido(null)
              setShowForm(true)
            }}
          >
            Nuevo Pedido
          </Button>
        </HStack>

        {showForm ? (
          <PedidoForm
            pedido={selectedPedido}
            onFinish={handleFormFinish}
            onCancel={handleFormCancel}
          />
        ) : (
          <PedidoTable
            onPedidoSelect={handlePedidoSelect}
          />
        )}
      </VStack>
    </Box>
  )
}
