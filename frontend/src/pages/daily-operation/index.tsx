import { Box, Text, Flex, useColorModeValue, HStack, Button, InputGroup, InputLeftElement, Input, Menu, MenuButton, MenuList, MenuItem, useDisclosure, Modal, ModalOverlay, ModalContent, ModalBody, ModalHeader, useToast, VStack } from '@chakra-ui/react'
import { SectionBar } from '../../components/common/SectionBar'
import { useState, useRef, useEffect } from 'react'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoService } from '../../core/services/PedidoService'
import { IncidenciaForm } from '../../components/IncidenciaForm'
import { IncidenciaService } from '../../core/services/IncidenciaService'
import { VehiculoForm } from '../../components/VehiculosForm'
import { VehiculoService } from '../../core/services/VehiculoService'
import { OperacionProvider, useOperacion } from '../../components/common/SimulationContextDiario';
import DailyOperationControlPanel from './DailyOperationControlPanel';
import LegendPanel from '../../components/common/Legend'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFilter, faSort } from '@fortawesome/free-solid-svg-icons'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import { IndicadoresCard } from '../../components/common/cards/IndicadoresCard'
import { AlmacenCard } from '../../components/common/cards/AlmacenCard'
import type { IndicadoresSimulado } from '../../core/types/indicadores'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { FaPlus } from 'react-icons/fa'

const pedidoService = new PedidoService();
const incidenciaService = new IncidenciaService();
const vehiculoService = new VehiculoService();

const ORDER_OPTIONS = [
  { label: 'Tiempo de llegada más cercano', value: 'fechaLimite-asc' },
  { label: 'Tiempo de llegada más lejano', value: 'fechaLimite-desc' },
  { label: 'Mayor cantidad de GLP', value: 'glp-desc' },
  { label: 'Menor cantidad de GLP', value: 'glp-asc' },
];

const PedidosSection = () => {
  const { operationData, focusOnPedido } = useOperacion();
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<'todos' | 'pendiente' | 'completado'>('todos');
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS[0].value);
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const { isOpen, onOpen, onClose } = useDisclosure();

   const handleImport = async (file: File) => {
    try {
      await pedidoService.importarPedidos(file);
      toast({ title: 'Importación exitosa', status: 'success', duration: 3000 });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Error desconocido';
      toast({ title: 'Error al importar', description: errorMessage, status: 'error', duration: 4000 });
    }
  };

  // Filtrado por estado
  let pedidosFiltrados = (operationData?.pedidos || [])
    .filter((pedido) => {
      if (statusFilter === 'todos') return true;
      if (statusFilter === 'pendiente') return pedido.estado.toLowerCase() !== 'completado';
      if (statusFilter === 'completado') return pedido.estado.toLowerCase() === 'completado';
      return true;
    })
    .filter((pedido) => {
      if (!searchValue) return true;
      const codigo = `PE${pedido.idPedido.toString().padStart(3, '0')}`;
      return codigo.toLowerCase().includes(searchValue.toLowerCase());
    });

  // Ordenado
  pedidosFiltrados = [...pedidosFiltrados].sort((a, b) => {
    switch (orderBy) {
      case 'fechaLimite-asc':
        return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
      case 'fechaLimite-desc':
        return new Date(b.fechaLimite).getTime() - new Date(a.fechaLimite).getTime();
      case 'glp-asc':
        return a.glp - b.glp;
      case 'glp-desc':
        return b.glp - a.glp;
      default:
        return 0;
    }
  });

  return (
    <Box>
      <VStack spacing={2} align="stretch" bg="#e6e6ea" p={2} borderRadius="md">
        <Input
          placeholder="Buscar pedido..."
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
          borderRadius="md"
          bg="white"
          fontSize="lg"
          height="44px"
          mb={1}
          maxW="100%"
          _focus={{ borderColor: 'purple.400', boxShadow: '0 0 0 1px #805ad5' }}
        />
        <HStack spacing={2} mb={2}>
          <Menu>
            <MenuButton as={Button} leftIcon={<FontAwesomeIcon icon={faSort} />} colorScheme="purple" variant="solid" fontSize="md" height="40px" borderRadius="md">
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
          <Menu>
            <MenuButton as={Button} leftIcon={<FontAwesomeIcon icon={faFilter} />} colorScheme="purple" variant="solid" fontSize="md" height="40px" borderRadius="md">
              Filtrar
            </MenuButton>
            <MenuList>
              <MenuItem onClick={() => setStatusFilter('todos')} fontWeight={statusFilter === 'todos' ? 'bold' : 'normal'} color={statusFilter === 'todos' ? 'purple.600' : undefined}>Todos</MenuItem>
              <MenuItem onClick={() => setStatusFilter('pendiente')} fontWeight={statusFilter === 'pendiente' ? 'bold' : 'normal'} color={statusFilter === 'pendiente' ? 'purple.600' : undefined}>Pendiente</MenuItem>
              <MenuItem onClick={() => setStatusFilter('completado')} fontWeight={statusFilter === 'completado' ? 'bold' : 'normal'} color={statusFilter === 'completado' ? 'purple.600' : undefined}>Completado</MenuItem>
            </MenuList>
          </Menu>
          <Menu>
          <MenuButton as={Button} leftIcon={<FaPlus />} colorScheme="purple"  variant="solid" fontSize="md" height="40px" borderRadius="md">
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
        <VStack spacing={4} align="stretch">
          {pedidosFiltrados.length === 0 && (
            <Box color="gray.500" textAlign="center" py={6}>No hay pedidos para mostrar.</Box>
          )}
          {pedidosFiltrados.map((pedido) => (
            <PedidoCard key={pedido.idPedido} pedido={pedido} onClick={() => focusOnPedido(pedido)} />
          ))}
        </VStack>
      </VStack>
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
};

export default function DailyOperation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(false)
  /*
     * HANDLE PANEL SECTIONS
     */
  const FlotaSection = () => {
    const { operationData } = useOperacion();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {operationData?.vehiculos?.map((v) => (
            <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={() => console.log('enfocando...')} />
          ))}
        </VStack>
      </Box>
    );
  };

  const AveriaSection = () => {
    const { operationData } = useOperacion();
    const { onClose } = useDisclosure();
    const incidenciaService = new IncidenciaService();
    const toast = useToast();
    // const inputRef = useRef<HTMLInputElement>(null); // unused

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {/* Modal para crear avería */}
          {/* <ModalInsertAveria
              isOpen={isOpen}
              onClose={onClose}
              handleSubmit={handleRegister}
              plaque={''}
            />
            <Menu>
              <MenuButton
                as={Button}
                leftIcon={<FaPlus />}
                variant="secondary"
              >
                Agregar
              </MenuButton>
              <MenuList>
                <MenuItem onClick={onOpen}>Crear una avería</MenuItem>
                <MenuItem onClick={() => inputRef.current?.click()}>Importar desde archivo
                  <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={() => {}} />
                </MenuItem>
              </MenuList>
            </Menu> */}
          {operationData?.incidencias?.map((i) => (
            <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={() => console.log('enfocando...')} />
          ))}
        </VStack>
      </Box>
    );
  };

  const MantenimientoSection = () => {
    const { operationData } = useOperacion();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={() => console.log('searching...')} />
          {operationData?.mantenimientos?.map((m) => (
            <MantenimientoCard key={m.idMantenimiento} mantenimiento={m} onClick={() => console.log('enfocando...')} />
          ))}
        </VStack>
      </Box>
    );
  };

  const [vehiculosPorAlmacen, setVehiculosPorAlmacen] = useState<Record<number, Record<string, number>>>({});

  const [highlightedAlmacenId, setHighlightedAlmacenId] = useState<number | null>(null);
  const almacenSectionRef = useRef<HTMLDivElement>(null);

  // Permitir que desde fuera se pueda enfocar una card de almacén
  useEffect(() => {
    (window as any).focusAlmacenCard = (idAlmacen: number) => {
      setSection('Almacén');
      setHighlightedAlmacenId(idAlmacen);
      setTimeout(() => setHighlightedAlmacenId(null), 2000);
      // Scroll a la card si es posible
      setTimeout(() => {
        const card = document.getElementById(`almacen-card-${idAlmacen}`);
        if (card) {
          card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      }, 100);
    };
  }, []);

  const AlmacenSection = () => {
    const { operationData } = useOperacion();
    const focusOnAlmacen = (almacen: any) => {
      (window as any).highlightedWarehouseId = almacen.idAlmacen;
      setTimeout(() => {
        (window as any).highlightedWarehouseId = undefined;
      }, 3000);
    };
    return (
      <Box ref={almacenSectionRef}>
        <VStack spacing={4} align="stretch">
          {operationData?.almacenes?.map((almacen) => {
            const vehiculos = vehiculosPorAlmacen[almacen.idAlmacen] || {};
            return (
              <AlmacenCard
                key={almacen.idAlmacen}
                almacen={almacen}
                onFocus={() => focusOnAlmacen(almacen)}
                vehiculos={vehiculos}
                highlighted={highlightedAlmacenId === almacen.idAlmacen}
                id={`almacen-card-${almacen.idAlmacen}`}
              />
            );
          })}
        </VStack>
      </Box>
    );
  };

  const IndicadoresSection = () => {
    const { operationData } = useOperacion();
    const [indicadoresVisibles, setIndicadoresVisibles] = useState(operationData?.indicadores);
    const lastSetTimeRef = useRef<number>(0);

    useEffect(() => {
      const now = Date.now();
      if (
        operationData?.indicadores &&
        (now - lastSetTimeRef.current >= 5000 || lastSetTimeRef.current === 0)
      ) {
        setIndicadoresVisibles(operationData.indicadores);
        lastSetTimeRef.current = now;
      }
      // Si no han pasado 5 segundos, no actualiza el estado y por lo tanto no re-renderiza
    }, [operationData?.indicadores]);

    const staticIndicadores: IndicadoresSimulado = {
      fuelCounterTA: 1200,
      fuelCounterTB: 95.45,
      fuelCounterTC: 80.00,
      fuelCounterTD: 60.32,
      fuelCounterTotal: 3550,
      glpFilledNorth: 10,
      glpFilledEast: 210,
      glpFilledMain: 401,
      glpFilledTotal: 600,
      meanDeliveryTime: 42,
      completedOrders: 38,
      totalOrders: 45,
    };

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {indicadoresVisibles && (
            <IndicadoresCard key={'indicadores-default'} indicadores={indicadoresVisibles} />
          )}
          {/* {staticIndicadores && (
            <IndicadoresCard key={'indicadores-default'} indicadores={staticIndicadores} />
          )} */}
        </VStack>
      </Box>
    );
  };

  const sections = [
    {
      title: 'Pedidos',
      content: <PedidosSection />
    },
    {
      title: 'Flota',
      content: <FlotaSection />
    },
    {
      title: 'Averias',
      content: <AveriaSection />
    },
    {
      title: 'Mantenimiento',
      content: <MantenimientoSection />
    },
    {
      title: 'Almacén',
      content: <AlmacenSection />
    },
    {
      title: 'Indicadores',
      content: <IndicadoresSection />
    },
  ];

  const [section, setSection] = useState(sections[0].title);
  const { operationData, setOperationData } = useOperacion();
  const handleSectionChange = (section: string) => {
    setSection(section)
  }

  useEffect(() => {
    if (isCollapsed) {
      setSection('')
    }
  }, [isCollapsed]);

  return (
      <Flex height="100%" width="100%" overflowY="hidden" position="relative">
        <Box flex={1} p={0} bg={bgColor} h="full">
          <DailyOperationControlPanel data={operationData} setData={setOperationData} />
        </Box>
        <SectionBar
          sections={sections}
          onSectionChange={handleSectionChange}
          currentSection={section}
          isCollapsed={isCollapsed}
          onToggleCollapse={() => setIsCollapsed(!isCollapsed)}
        />
        <LegendPanel isSidebarCollapsed={isCollapsed} />
      </Flex>
  )
}
