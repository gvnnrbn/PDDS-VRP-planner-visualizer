import { Box, Button, Input, Text, useColorModeValue, VStack } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import PedidosPhase from './PedidosPhase'
import IncidenciasPhase from './IncidenciasPhase'
import SimulationPhase from './SimulationPhase'
import VehiculosPhase from './VehiculosPhase'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons'

import LegendPanel from '../../components/common/Legend'
import BottomLeftControls from '../../components/common/MapActions'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { OrderCard } from '../../components/common/cards/PedidoCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { MantenimientoCard } from '../../components/common/cards/mantenimientoCard'

// mock data
const ordersOutput = [
  {
    id: 'PED-001',
    state: 'En Proceso',
    glp: 150,
    deadline: '2023-10-15',
    vehicles: [
      { plaque: 'ABC123', eta: '2023-10-14 12:00' },
      { plaque: 'XYZ789', eta: '2023-10-14 14:30' }
    ]
  },
  {
    id: 'PED-002',
    state: 'Completado',
    glp: 200,
    deadline: '2023-10-16',
    vehicles: [
      { plaque: 'LMN456', eta: '2023-10-15 10:00' },
    ]
  },
]

const incidencias = [
  {
    id: 1,
    estado: 'En Curso',
    placa: 'ABC123',
    turno: "T1",
    tipo: "TI1",
    fechaInicio: '2023-10-14 18:00',
    fechaFin: '2023-10-14 22:00',
  },
  {
    id: 2,
    estado: 'Estimada',
    placa: 'IJK123',
    turno: "T1",
    tipo: "TI2",
    fechaInicio: '2023-10-14 18:00',
    fechaFin: '2023-10-14 22:00',
  },
  {
    id: 3,
    estado: 'Resuelta',
    placa: 'IJK123',
    turno: "T1",
    tipo: "TI2",
    fechaInicio: '2023-10-14 18:00',
    fechaFin: '2023-10-14 22:00',
  },
]

const flota = [
  {
    id:1,
    placa: 'ABC123',
    estado: "Averiado",
    eta: '2023-10-14 12:00',
    glp: 50,
    combustible: 180,
    maxCombustible: 200,
    pedidoId: 'PED-001',
  },
  {
    id:2,
    placa: 'XYZ098',
    estado: "Entregando",
    eta: '2023-10-14 12:00',
    glp: 50,
    combustible: 100,
    maxCombustible: 200,
    pedidoId: 'PED-002',
  },
  {
    id:3,
    placa: 'XSZ098',
    estado: "Sin Programación",
    eta: '2023-10-14 12:00',
    glp: 50,
    combustible: 100,
    maxCombustible: 200,
    pedidoId: 'PED-002',
  },
  {
    id:4,
    placa: 'XSZ098',
    estado: "En Mantenimiento",
    eta: '2023-10-14 12:00',
    glp: 50,
    combustible: 100,
    maxCombustible: 200,
    pedidoId: 'PED-002',
  },
]

const mantenimientos = [
  {
    id: 1,
    vehiculo: {
      placa: 'ABC123',
      tipo: 'TA',
    },
    estado: 'En Curso',
    fechaInicio: '2023-10-14 00:00',
    fechaFin: '2023-10-14 11:59',
  },
  {
    id: 2,
    vehiculo: {
      placa: 'ABC123',
      tipo: 'TA',
    },
    estado: 'Programado',
    fechaInicio: '2023-10-14 00:00',
    fechaFin: '2023-10-14 11:59',
  },
  {
    id: 3,
    vehiculo: {
      placa: 'ABC123',
      tipo: 'TA',
    },
    estado: 'Terminado',
    fechaInicio: '2023-10-14 00:00',
    fechaFin: '2023-10-14 11:59',
  },
]

const sections = [
  {
    title: 'Pedidos',
    content: (
      <Box>
        <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
          {ordersOutput.map((order) => (
            <Box key={order.id}>
              <OrderCard 
                orderCard={order} 
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
        {flota.map((vehiculo) => (
          <Box key={vehiculo.id}>
            <FlotaCard 
              flotaCard={vehiculo} 
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
          <Box key={incidencia.id}>
            <IncidenciaCard 
              incidenciaCard={incidencia} 
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
          <Box key={mantenimiento.id}>
            <MantenimientoCard 
              mantenimientoCard={mantenimiento} 
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

export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [section, setSection] = useState(sections[0].title)

  const currPath = useLocation().pathname.split('/').pop()

  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (currPath === "simulacion") {
      setIsLoading(true);
      const timer = setTimeout(() => setIsLoading(false), 1); // 10s simulado
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
  return (
    <Flex height="full" overflowY="auto" position="relative">
      <Box flex={1} p={4} bg={bgColor} h="full">
        <Routes>
          <Route path="pedidos" element={<PedidosPhase />} />
          <Route path="incidencias" element={<IncidenciasPhase />} />
          <Route path="vehiculos" element={<VehiculosPhase />} />
          <Route
            path="simulacion"
            element={
              isLoading ? <></> : <SimulationPhase />
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

          <BottomLeftControls 
            variant="full"
            date="Día 1 | 03/04/2025 | 11:00"
          />
        </>
      )}

      <LoadingOverlay isVisible={currPath === "simulacion" && isLoading} />
    </Flex>
  );
}