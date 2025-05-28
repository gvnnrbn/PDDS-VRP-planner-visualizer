import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button,useToast } from '@chakra-ui/react'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoTable } from '../../components/PedidoTable'
import { PedidoService } from '../../core/services/PedidoService'
import { useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
export default function PedidosPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedPedido, setSelectedPedido] = useState<any>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const toast = useToast()
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
  const pedidoService = new PedidoService()

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    try {
      await pedidoService.importarPedidos(file)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      toast({ title: 'Importación exitosa', status: 'success', duration: 3000, isClosable: true })
    } catch (error: any) {
      toast({ title: 'Error', description: error.message, status: 'error', duration: 3000, isClosable: true })
    }
  }
  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between" align="center">
          <Button 
            variant='primary'
            width="15rem"
            opacity={0}
          />
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Pedidos
          </Text>
          <Button 
            variant='primary'
            onClick={handleNextPhase}
            width="15rem"
          >
            Siguiente: Incidencias
          </Button>
        </HStack>

        <HStack justify="space-between">
          <Button 
            variant='primary'
            onClick={() => {
              setSelectedPedido(null)
              setShowForm(true)
            }}
          >
            Nuevo Pedido
          </Button>
          <Button colorScheme="teal" onClick={() => fileInputRef.current?.click()}>
            Importar archivo
          </Button>
          <input
            type="file"
            accept=".txt"
            ref={fileInputRef}
            onChange={handleFileUpload}
            hidden
          />
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
