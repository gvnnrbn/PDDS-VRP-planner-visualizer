import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, useColorModeValue, VStack, HStack, useDisclosure, useToast, Menu, MenuList, MenuItem, MenuButton } from '@chakra-ui/react'
import { Route, Routes } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useContext, useEffect, useRef, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { FilterSortButtons } from '../../components/common/cards/FilterSortButtons'
import AlmacenPhase from './AlmacenPhase'
import type { Message } from '@stomp/stompjs'
import type { VehiculoSimulado } from '../../core/types/vehiculo'
import type { PedidoSimulado } from '../../core/types/pedido'
import type { IncidenciaSimulada } from '../../core/types/incidencia'
import type { MantenimientoSimulado } from '../../core/types/manetenimiento'
import { set } from 'date-fns'
import { FaSort, FaFilter, FaRegClock, FaPlus } from 'react-icons/fa'
import { IncidenciaForm } from '../../components/IncidenciaForm'
// import { MapGrid } from '../../components/common/Map' // Si tienes un CollapseMap, usa ese
// import { SimulationProvider, useSimulation } from '../../components/common/SimulationContextSemanal' // Si tienes un CollapseSimulationContext, usa ese
import SimulationPhase from './SimulationPhase'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import CollapseSimulationControlPanel from './CollapseSimulationControlPanel'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoService } from '../../core/services/PedidoService'
import { IncidenciaService } from '../../core/services/IncidenciaService'
import { ModalInsertAveria } from '../../components/common/modals/ModalInsertAveria'
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
    <Flex height="full" overflowY="auto" position="relative">
      <Box flex={1} p={4} bg={bgColor} h="full">
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
