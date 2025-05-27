import { useState } from 'react'
import {
  Box, Button, Text, VStack, HStack, Input, FormControl, FormLabel, Select, Checkbox, useToast
} from '@chakra-ui/react'
import type { Vehiculo } from '../core/types/vehiculo'
import { VehiculoService } from '../core/services/VehiculoService'
import { useQueryClient } from '@tanstack/react-query'

const vehiculoService = new VehiculoService()

interface VehiculoFormProps {
  vehiculo?: Vehiculo
  onFinish: () => void
  onCancel: () => void
}

export const VehiculoForm = ({ vehiculo, onFinish, onCancel }: VehiculoFormProps) => {
  const queryClient = useQueryClient()
  const toast = useToast()

  const [formData, setFormData] = useState<Partial<Vehiculo>>({
    id: vehiculo?.id,
    tipo: vehiculo?.tipo || 'TA',
    peso: vehiculo?.peso || 0,
    maxCombustible: vehiculo?.maxCombustible || 0,
    maxGlp: vehiculo?.maxGlp || 0,
    currCombustible: vehiculo?.currCombustible || 0,
    currGlp: vehiculo?.currGlp || 0,
    posicionX: vehiculo?.posicionX || 0,
    posicionY: vehiculo?.posicionY || 0,
    disponible: vehiculo?.disponible ?? true
  })

  const handleSubmit = async () => {
    try {
      if (formData.id) {
        await vehiculoService.updateVehiculo(formData.id, formData)
      } else {
        await vehiculoService.createVehiculo(formData)
      }
      queryClient.invalidateQueries({ queryKey: ['vehiculos'] })
      toast({ title: 'Guardado', status: 'success', duration: 2000, isClosable: true })
      onFinish()
    } catch (error) {
      toast({ title: 'Error', description: 'No se pudo guardar', status: 'error', duration: 3000, isClosable: true })
    }
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <Text fontSize="xl" fontWeight="bold">
          {vehiculo ? 'Editar Vehículo' : 'Nuevo Vehículo'}
        </Text>

        <FormControl>
          <FormLabel>Tipo</FormLabel>
          <Select value={formData.tipo} onChange={(e) => setFormData({ ...formData, tipo: e.target.value as any })}>
            <option value="TA">TA</option>
            <option value="TB">TB</option>
            <option value="TC">TC</option>
            <option value="TD">TD</option>
          </Select>
        </FormControl>

        <FormControl>
          <FormLabel>Peso</FormLabel>
          <Input type="number" value={formData.peso} onChange={(e) => setFormData({ ...formData, peso: parseInt(e.target.value) })} />
        </FormControl>

        <HStack>
          <FormControl>
            <FormLabel>Max. Combustible</FormLabel>
            <Input type="number" value={formData.maxCombustible} onChange={(e) => setFormData({ ...formData, maxCombustible: parseFloat(e.target.value) })} />
          </FormControl>
          <FormControl>
            <FormLabel>Max. GLP</FormLabel>
            <Input type="number" value={formData.maxGlp} onChange={(e) => setFormData({ ...formData, maxGlp: parseFloat(e.target.value) })} />
          </FormControl>
        </HStack>
        <HStack>
          <FormControl>
            <FormLabel>Combustible Actual</FormLabel>
            <Input
              type="number"
              value={formData.currCombustible}
              onChange={(e) =>
                setFormData({ ...formData, currCombustible: e.target.value === '' ? undefined : parseFloat(e.target.value), })
              }
            />
          </FormControl>

          <FormControl>
            <FormLabel>GLP Actual</FormLabel>
            <Input
              type="number"
              value={formData.currGlp}
              onChange={(e) =>
                setFormData({ ...formData, currGlp: parseFloat(e.target.value) })
              }
            />
          </FormControl>
        </HStack>


        <HStack>
          <FormControl>
            <FormLabel>Posición X</FormLabel>
            <Input type="number" value={formData.posicionX} onChange={(e) => setFormData({ ...formData, posicionX: parseFloat(e.target.value) })} />
          </FormControl>
          <FormControl>
            <FormLabel>Posición Y</FormLabel>
            <Input type="number" value={formData.posicionY} onChange={(e) => setFormData({ ...formData, posicionY: parseFloat(e.target.value) })} />
          </FormControl>
        </HStack>

        <Checkbox isChecked={formData.disponible} onChange={(e) => setFormData({ ...formData, disponible: e.target.checked })}>
          Disponible
        </Checkbox>

        <HStack spacing={4}>
          <Button colorScheme="blue" onClick={handleSubmit}>
            {vehiculo ? 'Guardar Cambios' : 'Crear Vehículo'}
          </Button>
          <Button colorScheme="gray" onClick={onCancel}>
            Cancelar
          </Button>
        </HStack>
      </VStack>
    </Box>
  )
}
