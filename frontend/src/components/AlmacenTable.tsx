import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableContainer,
  IconButton,
  Button,
  HStack,
  VStack,
  Text,
  Select,
  useToast
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { Almacen } from '../core/types/almacen'
import { AlmacenService } from '../core/services/AlmacenService'

const almacenService = new AlmacenService()

export const AlmacenTable = ({ onAlmacenSelect }: { onAlmacenSelect: (a: Almacen) => void }) => {
  const queryClient = useQueryClient()
  const toast = useToast()
  const { data: almacenes, isLoading, error } = useQuery<Almacen[]>({
    queryKey: ['almacenes'],
    queryFn: () => almacenService.getAllAlmacenes()
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(5)

  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentAlmacenes = almacenes?.slice(indexOfFirstRow, indexOfLastRow) || []
  const totalPages = Math.ceil((almacenes?.length || 0) / rowsPerPage)

  const handleDelete = async (id: number) => {
    try {
      await almacenService.deleteAlmacen(id)
      queryClient.invalidateQueries({ queryKey: ['almacenes'] })
      toast({ title: 'Almacén eliminado', status: 'success', duration: 2000, isClosable: true })
    } catch {
      toast({ title: 'Error al eliminar', status: 'error', duration: 3000, isClosable: true })
    }
  }

  if (isLoading) return <Text>Cargando almacenes...</Text>
  if (error) return <Text>Error al cargar almacenes</Text>

  return (
    <VStack spacing={4} align="stretch">
      <TableContainer>
        <Table variant="simple">
          <Thead>
            <Tr>
              <Th>Capacidad</Th>
              <Th>¿Principal?</Th>
              <Th>Horario</Th>
              <Th>Posición</Th>
              <Th>Acciones</Th>
            </Tr>
          </Thead>
          <Tbody>
            {currentAlmacenes.map((a) => (
              <Tr key={a.id}>
                <Td>
                    {a.esPrincipal
                        ? '—'
                        : `${a.capacidadEfectivam3} m3`}
                </Td>


                <Td>{a.esPrincipal ? 'Sí' : 'No'}</Td>
                <Td>{a.horarioAbastecimiento}</Td>
                <Td>{`${a.posicionX}, ${a.posicionY}`}</Td>
                <Td>
                  <HStack spacing={2}>
                    <IconButton aria-label="Editar" icon={<EditIcon />} onClick={() => onAlmacenSelect(a)} />
                    <IconButton aria-label="Eliminar" icon={<DeleteIcon />} colorScheme="red" onClick={() => handleDelete(a.id)} />
                  </HStack>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </TableContainer>

      {/* Paginación alineada a la derecha con control de filas */}
      <HStack justify="flex-end" mt={4} spacing={6}>
        <HStack>
          <Text>Filas por página:</Text>
          <Select
            width="auto"
            value={rowsPerPage}
            onChange={(e) => {
              setRowsPerPage(Number(e.target.value))
              setCurrentPage(1)
            }}
          >
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={15}>15</option>
            <option value={20}>20</option>
          </Select>
        </HStack>

        <HStack>
          <Button onClick={() => setCurrentPage(p => Math.max(p - 1, 1))} isDisabled={currentPage === 1}>Anterior</Button>
          <Text>Página {currentPage} de {totalPages}</Text>
          <Button onClick={() => setCurrentPage(p => Math.min(p + 1, totalPages))} isDisabled={currentPage === totalPages}>Siguiente</Button>
        </HStack>
      </HStack>
    </VStack>
  )
}
