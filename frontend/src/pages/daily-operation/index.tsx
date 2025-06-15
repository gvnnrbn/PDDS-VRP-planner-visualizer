import { Box, Text, VStack, Flex, useColorModeValue, Button, HStack, useDisclosure, Modal, ModalOverlay, ModalContent, ModalBody, Menu, MenuButton, MenuList, MenuItem, useToast, Input } from '@chakra-ui/react'
import { SectionBar } from '../../components/common/SectionBar'
import { useState, useRef } from 'react'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { PedidoForm } from '../../components/PedidoForm'
import { FaPlus, FaFilter, FaSort } from 'react-icons/fa'
import { PedidoService } from '../../core/services/PedidoService'

const pedidoService = new PedidoService();

const PedidosActionsBar = ({ onAdd, onImport }: { onAdd: () => void, onImport: (file: File) => void }) => {
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <Box mb={4}>
      <PanelSearchBar onSubmit={() => {}} />
      <HStack spacing={2} mt={2}>
        <Button
          leftIcon={<FaSort />}
          colorScheme="purple"
          variant="outline"
          size="sm"
          borderRadius="md"
          fontWeight="bold"
          borderWidth="2px"
          borderColor="purple.500"
          color="purple.700"
          bg="white"
          _hover={{ bg: 'purple.100', color: 'purple.700' }}
          _active={{ bg: 'purple.500', color: 'white', borderColor: 'purple.700' }}
        >
          Ordenar
        </Button>
        <Button
          leftIcon={<FaFilter />}
          colorScheme="purple"
          variant="outline"
          size="sm"
          borderRadius="md"
          fontWeight="bold"
          borderWidth="2px"
          borderColor="purple.500"
          color="purple.700"
          bg="white"
          _hover={{ bg: 'purple.100', color: 'purple.700' }}
          _active={{ bg: 'purple.500', color: 'white', borderColor: 'purple.700' }}
        >
          Filtrar
        </Button>
        <Menu>
          <MenuButton
            as={Button}
            leftIcon={<FaPlus />}
            colorScheme="purple"
            variant="outline"
            size="sm"
            borderRadius="md"
            fontWeight="bold"
            borderWidth="2px"
            borderColor="purple.500"
            color="purple.700"
            bg="white"
            _hover={{ bg: 'purple.100', color: 'purple.700' }}
            _active={{ bg: 'purple.600', color: 'white', borderColor: 'purple.700' }}
            _expanded={{ bg: 'purple.600', color: 'white', borderColor: 'purple.700' }}
          >
            Agregar
          </MenuButton>
          <MenuList>
            <MenuItem onClick={onAdd} color="purple.700">Crear un pedido</MenuItem>
            <MenuItem onClick={() => inputRef.current?.click()} color="purple.700">Importar desde archivo
              <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={e => {
                if (e.target.files && e.target.files[0]) {
                  onImport(e.target.files[0]);
                  e.target.value = '';
                }
              }} />
            </MenuItem>
          </MenuList>
        </Menu>
      </HStack>
    </Box>
  )
}

const sections = [
  {
    title: 'Pedidos',
    content: (
      <PedidosSection />
    )
  },
  {
    title: 'Flota',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Flota
        </Text>
      </Box>
    )
  },
  {
    title: 'Averias',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Averías
        </Text>
      </Box>
    )
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

function PedidosSection() {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const handleImport = async (file: File) => {
    try {
      await pedidoService.importarPedidos(file);
      toast({ title: 'Importación exitosa', status: 'success', duration: 3000 });
    } catch (error: any) {
      toast({ title: 'Error al importar', description: error.message, status: 'error', duration: 4000 });
    }
  };
  return (
    <Box>
      <PedidosActionsBar onAdd={onOpen} onImport={handleImport} />
      {/* Aquí iría la tabla de pedidos o el contenido principal */}
      <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
        Contenido de la sección Pedidos
      </Text>
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
          <ModalBody>
            <PedidoForm onFinish={onClose} onCancel={onClose} />
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  )
}

export default function DailyOperation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [currentSection, setCurrentSection] = useState('Pedidos')
  const [isCollapsed, setIsCollapsed] = useState(false)

  return (
    <Flex height="full" overflowY="auto">
      <Box flex={1} p={4} bg={bgColor} h="full">
        <VStack spacing={4} align="stretch">
          <Text fontSize="2xl" fontWeight="bold">
            Operación día a día
          </Text>
        </VStack>
      </Box>

      <SectionBar
        sections={sections}
        onSectionChange={setCurrentSection}
        currentSection={currentSection}
        isCollapsed={isCollapsed}
        onToggleCollapse={() => setIsCollapsed(!isCollapsed)}
      />
    </Flex>
  )
}
