import { useState } from 'react'
import { Box, Button, Text, VStack, HStack, Input, FormControl, FormLabel } from '@chakra-ui/react'
import type { Pedido } from '../core/types/pedido'
import { PedidoService } from '../core/services/PedidoService'
import { useQueryClient } from '@tanstack/react-query'

const pedidoService = new PedidoService()

interface PedidoFormProps {
  pedido?: Pedido
  onFinish: () => void
  onCancel: () => void
}

export const PedidoForm = ({ pedido, onFinish, onCancel }: PedidoFormProps) => {
  const queryClient = useQueryClient()
  const [formData, setFormData] = useState<Pedido>({
    id: pedido?.id || 0,
    codigoCliente: pedido?.codigoCliente || '',
    fechaRegistro: pedido?.fechaRegistro || new Date().toISOString(),
    posicionX: pedido?.posicionX || 0,
    posicionY: pedido?.posicionY || 0,
    cantidadGLP: pedido?.cantidadGLP || 0,
    tiempoTolerancia: pedido?.tiempoTolerancia || 0
  })

  const handleSubmit = async () => {
    try {
      if (pedido?.id) {
        await pedidoService.updatePedido(formData.id, formData)
      } else {
        await pedidoService.createPedido(formData)
      }
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      onFinish()
    } catch (error) {
      console.error('Error saving pedido:', error)
    }
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <Text fontSize="xl" fontWeight="bold">
          {pedido ? 'Editar Pedido' : 'Nuevo Pedido'}
        </Text>

        <FormControl>
          <FormLabel>Código Cliente</FormLabel>
          <Input 
            value={formData.codigoCliente}
            onChange={(e) => setFormData({ ...formData, codigoCliente: e.target.value })}
          />
        </FormControl>

        <HStack>
          <FormControl>
            <FormLabel>Posición X</FormLabel>
            <Input 
              type="number"
              value={formData.posicionX}
              onChange={(e) => setFormData({ ...formData, posicionX: parseInt(e.target.value) })}
            />
          </FormControl>
          <FormControl>
            <FormLabel>Posición Y</FormLabel>
            <Input 
              type="number"
              value={formData.posicionY}
              onChange={(e) => setFormData({ ...formData, posicionY: parseInt(e.target.value) })}
            />
          </FormControl>
        </HStack>

        <HStack>
          <FormControl>
            <FormLabel>Cantidad GLP</FormLabel>
            <Input 
              type="number"
              value={formData.cantidadGLP}
              onChange={(e) => setFormData({ ...formData, cantidadGLP: parseInt(e.target.value) })}
            />
          </FormControl>
          <FormControl>
            <FormLabel>Tiempo Tolerancia (min)</FormLabel>
            <Input 
              type="number"
              value={formData.tiempoTolerancia}
              onChange={(e) => setFormData({ ...formData, tiempoTolerancia: parseInt(e.target.value) })}
            />
          </FormControl>
        </HStack>

        <HStack spacing={4}>
          <Button 
            colorScheme="blue" 
            onClick={handleSubmit}
          >
            {pedido ? 'Guardar Cambios' : 'Crear Pedido'}
          </Button>
          <Button 
            colorScheme="gray" 
            onClick={onCancel}
          >
            Cancelar
          </Button>
        </HStack>
      </VStack>
    </Box>
  )
}
