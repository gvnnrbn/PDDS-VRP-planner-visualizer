import { Box, Text, Flex, useColorModeValue, HStack, Button, Input, Menu, MenuButton, MenuList, MenuItem, useDisclosure, Modal, ModalOverlay, ModalContent, ModalBody, ModalHeader, useToast, VStack } from '@chakra-ui/react'
import { SectionBar } from '../../components/common/SectionBar'
import { useState, useRef, useEffect } from 'react'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoService } from '../../core/services/PedidoService'
import { useOperacion } from '../../components/common/SimulationContextDiario';
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
import { FaPlus } from 'react-icons/fa'
import { BloqueoCard } from '../../components/common/cards/BloqueosCard'
import { ModalInsertAveria } from '../../components/common/modals/ModalInsertAveria'

const pedidoService = new PedidoService();

const ORDER_OPTIONS_PEDIDOS = [
  { label: 'Tiempo de llegada más cercano', value: 'fechaLimite-asc' },
  { label: 'Tiempo de llegada más lejano', value: 'fechaLimite-desc' },
  { label: 'Mayor cantidad de GLP', value: 'glp-desc' },
  { label: 'Menor cantidad de GLP', value: 'glp-asc' },
];
const ORDER_OPTIONS_VEHICULOS = [
  { label: 'Menor cantidad de combustible', value: 'combustible-asc' },
  { label: 'Mayor cantidad de combustible', value: 'combustible-desc' },
  { label: 'Mayor cantidad de GLP', value: 'glp-desc' },
  { label: 'Menor cantidad de GLP', value: 'glp-asc' },
];

const PedidosSection = () => {
  const { operationData, focusOnPedido } = useOperacion();
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<'todos' | 'pendiente' | 'completado'>('todos');
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS_PEDIDOS[0].value);
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
              {ORDER_OPTIONS_PEDIDOS.map(opt => (
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

const FlotaSection = () => {
  const { operationData, focusOnVehiculo } = useOperacion();
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<'todos' | 'enruta' | 'averiado' | 'sinprogramacion' | 'mantenimiento'>('todos');
  const [orderBy, setOrderBy] = useState(ORDER_OPTIONS_VEHICULOS[0].value);

  // Filtrado por estado
  let vehiculosFiltrados = (operationData?.vehiculos || [])
    .filter((v) => {
      if (statusFilter === 'todos') return true;
      if (statusFilter === 'enruta') return v.estado.toLowerCase() === 'ontheway';
      if (statusFilter === 'sinprogramacion') return v.estado.toLowerCase() === 'idle' || v.estado.toLowerCase() === 'finished';
      if (statusFilter === 'averiado') return v.estado.toLowerCase() === 'stuck' || v.estado.toLowerCase() === 'repair';
      if (statusFilter === 'mantenimiento') return v.estado.toLowerCase() === 'maintenance';
      return true;
    })
    .filter((v) => {
      if (!searchValue) return true;
      const codigo = `${v.tipo}${v.idVehiculo.toString().padStart(3, '0')}`;
      return codigo.toLowerCase().includes(searchValue.toLowerCase());
    });

  // Ordenado
  vehiculosFiltrados = [...vehiculosFiltrados].sort((a, b) => {
    switch (orderBy) {
      case 'combustible-asc':
        return a.combustible - b.combustible;
      case 'combustible-desc':
        return b.combustible - a.combustible;
      case 'glp-asc':
        return a.currGLP - b.currGLP;
      case 'glp-desc':
        return b.currGLP - a.currGLP;
      default:
        return 0;
    }
  });

  return (
    <Box>
      <VStack spacing={2} align="stretch" bg="#e6e6ea" p={2} borderRadius="md">
        <Input
          placeholder="Buscar vehículo por placa..."
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
              {ORDER_OPTIONS_VEHICULOS.map(opt => (
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
              <MenuItem onClick={() => setStatusFilter('sinprogramacion')} fontWeight={statusFilter === 'sinprogramacion' ? 'bold' : 'normal'} color={statusFilter === 'sinprogramacion' ? 'purple.600' : undefined}>Sin Programación</MenuItem>
              <MenuItem onClick={() => setStatusFilter('enruta')} fontWeight={statusFilter === 'enruta' ? 'bold' : 'normal'} color={statusFilter === 'enruta' ? 'purple.600' : undefined}>En Ruta</MenuItem>
              <MenuItem onClick={() => setStatusFilter('averiado')} fontWeight={statusFilter === 'averiado' ? 'bold' : 'normal'} color={statusFilter === 'averiado' ? 'purple.600' : undefined}>Averiado</MenuItem>
              <MenuItem onClick={() => setStatusFilter('mantenimiento')} fontWeight={statusFilter === 'mantenimiento' ? 'bold' : 'normal'} color={statusFilter === 'mantenimiento' ? 'purple.600' : undefined}>En Mantenimiento</MenuItem>
            </MenuList>
          </Menu>
        </HStack>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {vehiculosFiltrados.length === 0 && (
            <Box color="gray.500" textAlign="center" py={6}>No hay vehículos para mostrar.</Box>
          )}
          {vehiculosFiltrados.map((v) => (
            <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={() => focusOnVehiculo(v)} />
          ))}
        </VStack>
      </VStack>
    </Box>
  );
};

const MantenimientoSection = () => {
  const { operationData } = useOperacion();

  const noData =
    !operationData?.mantenimientos || operationData.mantenimientos.length === 0;

  return (
    <Box>
      <VStack spacing={4} align="stretch">
        {noData ? (
          <Text color="gray.500" fontStyle="italic">
            No hay mantenimientos registrados en este momento.
          </Text>
        ) : (
          operationData.mantenimientos.map((m) => (
            <MantenimientoCard
              key={m.idMantenimiento}
              mantenimiento={m}
              onClick={() => console.log('enfocando...')}
            />
          ))
        )}
      </VStack>
    </Box>
  );
};

const IndicadoresSection = () => {
  const { operationData } = useOperacion();
  const [indicadoresVisibles, setIndicadoresVisibles] = useState<IndicadoresSimulado | null>(null);
  const lastSetTimeRef = useRef<number>(0);
  //

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

  useEffect(() => {
    const now = Date.now();
    const indicadores = operationData?.indicadores;

    if (indicadores && (now - lastSetTimeRef.current >= 5000 || lastSetTimeRef.current === 0)) {
      setIndicadoresVisibles(indicadores);
      lastSetTimeRef.current = now;
    } else if (!indicadores && !indicadoresVisibles) {
      // Solo setea los estáticos si aún no hay nada mostrado
      setIndicadoresVisibles(staticIndicadores);
    }
  }, [operationData?.indicadores]);

  return (
    <Box>
      <VStack spacing={4} align="stretch">
        {indicadoresVisibles && (
          <IndicadoresCard key={'indicadores-default'} indicadores={indicadoresVisibles} />
        )}
      </VStack>
    </Box>
  );
};

const BloqueosSection = () => {
  const { operationData, focusOnBloqueo } = useOperacion();

  const noData =
    !operationData?.bloqueos || operationData.bloqueos.length === 0;

  return (
    <Box>
      <VStack spacing={4} align="stretch">
        {noData ? (
          <Text color="gray.500" fontStyle="italic">
            No hay bloqueos registrados en este momento.
          </Text>
        ) : (
          operationData.bloqueos.map((bloqueo) => (
            <BloqueoCard
              key={bloqueo.idBloqueo}
              bloqueo={bloqueo}
              onClick={() => focusOnBloqueo(bloqueo)}
            />
          ))
        )}
      </VStack>
    </Box>
  );
};


export default function DailyOperation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(false)
  /*
     * HANDLE PANEL SECTIONS
     */
  

  const AveriaSection = () => {
    const { operationData } = useOperacion();
    const [searchValue, setSearchValue] = useState('');
    const [statusFilter, setStatusFilter] = useState<'todos' | 'resuelta' | 'encurso' | 'inminente'>('todos');
    const [typeFilter, setTypeFilter] = useState<'todos' | 'tipo1' | 'tipo2' | 'tipo3'>('todos');
    
    // Modal para registrar avería
    const { isOpen: isOpenAveria, onOpen: onOpenAveria, onClose: onCloseAveria } = useDisclosure();
    const [averiaData, setAveriaData] = useState<any>({
      turno: 'T1',
      tipo: 'Ti1',
      placa: '',
    });
    const toast = useToast();

      // Función para registrar avería
  const registerAveria = () => {
    // Validar que se haya ingresado una placa
    if (!averiaData.placa || averiaData.placa.trim() === '') {
      toast({ title: 'Error', description: 'Debe ingresar la placa del vehículo', status: 'error', duration: 3000 });
      return;
    }
    
    // Obtener el cliente STOMP del DailyOperationControlPanel
    const stompClient = (window as any).dailyOperationStompClient;
    
    if (!stompClient) {
      toast({ title: 'No conectado', status: 'error', duration: 2000 });
      return;
    }
    
    const message = {
      vehiclePlaque: averiaData.placa.trim(),
      type: averiaData.tipo,
      shiftOccurredOn: averiaData.turno,
    };
    
    console.log(`⚠️ Registrando avería: ${averiaData.tipo} para vehículo ${averiaData.placa} en turno ${averiaData.turno}`);
    
    stompClient.publish({ 
      destination: '/app/daily/update-failures', 
      body: JSON.stringify(message)
    });
    
    onCloseAveria();
    toast({ title: 'Avería registrada', status: 'success', duration: 2000 });
  };

    // Función para determinar el turno actual basado en la simulación
    const getTurnoActual = () => {
      if (!operationData?.minuto) {
        // Si no hay datos de simulación, usar la hora actual del sistema como fallback
        const now = new Date();
        const hora = now.getHours();
        if (hora >= 0 && hora < 8) return 'T1';
        if (hora >= 8 && hora < 16) return 'T2';
        return 'T3';
      }
      
      // Extraer la hora del minuto actual de la simulación
      // Formato esperado: "dd/MM/yyyy HH:mm"
      const [, hora] = operationData.minuto.split(" ");
      if (!hora) return 'T1'; // Fallback
      
      const [horaStr] = hora.split(":");
      const horaNum = Number(horaStr);
      
      if (horaNum >= 0 && horaNum < 8) return 'T1';
      if (horaNum >= 8 && horaNum < 16) return 'T2';
      return 'T3';
    };

    // Determinar turno automáticamente basado en la simulación actual
    useEffect(() => {
      const turnoActual = getTurnoActual();
      
      setAveriaData({
        ...averiaData,
        turno: turnoActual,
      });
    }, [operationData?.minuto]); // Se ejecuta cuando cambia el minuto de la simulación

    // Filtrado por estado y tipo
    const incidenciasFiltradas = (operationData?.incidencias || [])
      .filter((incidencia) => {
        // Filtro por estado
        if (statusFilter === 'todos') return true;
        if (statusFilter === 'resuelta') return incidencia.estado.toLowerCase() === 'resuelta';
        if (statusFilter === 'encurso') return incidencia.estado.toLowerCase() === 'en curso';
        if (statusFilter === 'inminente') return incidencia.estado.toLowerCase() === 'inminente';
        return true;
      })
      .filter((incidencia) => {
        // Filtro por tipo de incidente
        if (typeFilter === 'todos') return true;
        if (typeFilter === 'tipo1') return incidencia.tipo.toLowerCase() === 'ti1';
        if (typeFilter === 'tipo2') return incidencia.tipo.toLowerCase() === 'ti2';
        if (typeFilter === 'tipo3') return incidencia.tipo.toLowerCase() === 'ti3';
        return true;
      })
      .filter((incidencia) => {
        // Filtro por búsqueda (vehículo)
        if (!searchValue) return true;
        return incidencia.placa.toLowerCase().includes(searchValue.toLowerCase());
      });

    return (
      <Box>
        <VStack spacing={2} align="stretch" bg="#e6e6ea" p={2} borderRadius="md">
          <Input
            placeholder="Buscar avería por vehículo..."
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
              <MenuButton as={Button} leftIcon={<FontAwesomeIcon icon={faFilter} />} colorScheme="purple" variant="solid" fontSize="md" height="40px" borderRadius="md">
                Estado
              </MenuButton>
              <MenuList>
                <MenuItem onClick={() => setStatusFilter('todos')} fontWeight={statusFilter === 'todos' ? 'bold' : 'normal'} color={statusFilter === 'todos' ? 'purple.600' : undefined}>Todos</MenuItem>
                <MenuItem onClick={() => setStatusFilter('resuelta')} fontWeight={statusFilter === 'resuelta' ? 'bold' : 'normal'} color={statusFilter === 'resuelta' ? 'purple.600' : undefined}>Resuelta</MenuItem>
                <MenuItem onClick={() => setStatusFilter('encurso')} fontWeight={statusFilter === 'encurso' ? 'bold' : 'normal'} color={statusFilter === 'encurso' ? 'purple.600' : undefined}>En Curso</MenuItem>
                <MenuItem onClick={() => setStatusFilter('inminente')} fontWeight={statusFilter === 'inminente' ? 'bold' : 'normal'} color={statusFilter === 'inminente' ? 'purple.600' : undefined}>Inminente</MenuItem>
              </MenuList>
            </Menu>
            
            <Menu>
              <MenuButton as={Button} leftIcon={<FontAwesomeIcon icon={faFilter} />} colorScheme="purple" variant="solid" fontSize="md" height="40px" borderRadius="md">
                Tipo
              </MenuButton>
              <MenuList>
                <MenuItem onClick={() => setTypeFilter('todos')} fontWeight={typeFilter === 'todos' ? 'bold' : 'normal'} color={typeFilter === 'todos' ? 'purple.600' : undefined}>Todos</MenuItem>
                <MenuItem onClick={() => setTypeFilter('tipo1')} fontWeight={typeFilter === 'tipo1' ? 'bold' : 'normal'} color={typeFilter === 'tipo1' ? 'purple.600' : undefined}>Tipo 1</MenuItem>
                <MenuItem onClick={() => setTypeFilter('tipo2')} fontWeight={typeFilter === 'tipo2' ? 'bold' : 'normal'} color={typeFilter === 'tipo2' ? 'purple.600' : undefined}>Tipo 2</MenuItem>
                <MenuItem onClick={() => setTypeFilter('tipo3')} fontWeight={typeFilter === 'tipo3' ? 'bold' : 'normal'} color={typeFilter === 'tipo3' ? 'purple.600' : undefined}>Tipo 3</MenuItem>
              </MenuList>
            </Menu>

            <Button 
              leftIcon={<FaPlus />} 
              colorScheme="purple" 
              variant="solid" 
              fontSize="md" 
              height="40px" 
              borderRadius="md"
              onClick={onOpenAveria}
            >
              Registrar
            </Button>
          </HStack>
          <VStack spacing={4} align="stretch">
            {incidenciasFiltradas.length === 0 && (
              <Box color="gray.500" textAlign="center" py={6}>No hay averías para mostrar.</Box>
            )}
            {incidenciasFiltradas.map((i) => (
              <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={() => console.log('enfocando...')} />
            ))}
          </VStack>
        </VStack>

        {/* Modal para registrar avería */}
        <ModalInsertAveria
          isOpen={isOpenAveria}
          onClose={onCloseAveria}
          onSubmit={registerAveria}
          averiaData={averiaData}
          setAveriaData={setAveriaData}
        />
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
      title: 'Bloqueos',
      content: <BloqueosSection/>
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
