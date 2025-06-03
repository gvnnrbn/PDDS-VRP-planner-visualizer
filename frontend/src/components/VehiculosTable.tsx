import {
  Table, Thead, Tbody, Tr, Th, Td, TableContainer,
  IconButton, HStack, Text, Box, VStack, useToast, Button, Select, Flex
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { Vehiculo } from '../core/types/vehiculo'
import { VehiculoService } from '../core/services/VehiculoService'

const vehiculoService = new VehiculoService()

interface VehiculoTableProps {
  onVehiculoSelect?: (vehiculo: Vehiculo) => void
}

export const VehiculoTable = ({ onVehiculoSelect }: VehiculoTableProps) => {
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: vehiculos, isLoading, error } = useQuery({
    queryKey: ['vehiculos'],
    queryFn: () => vehiculoService.getAllVehiculos()
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentVehiculos = vehiculos?.slice(indexOfFirstRow, indexOfLastRow) || []
  const totalPages = Math.ceil((vehiculos?.length || 0) / rowsPerPage)

  const handleDelete = async (id: number) => {
    try {
      await vehiculoService.deleteVehiculo(id)
      queryClient.invalidateQueries({ queryKey: ['vehiculos'] })
      toast({ title: 'Vehículo eliminado', status: 'success', duration: 2000, isClosable: true })
    } catch (error) {
      toast({ title: 'Error', description: 'No se pudo eliminar', status: 'error', duration: 3000, isClosable: true })
    }
  }

  if (isLoading) return <Text>Cargando vehículos...</Text>
  if (error) return <Text>Error al cargar vehículos</Text>

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <TableContainer>
          <Table variant="simple">
            <Thead>
              <Tr>
                <Th>Tipo</Th>
                <Th>Placa</Th>
                <Th>Peso</Th>
                <Th>Combustible</Th>
                <Th>GLP</Th>
                <Th>Posición</Th>
                <Th>Disponible</Th>
                <Th>Acciones</Th>
              </Tr>
            </Thead>
            <Tbody>
              {currentVehiculos.map((v) => (
                <Tr key={v.id}>
                  <Td>{v.tipo}</Td>
                  <Td>{v.placa}</Td>
                  <Td>{v.peso}</Td>
                  <Td>{`${v.currCombustible}/${v.maxCombustible}`}</Td>
                  <Td>{`${v.currGlp}/${v.maxGlp}`}</Td>
                  <Td>{`${v.posicionX}, ${v.posicionY}`}</Td>
                  <Td>{v.disponible ? 'Sí' : 'No'}</Td>
                  <Td>
                    <HStack>
                      <IconButton aria-label="Editar" icon={<EditIcon />} onClick={() => onVehiculoSelect?.(v)} />
                      <IconButton aria-label="Eliminar" icon={<DeleteIcon />} colorScheme="red" onClick={() => handleDelete(v.id)} />
                    </HStack>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </TableContainer>

        {/* Pagination Controls */}
        <Flex justify="flex-end" align="center" gap={4} mt={4}>
          <Text>Filas por página:</Text>
          <Select
            width="75px"
            value={rowsPerPage}
            onChange={(e) => {
              setRowsPerPage(parseInt(e.target.value))
              setCurrentPage(1) // Reset to page 1 when rows per page changes
            }}
          >
            {[5, 10, 15, 20].map(n => (
              <option key={n} value={n}>{n}</option>
            ))}
          </Select>
          <Button
            onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
            isDisabled={currentPage === 1}
          >
            Anterior
          </Button>
          <Text>Página {currentPage} de {totalPages}</Text>
          <Button
            onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
            isDisabled={currentPage === totalPages}
          >
            Siguiente
          </Button>
        </Flex>
      </VStack>
    </Box>
  )
}
