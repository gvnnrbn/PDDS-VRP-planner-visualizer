import { Box,  useColorModeValue, VStack } from '@chakra-ui/react'
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

export default function CollapseSimulation() {
  return (
    <CollapseSimulationProvider>
      <CollapseSimulationInner />
    </CollapseSimulationProvider>
  );
}

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

  const PedidosSection = () => (
    <Box>
      <VStack spacing={4} align="stretch">
        {currentMinuteData?.pedidos?.map((pedido) => (
          <PedidoCard key={pedido.idPedido} pedido={pedido} onClick={() => console.log('enfocando...')} />
        ))}
      </VStack>
    </Box>
  );
  const FlotaSection = () => (
    <Box>
      <VStack spacing={4} align="stretch">
        {currentMinuteData?.vehiculos?.map((v) => (
          <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={()=>console.log('enfocando...')}/>
        ))}
      </VStack>
    </Box>
  );
  const AveriaSection = () => (
    <Box>
      <VStack spacing={4} align="stretch">
        {currentMinuteData?.incidencias?.map((i) => (
          <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={()=>console.log('enfocando...')}/>
        ))}
      </VStack>
    </Box>
  );
  const MantenimientoSection = () => (
    <Box>
      <VStack spacing={4} align="stretch">
        <PanelSearchBar onSubmit={() => console.log('searching...')} />
        {currentMinuteData?.mantenimientos?.map((m) => (
          <MantenimientoCard key={m.idMantenimiento} mantenimiento={m} onClick={()=>console.log('enfocando...')}/>
        ))}
      </VStack>
    </Box>
  );
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
