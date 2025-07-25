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
      // Create a copy of formData and format the date
      const formattedData = { ...formData }
      console.log("Formatted data", formattedData)
      if (formattedData.fechaRegistro) {
        // Si viene en formato yyyy-MM-ddTHH:mm, convertir a 'YYYY-MM-DD HH:mm:ss'
        let dateStr = formattedData.fechaRegistro;
        if (dateStr.includes('T')) {
          // Asegura segundos
          if (dateStr.length === 16) dateStr += ':00';
          formattedData.fechaRegistro = dateStr.replace('T', ' ');
        }
      }

      console.log("Inserting pedido", formattedData)

      if (pedido?.id) {
        await pedidoService.updatePedido(formData.id, formattedData)
      } else {
        await pedidoService.createPedido(formattedData)
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

        <FormControl>
          <FormLabel>Fecha de Ingreso</FormLabel>
          <Input
            type="datetime-local"
            value={(() => {
              if (!formData.fechaRegistro) return '';
              const d = new Date(formData.fechaRegistro);
              if (isNaN(d.getTime())) return ''; // <-- Evita RangeError
              const tzOffset = d.getTimezoneOffset() * 60000;
              const local = new Date(d.getTime() - tzOffset);
              return local.toISOString().slice(0, 16);
            })()}
            onChange={e => {
              setFormData({ ...formData, fechaRegistro: e.target.value });
            }}
          />
        </FormControl>

        <HStack>
          <FormControl>
            <FormLabel>Posición X</FormLabel>
            <Input 
              type="number"
              value={formData.posicionX}
              onChange={(e) => setFormData({ ...formData, posicionX: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
            />
          </FormControl>
          <FormControl>
            <FormLabel>Posición Y</FormLabel>
            <Input 
              type="number"
              value={formData.posicionY}
              onChange={(e) => setFormData({ ...formData, posicionY: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
            />
          </FormControl>
        </HStack>

        <HStack>
          <FormControl>
            <FormLabel>Cantidad GLP</FormLabel>
            <Input 
              type="number"
              value={formData.cantidadGLP}
              onChange={(e) => setFormData({ ...formData, cantidadGLP: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
            />
          </FormControl>
          <FormControl>
            <FormLabel>Tiempo Tolerancia (horas)</FormLabel>
            <Input 
              type="number"
              value={formData.tiempoTolerancia}
              onChange={(e) => setFormData({ ...formData, tiempoTolerancia: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 })}
            />
          </FormControl>
        </HStack>

        <HStack spacing={4}>
          <Button 
            variant={'primary'}
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
