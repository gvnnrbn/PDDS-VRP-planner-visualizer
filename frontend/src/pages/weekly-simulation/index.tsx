import { Box, Text, useColorModeValue, VStack } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import PedidosPhase from './PedidosPhase'
import IncidenciasPhase from './IncidenciasPhase'
import SimulationPhase from './SimulationPhase'
import VehiculosPhase from './VehiculosPhase'

import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import { FilterSortButtons } from '../../components/common/cards/FilterSortButtons'
import AlmacenPhase from './AlmacenPhase'
import jsonData from "../../data/simulacion.json";

export default function WeeklySimulation() {  
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  
  const currPath = useLocation().pathname.split('/').pop()
  const [isLoading, setIsLoading] = useState(false);
  
  const [minuto, setMinuto] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [speedMs, setSpeedMs] = useState(5000); // valor inicial
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);
  
  const data = jsonData;
  const fechaInicio = new Date(data.fechaInicio);
  const planificacion = data.simulacion;
  const totalMinutos = planificacion.length;
  
  // ➕ Cálculo de fecha actual (usado por BottomLeftControls)
  const fechaActual = new Date(fechaInicio);
  fechaActual.setDate(fechaInicio.getDate() + minuto);
  
  // ➕ Cálculo de fecha fin
  const fechaFin = new Date(fechaInicio);
  fechaFin.setDate(fechaInicio.getDate() + totalMinutos - 1);
  
  // const displayDate = `Día ${minuto + 1} | ${formatDateTime(fechaActual)} | 11:00`;
  
  
  useEffect(() => {
    if (currPath === "simulacion") {
      setIsLoading(true);
      const timer = setTimeout(() => setIsLoading(false), 100); // 10s simulado
      return () => clearTimeout(timer);
    }
  }, [currPath]);
  
  const handleSectionChange = (section: string) => {
    setSection(section)
  }
  useEffect(() => {
    if(isCollapsed){
      setSection('')
    }
  }, [isCollapsed]);
  
  // ➕ Simulación automática
  useEffect(() => {
    console.log(minuto);
    if (isPaused || minuto >= totalMinutos - 1) return;
    
    const interval = setTimeout(() => {
      setMinuto((prev) => prev + 1);
    }, speedMs);
    
    return () => clearTimeout(interval);
  }, [minuto, speedMs, isPaused]);
  
  const minutoActual = data.simulacion.find((m) => m.minuto === minuto);
  const pedidos = minutoActual?.pedidos || [];
  const vehiculos = minutoActual?.vehiculos || [];
  const incidencias = minutoActual?.incidencias || [];
  const mantenimientos = minutoActual?.mantenimientos || [];
  const sections = [
    {
      title: 'Pedidos',
      content: (
        <Box>
          <VStack spacing={4} align="stretch">
            <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
              {/* <FilterSortButtons entity={'Pedidos'}/> */}
            {pedidos.map((pedido) => (
              <Box key={pedido.idPedido}>
                <PedidoCard 
                  pedido={pedido} 
                  onClick={() => console.log('Enfocando pedido')}
                />
              </Box>
            ))}
          </VStack>
        </Box>
      )
    },
    {
      title: 'Flota',
      content: (
        <Box>
          <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
          {vehiculos.map((vehiculo) => (
            <Box key={vehiculo.idVehiculo}>
              <FlotaCard 
                vehiculo={vehiculo} 
                onClick={() => console.log('Enfocando vehiculo')}
              />
            </Box>
        ))}
  
          </VStack>
        </Box>
      )
    },
    {
      title: 'Averias',
      content: (
        <Box>
          <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
          {incidencias.map((incidencia) => (
            <Box key={incidencia.idIncidencia}>
              <IncidenciaCard 
                incidencia={incidencia} 
                onClick={() => console.log('Enfocando vehiculo')}
              />
            </Box>
        ))}
  
          </VStack>
        </Box>
      )
    },
    {
      title: 'Mantenimiento',
      content: (
        <Box>
          <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
          {mantenimientos.map((mantenimiento) => (
            <Box key={mantenimiento.idMantenimiento}>
              <MantenimientoCard 
                mantenimiento={mantenimiento} 
                onClick={() => console.log('Enfocando vehiculo')}
              />
            </Box>
        ))}
  
          </VStack>
        </Box>
      )
    },
    {
      title: 'Indicadores',
      content: (
        <Box>
          <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
            contenido indicadores
          </Text>
        </Box>
      )
    },
  ]
  const [section, setSection] = useState(sections[0].title)



  return (
    <Flex height="full" overflowY="auto" position="relative">
      <Box flex={1} p={4} bg={bgColor} h="full">
        <Routes>
          <Route path="pedidos" element={<PedidosPhase />} />
          <Route path="incidencias" element={<IncidenciasPhase />} />
          <Route path="vehiculos" element={<VehiculosPhase />} />
          <Route path="almacen" element={<AlmacenPhase />} />
          <Route
            path="simulacion"
            element={
              isLoading 
              ? <></> 
              : <SimulationPhase 
                minuto={minuto}
                setMinuto={setMinuto} 
                data={data} 
                isPaused={isPaused}
                setIsPaused={setIsPaused}
                speedMs={speedMs}
                setSpeedMs={setSpeedMs}
                />
            }
          />
        </Routes>
      </Box>

      {currPath === "simulacion" && !isLoading && (
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

      <LoadingOverlay isVisible={currPath === "simulacion" && isLoading} />
    </Flex>
  );
}