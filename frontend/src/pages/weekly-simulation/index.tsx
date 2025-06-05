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
import jsonData from "../../data/simulacionV2.json";
import { min, set } from 'date-fns'

export default function WeeklySimulation() {  
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  
  // delay
  const currPath = useLocation().pathname.split('/').pop()
  const [isLoading, setIsLoading] = useState(false);
  useEffect(() => {
    if (currPath === "simulacion") {
      setIsLoading(true);
      const timer = setTimeout(() => setIsLoading(false), 2000); // 2s simulado
      return () => clearTimeout(timer);
    }
  }, [currPath]);
  
  // Datos de simulación
  const [minuto, setMinuto] = useState(0);
  const [ data, setData ] = useState(jsonData);
  const minutoActual = data.simulacion.find((m) => m.minuto === minuto);
  const pedidos = minutoActual?.pedidos || [];
  const vehiculos = minutoActual?.vehiculos || [];
  const incidencias = minutoActual?.incidencias || [];
  const mantenimientos = minutoActual?.mantenimientos || [];  

  const [isPaused, setIsPaused] = useState(false);
  const [speedMs, setSpeedMs] = useState(38250); // valor inicial
    const [fechaVisual, setFechaVisual] = useState(new Date(data.fechaInicio));

  useEffect(() => {
    setData(jsonData);
  },[])

// ➕ Simulación automática
    useEffect(() => {
      const totalMinutos = data.simulacion.length;
      if (minuto >= totalMinutos || isPaused) return;
  
      // Avanza minuto real
      const interval = setTimeout(() => {
        setMinuto((prev) => prev + 1);
      }, speedMs);
  
      // Animar tiempo visual
      const fechaInicio = new Date(data.fechaInicio);
      const from = new Date(fechaInicio);
      from.setMinutes(from.getMinutes() + minuto * 75);
  
      const to = new Date(fechaInicio);
      to.setMinutes(to.getMinutes() + (minuto + 1) * 75);
  
      const animSteps = 30;
      let step = 0;
  
      const animInterval = setInterval(() => {
        step++;
        const interpolatedTime = new Date(from.getTime() + ((to.getTime() - from.getTime()) * (step / animSteps)));
        setFechaVisual(interpolatedTime);
        if (step >= animSteps) clearInterval(animInterval);
      }, speedMs / animSteps);
  
      return () => {
        clearTimeout(interval);
        clearInterval(animInterval);
      };
    }, [minuto, speedMs, isPaused]);
  

  useEffect(() => {
    console.log('Minuto index:', minuto);
  },[minuto])
  // Secciones de panel lateral
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
  const handleSectionChange = (section: string) => {
    setSection(section)
  }
  useEffect(() => {
    if(isCollapsed){
      setSection('')
    }
  }, [isCollapsed]);


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
                // setMinuto={setMinuto} 
                data={data}
                // isPaused={isPaused}
                setIsPaused={setIsPaused}
                // speedMs={speedMs}
                setSpeedMs={setSpeedMs}
                fechaVisual={fechaVisual}
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