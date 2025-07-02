import { Box, Text, Flex, useColorModeValue, HStack, Button, InputGroup, InputLeftElement, Input, Menu, MenuButton, MenuList, MenuItem, useDisclosure, Modal, ModalOverlay, ModalContent, ModalBody, ModalHeader, useToast } from '@chakra-ui/react'
import { SectionBar } from '../../components/common/SectionBar'
import { useState, useRef } from 'react'
import { FaSearch, FaSort, FaFilter, FaPlus } from 'react-icons/fa'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoService } from '../../core/services/PedidoService'
import { IncidenciaForm } from '../../components/IncidenciaForm'
import { IncidenciaService } from '../../core/services/IncidenciaService'
import { VehiculoForm } from '../../components/VehiculosForm'
import { VehiculoService } from '../../core/services/VehiculoService'
import { OperacionProvider, useOperacion } from '../../components/common/SimulationContextDiario';
import DailyOperationControlPanel from './DailyOperationControlPanel';

const pedidoService = new PedidoService();
const incidenciaService = new IncidenciaService();
const vehiculoService = new VehiculoService();

function PedidosPanel() {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [searchValue, setSearchValue] = useState('');

  const handleImport = async (file: File) => {
    try {
      await pedidoService.importarPedidos(file);
      toast({ title: 'Importación exitosa', status: 'success', duration: 3000 });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Error desconocido';
      toast({ title: 'Error al importar', description: errorMessage, status: 'error', duration: 4000 });
    }
  };

  return (
    <Box>
      {/* Barra de búsqueda */}
      <InputGroup mb={3}>
        <InputLeftElement pointerEvents="none">
          <FaSearch color="gray.400" />
        </InputLeftElement>
        <Input
          placeholder="Buscar pedido..."
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
          borderRadius="md"
          bg="white"
          size="md"
        />
      </InputGroup>
      {/* Botones de acción */}
      <HStack spacing={2} mb={4}>
        <Button leftIcon={<FaSort />} colorScheme="purple" variant="solid" size="sm">Ordenar</Button>
        <Button leftIcon={<FaFilter />} colorScheme="purple" variant="solid" size="sm">Filtrar</Button>
        <Menu>
          <MenuButton as={Button} leftIcon={<FaPlus />} colorScheme="purple" size="sm">
            Agregar
          </MenuButton>
          <MenuList>
            <MenuItem onClick={onOpen}>Crear un pedido</MenuItem>
            <MenuItem onClick={() => inputRef.current?.click()}>Importar desde archivo
              <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={e => {
                if (e.target.files && e.target.files[0]) {
                  handleImport(e.target.files[0]);
                  e.target.value = '';
                }
              }} />
            </MenuItem>
          </MenuList>
        </Menu>
      </HStack>
      {/* Modal para crear pedido */}
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Registrar Pedido</ModalHeader>
          <ModalBody>
            <PedidoForm onFinish={onClose} onCancel={onClose} />
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  );
}

function AveriasPanel() {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [searchValue, setSearchValue] = useState('');

  return (
    <Box>
      {/* Barra de búsqueda */}
      <InputGroup mb={3}>
        <InputLeftElement pointerEvents="none">
          <FaSearch color="gray.400" />
        </InputLeftElement>
        <Input
          placeholder="Buscar avería..."
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
          borderRadius="md"
          bg="white"
          size="md"
        />
      </InputGroup>
      {/* Botones de acción */}
      <HStack spacing={2} mb={4}>
        <Button leftIcon={<FaSort />} colorScheme="purple" variant="solid" size="sm">Ordenar</Button>
        <Button leftIcon={<FaFilter />} colorScheme="purple" variant="solid" size="sm">Filtrar</Button>
        <Menu>
          <MenuButton as={Button} leftIcon={<FaPlus />} colorScheme="purple" size="sm">
            Agregar
          </MenuButton>
          <MenuList>
            <MenuItem onClick={onOpen}>Crear una avería</MenuItem>
            <MenuItem onClick={() => inputRef.current?.click()}>Importar desde archivo
              <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={() => {}} />
            </MenuItem>
          </MenuList>
        </Menu>
      </HStack>
      {/* Modal para crear avería */}
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Registrar Avería</ModalHeader>
          <ModalBody>
            <IncidenciaForm onFinish={onClose} onCancel={onClose} />
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  );
}

function FlotaPanel() {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const [searchValue, setSearchValue] = useState('');

  return (
    <Box>
      {/* Barra de búsqueda */}
      <InputGroup mb={3}>
        <InputLeftElement pointerEvents="none">
          <FaSearch color="gray.400" />
        </InputLeftElement>
        <Input
          placeholder="Buscar vehículo..."
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
          borderRadius="md"
          bg="white"
          size="md"
        />
      </InputGroup>
      {/* Botones de acción */}
      <HStack spacing={2} mb={4}>
        <Button leftIcon={<FaSort />} colorScheme="purple" variant="solid" size="sm">Ordenar</Button>
        <Button leftIcon={<FaFilter />} colorScheme="purple" variant="solid" size="sm">Filtrar</Button>
        <Button leftIcon={<FaPlus />} colorScheme="purple" size="sm" onClick={onOpen}>Agregar</Button>
      </HStack>
      {/* Modal para crear vehículo */}
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Registrar Vehículo</ModalHeader>
          <ModalBody>
            <VehiculoForm onFinish={onClose} onCancel={onClose} />
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  );
}

const sections = [
  {
    title: 'Pedidos',
    content: <PedidosPanel />
  },
  {
    title: 'Flota',
    content: <FlotaPanel />
  },
  {
    title: 'Averias',
    content: <AveriasPanel />
  },
  {
    title: 'Mantenimiento',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Mantenimiento
        </Text>
      </Box>
    )
  },
  {
    title: 'Indicadores',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Indicadores
        </Text>
      </Box>
    )
  }
]

export default function DailyOperation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [currentSection, setCurrentSection] = useState('Pedidos')
  const [isCollapsed, setIsCollapsed] = useState(false)
  // State for real-time operation data
  const [operationData, setOperationData] = useState<any>(null);

  return (
    <OperacionProvider>
    <Flex height="full" overflowY="auto" position="relative">
      <Box flex={1} p={4} bg={bgColor} h="full">
          <DailyOperationControlPanel data={operationData} setData={setOperationData} />
      </Box>
      <SectionBar
        sections={sections}
        onSectionChange={setCurrentSection}
        currentSection={currentSection}
        isCollapsed={isCollapsed}
        onToggleCollapse={() => setIsCollapsed(!isCollapsed)}
      />
    </Flex>
    </OperacionProvider>
  )
}
