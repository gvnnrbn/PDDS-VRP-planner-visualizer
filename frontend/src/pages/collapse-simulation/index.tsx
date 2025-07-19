import { Box,  HStack,  Menu,  MenuButton,  MenuItem,  MenuList,  useColorModeValue, VStack } from '@chakra-ui/react'
import { Route, Routes } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
// import { MapGrid } from '../../components/common/Map' // Si tienes un CollapseMap, usa ese
// import { SimulationProvider, useSimulation } from '../../components/common/SimulationContextSemanal' // Si tienes un CollapseSimulationContext, usa ese
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import CollapseSimulationControlPanel from './CollapseSimulationControlPanel'
import { CollapseSimulationProvider, useCollapseSimulation } from './CollapseSimulationContext'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFilter, faSort } from '@fortawesome/free-solid-svg-icons'

export default function CollapseSimulation() {
  return (
    <CollapseSimulationProvider>
      <CollapseSimulationInner />
    </CollapseSimulationProvider>
  );
}

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
  const { currentMinuteData, focusOnPedido } = useCollapseSimulation();
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<'todos' | 'pendiente' | 'completado'>('todos');
  // orderBy null significa "pendiente primero, luego completado, ambos por fechaLimite asc"
  const [orderBy, setOrderBy] = useState<string | null>(null);

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
    if (!orderBy) {
      // Pendientes primero, luego completados, ambos por fechaLimite ascendente
      const estadoA = a.estado.toLowerCase() === 'completado' ? 1 : 0;
      const estadoB = b.estado.toLowerCase() === 'completado' ? 1 : 0;
      if (estadoA !== estadoB) return estadoA - estadoB;
      return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
    }
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
          placeholder="Buscar pedido por código..."
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
              <MenuItem
                key="default"
                onClick={() => setOrderBy(null)}
                color={orderBy === null ? 'purple.600' : undefined}
                fontWeight={orderBy === null ? 'bold' : 'normal'}
              >
                Pendiente primero
              </MenuItem>
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
const FlotaSection = () => (
  <Box>
    <VStack spacing={4} align="stretch">
      {currentMinuteData?.vehiculos?.map((v) => (
        <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={() => console.log('enfocando...')} />
      ))}
    </VStack>
  </Box>
);
const AveriaSection = () => (
  <Box>
    <VStack spacing={4} align="stretch">
      {currentMinuteData?.incidencias?.map((i) => (
        <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={() => console.log('enfocando...')} />
      ))}
    </VStack>
  </Box>
);
const MantenimientoSection = () => (
  <Box>
    <VStack spacing={4} align="stretch">
      <PanelSearchBar onSubmit={() => console.log('searching...')} />
      {currentMinuteData?.mantenimientos?.map((m) => (
        <MantenimientoCard key={m.idMantenimiento} mantenimiento={m} onClick={() => console.log('enfocando...')} />
      ))}
    </VStack>
  </Box>
);

function CollapseSimulationInner() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [dateValue, setDateValue] = useState<string>(() => {
    const now = new Date();
    return now.toISOString().split('T')[0];
  });
  const [hourValue, setHourValue] = useState<number>(new Date().getHours());
  const [minuteValue, setMinuteValue] = useState<number>(new Date().getMinutes());
  const [section, setSection] = useState('Pedidos');
  const { currentMinuteData, setCurrentMinuteData } = useCollapseSimulation();

  const handleSectionChange = (section: string) => {
    setSection(section)
  }
  useEffect(() => {
    if(isCollapsed){
      setSection('')
    }
  }, [isCollapsed]);

  
  const sections = [
    { title: 'Pedidos', content: <PedidosSection/> },
    { title: 'Flota', content: <FlotaSection/> },
    { title: 'Averias', content: <AveriaSection/> },
    { title: 'Mantenimiento', content: <MantenimientoSection/> },
  ];

  return (
    <Flex height="100%" width="100%" overflowY="hidden" position="relative">
      <Box flex={1} p={0} bg={bgColor} h="full">
        <Routes>
          <Route
            path="simulacion"
            element={
              isSimulationLoading 
                ? <></> 
                : (
                    <CollapseSimulationControlPanel setData={setCurrentMinuteData} data={currentMinuteData}/>
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
      <LoadingOverlay isVisible={isSimulationLoading} />
    </Flex>
  );
}
