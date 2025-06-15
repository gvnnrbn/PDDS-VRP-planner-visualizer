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
  Select,
  InputGroup,
  InputLeftElement,
  Input,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  Flex
} from '@chakra-ui/react'
import { DeleteIcon, EditIcon } from '@chakra-ui/icons'
import type { Incidencia } from '../core/types/incidencia'
import { IncidenciaService } from '../core/services/IncidenciaService'
import { format } from 'date-fns'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { FaSearch, FaSort } from 'react-icons/fa'

const incidenciaService = new IncidenciaService()

interface IncidenciaTableProps {
  onIncidenciaSelect?: (incidencia: Incidencia) => void
  onNuevaIncidencia?: () => void
}

const ORDER_OPTIONS = [
  { label: 'Fecha más reciente', value: 'fecha-desc' },
  { label: 'Fecha más lejana', value: 'fecha-asc' },
  { label: 'Id vehículo mayor a menor', value: 'vehiculo-desc' },
  { label: 'Id vehículo menor a mayor', value: 'vehiculo-asc' },
]

export const IncidenciaTable = ({ onIncidenciaSelect, onNuevaIncidencia }: IncidenciaTableProps) => {
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: incidencias, isLoading, error } = useQuery<Incidencia[]>({
    queryKey: ['incidencias'],
    queryFn: () => incidenciaService.getAllIncidencias(),
    retry: 1
  })

  const [currentPage, setCurrentPage] = useState(1)
  const [rowsPerPage, setRowsPerPage] = useState(10)
  const [searchValue, setSearchValue] = useState('')
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS[0].value)
  const [turnoFilter, setTurnoFilter] = useState('Todos')
  const [ocurridoFilter, setOcurridoFilter] = useState('Todos')

  // Filtrado por búsqueda, turno y ocurrido
  const filteredIncidencias = useMemo(() => {
    return (incidencias || []).filter(inc => {
      // Búsqueda
      const search = searchValue.toLowerCase()
      const fechaStr = format(new Date(inc.fecha), 'yyyy-MM-dd')
      const turnoStr = inc.turno?.toLowerCase() || ''
      const vehiculoStr = inc.vehiculo?.id?.toString() || ''
      const matchSearch =
        fechaStr.includes(search) ||
        turnoStr.includes(search) ||
        vehiculoStr.includes(search)
      // Filtro turno
      const matchTurno = turnoFilter === 'Todos' || inc.turno === turnoFilter
      // Filtro ocurrido
      const matchOcurrido =
        ocurridoFilter === 'Todos' ||
        (ocurridoFilter === 'Sí' && inc.ocurrido) ||
        (ocurridoFilter === 'No' && !inc.ocurrido)
      return matchSearch && matchTurno && matchOcurrido
    })
  }, [incidencias, searchValue, turnoFilter, ocurridoFilter])

  // Ordenado
  const sortedIncidencias = useMemo(() => {
    return [...filteredIncidencias].sort((a, b) => {
      switch (orderBy) {
        case 'fecha-desc':
          return new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
        case 'fecha-asc':
          return new Date(a.fecha).getTime() - new Date(b.fecha).getTime()
        case 'vehiculo-desc':
          return (b.vehiculo?.id || 0) - (a.vehiculo?.id || 0)
        case 'vehiculo-asc':
          return (a.vehiculo?.id || 0) - (b.vehiculo?.id || 0)
        default:
          return 0
      }
    })
  }, [filteredIncidencias, orderBy])

  // Paginación
  const indexOfLastRow = currentPage * rowsPerPage
  const indexOfFirstRow = indexOfLastRow - rowsPerPage
  const currentIncidencias = sortedIncidencias.slice(indexOfFirstRow, indexOfLastRow)
  const totalPages = Math.ceil(sortedIncidencias.length / rowsPerPage)

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
      {/* Barra de búsqueda, orden y filtros */}
      <Flex mb={4} mt={4} align="center" gap={4} wrap="wrap">
        <InputGroup maxW="300px">
          <InputLeftElement pointerEvents="none">
            <FaSearch color="gray.400" />
          </InputLeftElement>
          <Input
            placeholder="Buscar por fecha, turno, vehículo..."
            value={searchValue}
            onChange={e => { setSearchValue(e.target.value); setCurrentPage(1); }}
            borderRadius="md"
            bg="white"
            size="md"
          />
        </InputGroup>
        <Menu>
          <MenuButton as={Button} leftIcon={<FaSort />} colorScheme="purple" variant="solid" size="md">
            Ordenar
          </MenuButton>
          <MenuList>
            {ORDER_OPTIONS.map(opt => (
              <MenuItem
                key={opt.value}
                onClick={() => { setOrderBy(opt.value); setCurrentPage(1); }}
                color={orderBy === opt.value ? 'purple.600' : undefined}
                fontWeight={orderBy === opt.value ? 'bold' : 'normal'}
              >
                {opt.label}
              </MenuItem>
            ))}
          </MenuList>
        </Menu>
        <Text fontWeight="semibold" color="purple.700" ml={2}>Turno:</Text>
        <Select
          placeholder="Todos"
          value={turnoFilter}
          onChange={e => { setTurnoFilter(e.target.value); setCurrentPage(1); }}
          maxW="120px"
          size="md"
          colorScheme="purple"
          variant="outline"
        >
          <option value="Todos">Todos</option>
          <option value="T1">T1</option>
          <option value="T2">T2</option>
          <option value="T3">T3</option>
        </Select>
        <Text fontWeight="semibold" color="purple.700" ml={2}>Ocurrido:</Text>
        <Select
          placeholder="Todos"
          value={ocurridoFilter}
          onChange={e => { setOcurridoFilter(e.target.value); setCurrentPage(1); }}
          maxW="120px"
          size="md"
          colorScheme="purple"
          variant="outline"
        >
          <option value="Todos">Todos</option>
          <option value="Sí">Sí</option>
          <option value="No">No</option>
        </Select>
        <Box flex="1" />
        {onNuevaIncidencia && (
          <Button colorScheme="purple" size="md" fontWeight="bold" onClick={onNuevaIncidencia}>
            Nueva Incidencia
          </Button>
        )}
      </Flex>
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
