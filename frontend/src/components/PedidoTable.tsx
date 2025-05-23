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
  VStack,
  useToast
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import type { Pedido } from '../core/types/pedido'
import { PedidoService } from '../core/services/PedidoService'
import { format } from 'date-fns'
import { useQuery, useQueryClient } from '@tanstack/react-query'

const pedidoService = new PedidoService()

interface PedidoTableProps {
  onPedidoSelect?: (pedido: Pedido) => void
}

export const PedidoTable = ({ onPedidoSelect }: PedidoTableProps) => {
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: pedidos, isLoading, error } = useQuery<Pedido[]>({
    queryKey: ['pedidos'],
    queryFn: () => pedidoService.getAllPedidos(),
    retry: 1
  })

  const handleDelete = async (id: number) => {
    try {
      await pedidoService.deletePedido(id)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      toast({
        title: 'Success',
        description: 'Pedido deleted successfully',
        status: 'success',
        duration: 3000,
        isClosable: true,
      })
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Error deleting pedido',
        status: 'error',
        duration: 3000,
        isClosable: true,
      })
    }
  }

  if (isLoading) {
    return <Text>Loading...</Text>
  }

  if (error) {
    return <Text>Error loading pedidos</Text>
  }

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <TableContainer>
          <Table variant="simple">
            <Thead>
              <Tr>
                <Th>Código Cliente</Th>
                <Th>Fecha Registro</Th>
                <Th>Posición</Th>
                <Th>Cantidad GLP</Th>
                <Th>Tiempo Tolerancia</Th>
                <Th>Acciones</Th>
              </Tr>
            </Thead>
            <Tbody>
              {pedidos?.map((pedido) => (
                <Tr key={pedido.id}>
                  <Td>{pedido.codigoCliente}</Td>
                  <Td>{format(new Date(pedido.fechaRegistro), 'dd/MM/yyyy HH:mm')}</Td>
                  <Td>{`${pedido.posicionX}, ${pedido.posicionY}`}</Td>
                  <Td>{pedido.cantidadGLP}</Td>
                  <Td>{pedido.tiempoTolerancia}</Td>
                  <Td>
                    <HStack spacing={2}>
                      <IconButton
                        aria-label="Editar"
                        icon={<EditIcon />}
                        onClick={() => onPedidoSelect?.(pedido)}
                      />
                      <IconButton
                        aria-label="Eliminar"
                        icon={<DeleteIcon />}
                        colorScheme="red"
                        onClick={() => handleDelete(pedido.id)}
                      />
                    </HStack>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </TableContainer>
      </VStack>
    </Box>
  )
}