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
  Box,
  Text,
  Select,
  useToast
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { PedidoService } from '../core/services/PedidoService'
import { format } from 'date-fns'
import type { Pedido } from '../core/types/pedido'

const pedidoService = new PedidoService()

export const PedidoTable = ({ onPedidoSelect }: { onPedidoSelect: (pedido: Pedido) => void }) => {
  const queryClient = useQueryClient()
  const toast = useToast()
  const { data: pedidos, isLoading, error } = useQuery<Pedido[]>({
    queryKey: ['pedidos'],
    queryFn: () => pedidoService.getAllPedidos()
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(5)

  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentPedidos = pedidos?.slice(indexOfFirstRow, indexOfLastRow) || []
  const totalPages = Math.ceil((pedidos?.length || 0) / rowsPerPage)

  const handleDelete = async (id: number) => {
    try {
      await pedidoService.deletePedido(id)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      toast({ title: 'Eliminado', status: 'success', duration: 2000, isClosable: true })
    } catch {
      toast({ title: 'Error al eliminar', status: 'error', duration: 3000, isClosable: true })
    }
  }

  if (isLoading) return <Text>Cargando pedidos...</Text>
  if (error) return <Text>Error al cargar pedidos</Text>

  return (
    <VStack spacing={4} align="stretch">
      <TableContainer>
        <Table variant="simple">
          <Thead>
            <Tr>
              <Th>Código Cliente</Th>
              <Th>Fecha Registro</Th>
              <Th>Posición</Th>
              <Th>Cantidad GLP</Th>
              <Th>Tolerancia</Th>
              <Th>Acciones</Th>
            </Tr>
          </Thead>
          <Tbody>
            {currentPedidos.map((pedido) => (
              <Tr key={pedido.id}>
                <Td>{pedido.codigoCliente}</Td>
                <Td>{format(new Date(pedido.fechaRegistro), 'dd/MM/yyyy HH:mm')}</Td>
                <Td>{`${pedido.posicionX}, ${pedido.posicionY}`}</Td>
                <Td>{pedido.cantidadGLP}</Td>
                <Td>{pedido.tiempoTolerancia}</Td>
                <Td>
                  <HStack spacing={2}>
                    <IconButton aria-label="Editar" icon={<EditIcon />} onClick={() => onPedidoSelect(pedido)} />
                    <IconButton aria-label="Eliminar" icon={<DeleteIcon />} colorScheme="red" onClick={() => handleDelete(pedido.id)} />
                  </HStack>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </TableContainer>

      {/* Paginación centrada y control de filas */}
      <HStack justify="flex-end" mt={4} spacing={6}>
        <HStack>
          <Text>Filas por página:</Text>
          <Select
            width="auto"
            value={rowsPerPage}
            onChange={(e) => {
              setRowsPerPage(Number(e.target.value))
              setCurrentPage(1) // Reinicia a página 1 si cambia
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
