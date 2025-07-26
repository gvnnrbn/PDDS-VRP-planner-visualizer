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
        // Convert the date to the format expected by the backend: 'YYYY-MM-DD HH:mm:ss'
        let dateStr = formattedData.fechaRegistro;
        
        // If it's an ISO string with timezone (e.g., "2025-07-26T00:33:57.323Z")
        if (dateStr.includes('T') && dateStr.includes('Z')) {
          // Remove timezone and milliseconds
          dateStr = dateStr.replace('Z', '');
          if (dateStr.includes('.')) {
            dateStr = dateStr.substring(0, dateStr.indexOf('.'));
          }
          // Replace 'T' with space
          dateStr = dateStr.replace('T', ' ');
        }
        // If it's an ISO string without timezone (e.g., "2025-07-26T00:33:57")
        else if (dateStr.includes('T')) {
          // Ensure seconds are present
          if (dateStr.length === 16) dateStr += ':00';
          dateStr = dateStr.replace('T', ' ');
        }
        // If it's already in the correct format, leave it as is
        
        formattedData.fechaRegistro = dateStr;
      }

      console.log("Date format being sent:", formattedData.fechaRegistro)

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

        {/* <FormControl>
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
        </FormControl> */}

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
