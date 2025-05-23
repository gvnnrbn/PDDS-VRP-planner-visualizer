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
  useToast
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import type { Incidencia } from '../core/types/incidencia'
import { IncidenciaService } from '../core/services/IncidenciaService'
import { format } from 'date-fns'
import { useQuery, useQueryClient } from '@tanstack/react-query'

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

  if (isLoading) return <Text>Loading...</Text>
  if (error) return <Text>Error: {error.message}</Text>

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
            {incidencias?.map((incidencia) => (
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
    </Box>
  )
}
