import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Input,
  NumberInput,
  NumberInputField,
  Switch,
  VStack,
  HStack
} from '@chakra-ui/react'
import { useState, useEffect } from 'react'
import type { Almacen } from '../core/types/almacen'

interface AlmacenFormProps {
  almacen?: Almacen
  onFinish: () => void
  onCancel: () => void
  onSubmitAlmacen: (data: Partial<Almacen>) => Promise<void>
}

export const AlmacenForm = ({ almacen, onFinish, onCancel, onSubmitAlmacen }: AlmacenFormProps) => {
  const [formData, setFormData] = useState<Partial<Almacen>>({
    capacidadEfectivam3: almacen?.capacidadEfectivam3 ?? 0,
    esPrincipal: almacen?.esPrincipal ?? false,
    horarioAbastecimiento: almacen?.horarioAbastecimiento ?? '',
    posicionX: almacen?.posicionX ?? 0,
    posicionY: almacen?.posicionY ?? 0
  })

  const handleChange = (field: keyof Almacen, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await onSubmitAlmacen(formData)
    onFinish()
  }

  useEffect(() => {
    if (formData.esPrincipal) {
      handleChange('horarioAbastecimiento', '') // reset
    }
  }, [formData.esPrincipal])

  return (
    <Box as="form" onSubmit={handleSubmit}>
      <VStack spacing={4} align="stretch">

        <FormControl>
          <FormLabel>Capacidad Efectiva (m³)</FormLabel>
          <NumberInput value={formData.capacidadEfectivam3} min={0} onChange={(val) => handleChange('capacidadEfectivam3', val === '' ? 0 : parseFloat(val) || 0)}>
            <NumberInputField />
          </NumberInput>
        </FormControl>

        <FormControl display="flex" alignItems="center">
          <FormLabel htmlFor="esPrincipal" mb="0">¿Es principal?</FormLabel>
          <Switch id="esPrincipal" isChecked={formData.esPrincipal} onChange={(e) => handleChange('esPrincipal', e.target.checked)} />
        </FormControl>

        <FormControl isDisabled={formData.esPrincipal}>
          <FormLabel>Horario de Abastecimiento (HH:mm)</FormLabel>
          <Input
            type="time"
            value={formData.horarioAbastecimiento}
            onChange={(e) => handleChange('horarioAbastecimiento', e.target.value)}
          />
        </FormControl>

        <FormControl>
          <FormLabel>Posición X</FormLabel>
          <NumberInput value={formData.posicionX} onChange={(val) => handleChange('posicionX', val === '' ? 0 : parseFloat(val) || 0)}>
            <NumberInputField />
          </NumberInput>
        </FormControl>

        <FormControl>
          <FormLabel>Posición Y</FormLabel>
          <NumberInput value={formData.posicionY} onChange={(val) => handleChange('posicionY', val === '' ? 0 : parseFloat(val) || 0)}>
            <NumberInputField />
          </NumberInput>
        </FormControl>

        <HStack justify="flex-end" spacing={4}>
          <Button onClick={onCancel}>Cancelar</Button>
          <Button colorScheme="teal" type="submit">Guardar</Button>
        </HStack>
      </VStack>
    </Box>
  )
}
