import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableContainer,
  IconButton,
  HStack,
  Text,
  Box,
  useToast,
  Button,
  Select
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import type { Incidencia } from '../core/types/incidencia'
import { IncidenciaService } from '../core/services/IncidenciaService'
import { format } from 'date-fns'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

const incidenciaService = new IncidenciaService()

interface IncidenciaTableProps {
  onIncidenciaSelect?: (incidencia: Incidencia) => void
}

export const IncidenciaTable = ({ onIncidenciaSelect }: IncidenciaTableProps) => {
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: incidencias, isLoading, error } = useQuery<Incidencia[]>({
    queryKey: ['incidencias'],
    queryFn: () => incidenciaService.getAllIncidencias(),
    retry: 1
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentIncidencias = incidencias?.slice(indexOfFirstRow, indexOfLastRow) || []
  const totalPages = Math.ceil((incidencias?.length || 0) / rowsPerPage)

  const handleDelete = async (id: number) => {
    try {
      await incidenciaService.deleteIncidencia(id)
      queryClient.invalidateQueries({ queryKey: ['incidencias'] })
      toast({
        title: 'Success',
        description: 'Incidencia eliminada exitosamente',
        status: 'success',
        duration: 3000,
        isClosable: true,
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Error al eliminar la incidencia',
        status: 'error',
        duration: 3000,
        isClosable: true,
      })
    }
  }

  const handleRowsChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setRowsPerPage(parseInt(e.target.value))
    setCurrentPage(1)
  }

  if (isLoading) return <Text>Loading...</Text>
  if (error) return <Text>Error: {(error as Error).message}</Text>

  return (
    <Box>
      <TableContainer overflowY="auto">
        <Table variant="simple">
          <Thead>
            <Tr>
              <Th>Fecha</Th>
              <Th>Turno</Th>
              <Th>Vehículo</Th>
              <Th>Ocurrido</Th>
              <Th>Acciones</Th>
            </Tr>
          </Thead>
          <Tbody>
            {currentIncidencias.map((incidencia) => (
              <Tr key={incidencia.id}>
                <Td>{format(new Date(incidencia.fecha), 'yyyy-MM-dd')}</Td>
                <Td>{incidencia.turno}</Td>
                <Td>{incidencia.vehiculo?.id}</Td>
                <Td>{incidencia.ocurrido ? 'Sí' : 'No'}</Td>
                <Td>
                  <HStack spacing={2}>
                    <IconButton
                      aria-label="Editar"
                      icon={<EditIcon />}
                      size="sm"
                      onClick={() => onIncidenciaSelect?.(incidencia)}
                    />
                    <IconButton
                      aria-label="Eliminar"
                      icon={<DeleteIcon />}
                      size="sm"
                      colorScheme="red"
                      onClick={() => handleDelete(incidencia.id)}
                    />
                  </HStack>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </TableContainer>

      {/* Paginación alineada a la derecha */}
      <HStack justify="flex-end" mt={4} spacing={6}>
        <HStack>
          <Text>Filas por página:</Text>
          <Select size="sm" value={rowsPerPage} onChange={handleRowsChange} w="75px">
            <option value="5">5</option>
            <option value="10">10</option>
            <option value="15">15</option>
          </Select>
        </HStack>

        <Button
          size="sm"
          onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
          isDisabled={currentPage === 1}
        >
          Anterior
        </Button>
        <Text fontSize="sm">Página {currentPage} de {totalPages}</Text>
        <Button
          size="sm"
          onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
          isDisabled={currentPage === totalPages}
        >
          Siguiente
        </Button>
      </HStack>
    </Box>
  )
}
