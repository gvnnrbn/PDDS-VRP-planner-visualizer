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
  useToast,
  InputGroup,
  InputLeftElement,
  Input,
  Menu,
  MenuButton,
  MenuList,
  MenuItem
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { PedidoService } from '../core/services/PedidoService'
import { format } from 'date-fns'
import type { Pedido } from '../core/types/pedido'
import { FaSearch, FaSort } from 'react-icons/fa'

const pedidoService = new PedidoService()

const ORDER_OPTIONS = [
  { label: 'Tiempo de llegada más cercano', value: 'fechaRegistro-asc' },
  { label: 'Tiempo de llegada más lejano', value: 'fechaRegistro-desc' },
  { label: 'Mayor cantidad de GLP', value: 'cantidadGLP-desc' },
  { label: 'Menor cantidad de GLP', value: 'cantidadGLP-asc' },
]

export const PedidoTable = ({ onPedidoSelect, onNuevoPedido, onImportarArchivo }: { onPedidoSelect: (pedido: Pedido) => void, onNuevoPedido?: () => void, onImportarArchivo?: () => void }) => {
  const queryClient = useQueryClient()
  const toast = useToast()
  const { data: pedidos, isLoading, error } = useQuery<Pedido[]>({
    queryKey: ['pedidos'],
    queryFn: () => pedidoService.getAllPedidos()
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(5)
  const [searchValue, setSearchValue] = useState('')
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS[0].value)

  // Filtrado por búsqueda
  const filteredPedidos = (pedidos || []).filter(pedido => {
    const search = searchValue.toLowerCase()
    return (
      pedido.codigoCliente.toLowerCase().includes(search) ||
      format(new Date(pedido.fechaRegistro), 'dd/MM/yyyy HH:mm').includes(search) ||
      `${pedido.posicionX},${pedido.posicionY}`.includes(search)
    )
  })

  // Ordenado
  const sortedPedidos = [...filteredPedidos].sort((a, b) => {
    switch (orderBy) {
      case 'fechaRegistro-asc':
        return new Date(a.fechaRegistro).getTime() - new Date(b.fechaRegistro).getTime()
      case 'fechaRegistro-desc':
        return new Date(b.fechaRegistro).getTime() - new Date(a.fechaRegistro).getTime()
      case 'cantidadGLP-asc':
        return a.cantidadGLP - b.cantidadGLP
      case 'cantidadGLP-desc':
        return b.cantidadGLP - a.cantidadGLP
      default:
        return 0
    }
  })

  // Paginación
  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentPedidos = sortedPedidos.slice(indexOfFirstRow, indexOfLastRow)
  const totalPages = Math.ceil(sortedPedidos.length / rowsPerPage)

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
      {/* Barra de búsqueda, ordenar y acciones */}
      <HStack spacing={4} mb={4} align="center" width="100%" justify="space-between">
        <HStack spacing={2} flex={1} maxW="700px">
          <InputGroup maxW="700px" flex={1}>
            <InputLeftElement pointerEvents="none">
              <FaSearch color="gray.400" />
            </InputLeftElement>
            <Input
              placeholder="Buscar por cliente, fecha, etc."
              value={searchValue}
              onChange={e => setSearchValue(e.target.value)}
              borderRadius="md"
              bg="white"
              fontSize="lg"
              height="48px"
            />
          </InputGroup>
          <Menu>
            <MenuButton as={Button} leftIcon={<FaSort />} colorScheme="purple" variant="solid" fontSize="md" height="48px">
              Ordenar
            </MenuButton>
            <MenuList>
              {ORDER_OPTIONS.map(opt => (
                <MenuItem
                  key={opt.value}
                  onClick={() => setOrderBy(opt.value)}
                  color={orderBy === opt.value ? 'purple.600' : undefined}
                  fontWeight={orderBy === opt.value ? 'bold' : 'normal'}
                >
                  {opt.label}
                </MenuItem>
              ))}
            </MenuList>
          </Menu>
        </HStack>
        <HStack spacing={2}>
          {onNuevoPedido && (
            <Button colorScheme="purple" size="md" onClick={onNuevoPedido} fontWeight="bold">
              Nuevo Pedido
            </Button>
          )}
          {onImportarArchivo && (
            <Button colorScheme="teal" size="md" onClick={onImportarArchivo} fontWeight="bold">
              Importar archivo
            </Button>
          )}
        </HStack>
      </HStack>
      <TableContainer>
        <Table variant="simple">
          <Thead>
            <Tr>
              <Th>Código Cliente</Th>
              <Th>Fecha Ingreso</Th>
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
