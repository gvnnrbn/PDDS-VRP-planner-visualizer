import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, VStack, HStack, useDisclosure, useToast, Menu, MenuList, MenuItem, MenuButton, Collapse, IconButton } from '@chakra-ui/react'
import { Route, Routes } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useRef, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import SimulationControlPanel from './SimulationControlPanel'
import { IncidenciaService } from '../../core/services/IncidenciaService'
import { IndicadoresCard } from '../../components/common/cards/IndicadoresCard'
import { useSimulation } from '../../components/common/SimulationContextSemanal'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFilter, faSort } from '@fortawesome/free-solid-svg-icons'
import { AlmacenCard } from '../../components/common/cards/AlmacenCard'
import { ChevronDownIcon, ChevronUpIcon } from '@chakra-ui/icons'

const ORDER_OPTIONS = [
  { label: 'Tiempo de llegada más cercano', value: 'fechaLimite-asc' },
  { label: 'Tiempo de llegada más lejano', value: 'fechaLimite-desc' },
  { label: 'Mayor cantidad de GLP', value: 'glp-desc' },
  { label: 'Menor cantidad de GLP', value: 'glp-asc' },
];

const PedidosSection = () => {
  const { currentMinuteData, focusOnPedido } = useSimulation();
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<'todos' | 'pendiente' | 'completado'>('todos');
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS[0].value);

  // Filtrado por estado
  let pedidosFiltrados = (currentMinuteData?.pedidos || [])
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
    </Box>
  );
};

export default function WeeklySimulation() {
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [selectedDateCount, setSelectedDateCount] = useState(() => {
    return localStorage.getItem('vrp_sim_start_date') || '';
  });
  const [isModalOpen, setIsModalOpen] = useState(false);
  // const [ speedMs, setSpeedMs ] = useState(1000); // unused
  

  const [dateValue, setDateValue] = useState<string>(() => {
    const now = new Date();
    return now.toISOString().split('T')[0];
  });
  const [hourValue, setHourValue] = useState<number>(new Date().getHours());
  const [minuteValue, setMinuteValue] = useState<number>(new Date().getMinutes());

  /**
   * HANDLE DATE
   */
  const formatDateTime = () => {
    const [year, month, day] = dateValue.split('-');
    const formattedDate = `${year}-${month}-${day}T${String(hourValue).padStart(2, '0')}:${String(minuteValue).padStart(2, '0')}`;
    setSelectedDateCount(formattedDate);
  };

  useEffect(() => {
    formatDateTime();
  }, [dateValue, hourValue, minuteValue]);

  /**
   * START SIMULATION
  */

  // const [pendingStart, setPendingStart] = useState(false); // unused

  const handleSubmit = () => {
    // Usa el valor ISO del input tipo date o datetime-local
    // dateValue es yyyy-MM-dd, hourValue y minuteValue son números
    const formattedStartDate = `${dateValue}T${String(hourValue).padStart(2, '0')}:${String(minuteValue).padStart(2, '0')}:00`;
    localStorage.setItem('vrp_sim_start_date', formattedStartDate);
    setSelectedDateCount(formattedStartDate);

    setIsModalOpen(false);
    setIsSimulationLoading(true);
    // ⏳ esperar conexión
  };
 

  /*
   * HANDLE PANEL SECTIONS
   */
  const FlotaSection = () => {
    const { currentMinuteData } = useSimulation();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {currentMinuteData?.vehiculos?.map((v) => (
            <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={()=>console.log('enfocando...')}/>
          ))}
        </VStack>
      </Box>
    );
  };

  const AveriaSection = () => {
    const { currentMinuteData } = useSimulation();
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
          {currentMinuteData?.incidencias?.map((i) => (
            <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={()=>console.log('enfocando...')}/>
          ))}
        </VStack>
      </Box>
    );
  };

  const MantenimientoSection = () => {
    const { currentMinuteData } = useSimulation();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={() => console.log('searching...')} />
          {currentMinuteData?.mantenimientos?.map((m) => (
            <MantenimientoCard key={m.idMantenimiento} mantenimiento={m} onClick={()=>console.log('enfocando...')}/>
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
          card.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 100);
    };
  }, []);

  const AlmacenSection = () => {
    const { currentMinuteData } = useSimulation();
    const focusOnAlmacen = (almacen: any) => {
      (window as any).highlightedWarehouseId = almacen.idAlmacen;
      setTimeout(() => {
        (window as any).highlightedWarehouseId = undefined;
      }, 3000);
    };
    return (
      <Box ref={almacenSectionRef}>
        <VStack spacing={4} align="stretch">
          {currentMinuteData?.almacenes?.map((almacen) => {
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
    const { currentMinuteData } = useSimulation();
     const [indicadoresVisibles, setIndicadoresVisibles] = useState(currentMinuteData?.indicadores);

    useEffect(() => {
      const interval = setInterval(() => {
        setIndicadoresVisibles(currentMinuteData?.indicadores);
      }, 2000000); // cada 10 segundos

      return () => clearInterval(interval); // limpieza al desmontar
    }, [currentMinuteData?.indicadores]); // reinicia si cambian los indicadores (opcional)

    const indicadores = indicadoresVisibles?? {
      fuelCounterTA: 0,
      fuelCounterTB: 0,
      fuelCounterTC: 0,
      fuelCounterTD: 0,
      fuelCounterTotal: 0,
      glpFilledNorth: 0,
      glpFilledEast: 0,
      glpFilledMain: 0,
      glpFilledTotal: 0,
      meanDeliveryTime: 0,
      completedOrders: 0,
      totalOrders: 0,
    };
    return (
      <Box>
        <VStack spacing={4} align="stretch">
            <IndicadoresCard key={'indicadores-default'} indicadores={indicadores}/>
        </VStack>
      </Box>
    );
  };

  const sections = [
  {
    title: 'Pedidos',
    content: <PedidosSection/>
  },
  {
    title: 'Flota',
    content: <FlotaSection/>
  },
  {
    title: 'Averias',
    content: <AveriaSection/>
  },
  {
    title: 'Mantenimiento',
    content: <MantenimientoSection/>
  },
  {
    title: 'Almacén',
    content: <AlmacenSection/>
  },
  {
    title: 'Indicadores',
    content: <IndicadoresSection/>
  },
  ];

  const [section, setSection] = useState(sections[0].title);
  const { currentMinuteData, setCurrentMinuteData } = useSimulation();

  const handleSectionChange = (section: string) => {
    setSection(section)
  }
  useEffect(() => {
    if(isCollapsed){
      setSection('')
    }
  }, [isCollapsed]);

  return (
    <Flex height="100%" width="100%" overflowY="hidden" position="relative">
      <Box
        position="absolute"
        top="0"
        left="0"
        right="0"
        bottom="0"
        zIndex={0}
      >
        <Routes>
          <Route
            path="simulacion"
            element={
              isSimulationLoading 
                ? <></> 
                : (
                    <SimulationControlPanel 
                      setData={setCurrentMinuteData} 
                      data={currentMinuteData}
                      startDate={selectedDateCount}
                      onVehiculosPorAlmacenUpdate={setVehiculosPorAlmacen}
                    />
                )
            }
          />
        </Routes>
      </Box>
      {!isSimulationLoading && (
        <>
          <SectionBar
            sections={sections}
            onSectionChange={handleSectionChange}
            currentSection={section}
            isCollapsed={isCollapsed}
            onToggleCollapse={() => setIsCollapsed(!isCollapsed)}

          />

          <LegendPanel isSidebarCollapsed={isCollapsed} />

          
        </>
      )}
      <Modal isOpen={isModalOpen} onClose={()=>{}}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Seleccione la fecha de inicio de la simulación</ModalHeader>
          <ModalBody pb={6}>
            <Stack spacing={4}>
              <FormControl>
                <FormLabel>Fecha</FormLabel>
                <Input
                  type="date"
                  value={dateValue}
                  onChange={(e) => setDateValue(e.target.value)}
                />
              </FormControl>

              <FormControl>
                <FormLabel>Hora</FormLabel>
                <NumberInput
                  min={0}
                  max={23}
                  value={hourValue}
                  onChange={(_, value) => setHourValue(value)}
                >
                  <NumberInputField />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </FormControl>

              <FormControl>
                <FormLabel>Minutos</FormLabel>
                <NumberInput
                  min={0}
                  max={59}
                  value={minuteValue}
                  onChange={(_, value) => setMinuteValue(value)}
                >
                  <NumberInputField />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </FormControl>
            </Stack>
          </ModalBody>

          <ModalFooter justifyContent={'center'}>
            <Button variant={'primary'} mr={3} onClick={handleSubmit} disabled={!true}>
              Iniciar Simulación
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
      <LoadingOverlay isVisible={isSimulationLoading} />
    </Flex>
  );
}
