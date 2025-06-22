import { useState, useEffect } from 'react'
import { Box, Button, Text, VStack, HStack, Input, FormControl, FormLabel, Select } from '@chakra-ui/react'
import type { Incidencia } from '../core/types/incidencia'
import { IncidenciaService } from '../core/services/IncidenciaService'
import { VehiculoService } from '../core/services/VehiculoService'
import { useQueryClient } from '@tanstack/react-query'
import { format, parse } from 'date-fns'
import type { Vehiculo } from '../core/types/vehiculo'

const incidenciaService = new IncidenciaService()
const vehiculoService = new VehiculoService()

interface IncidenciaFormProps {
  incidencia?: Incidencia
  onFinish: () => void
  onCancel: () => void
}

export const IncidenciaForm = ({ incidencia, onFinish, onCancel }: IncidenciaFormProps) => {
  const queryClient = useQueryClient()
  const [formData, setFormData] = useState<Incidencia>({
    id: incidencia?.id || 0,
    fecha: incidencia?.fecha || format(new Date(), 'yyyy-MM-dd'),
    turno: incidencia?.turno || 'T1',
    vehiculo: { id: incidencia?.vehiculo?.id || 0 },
    ocurrido: incidencia?.ocurrido || false,
  })
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])

  useEffect(() => {
    fetchVehiculos()
  }, [])

  const fetchVehiculos = async () => {
    try {
      const data = await vehiculoService.getAllVehiculos()
      setVehiculos(data)
    } catch (error) {
      console.error('Error fetching vehiculos:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      // Create a copy of formData and format the date
      const formattedData = { ...formData }
      
      if (formattedData.fecha) {
        const date = parse(formattedData.fecha, 'yyyy-MM-dd', new Date())
        formattedData.fecha = date.toISOString().split('T')[0]
      }

      if (incidencia?.id) {
        await incidenciaService.updateIncidencia(incidencia.id, formattedData)
      } else {
        await incidenciaService.createIncidencia(formattedData)
      }
      
      queryClient.invalidateQueries({ queryKey: ['incidencias'] })
      onFinish()
    } catch (error) {
      console.error('Error saving incidencia:', error)
    }
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <Text fontSize="xl" fontWeight="bold">
          {incidencia ? 'Editar Incidencia' : 'Nueva Avería'}
        </Text>

        <FormControl>
          <FormLabel>Fecha</FormLabel>
          <Input 
            type="date"
            value={formData.fecha}
            onChange={(e) => setFormData({ ...formData, fecha: e.target.value })}
          />
        </FormControl>

        <FormControl>
          <FormLabel>Turno</FormLabel>
          <Select
            value={formData.turno}
            onChange={(e) => setFormData({ ...formData, turno: e.target.value as 'T1' | 'T2' | 'T3' })}
          >
            <option value="T1">T1</option>
            <option value="T2">T2</option>
            <option value="T3">T3</option>
          </Select>
        </FormControl>

        <FormControl>
          <FormLabel>Vehiculo</FormLabel>
          <Select
            value={formData.vehiculo.id}
            onChange={(e) => setFormData({ ...formData, vehiculo: { id: e.target.value === '' ? 0 : parseInt(e.target.value) || 0 } })}
          >
            <option value="">Seleccionar vehiculo</option>
            {vehiculos.map((vehiculo) => (
              <option key={vehiculo.id} value={vehiculo.id}>
                {vehiculo.id}
              </option>
            ))}
          </Select>
        </FormControl>

        <FormControl>
          <FormLabel>Ocurrido</FormLabel>
          <Select
            value={formData.ocurrido ? 'true' : 'false'}
            onChange={(e) => setFormData({ ...formData, ocurrido: e.target.value === 'true' })}
          >
            <option value="true">Sí</option>
            <option value="false">No</option>
          </Select>
        </FormControl>

        <HStack spacing={4}>
          <Button variant={'primary'} onClick={handleSubmit}>
            {incidencia ? 'Actualizar' : 'Crear'}
          </Button>
          <Button onClick={onCancel}>Cancelar</Button>
        </HStack>
      </VStack>
    </Box>
  )
}

export default IncidenciaForm