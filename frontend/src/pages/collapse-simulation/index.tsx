import { Box, Text, useColorModeValue, VStack } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useState } from 'react'
import { Flex } from '@chakra-ui/react'
import SimulationPhase from './SimulationPhase'
import LegendPanel from '../../components/common/Legend'
import BottomLeftControls from '../../components/common/MapActions'
import PedidosPhaseCollapse from './PedidosPhase'
import { PedidoCard } from '../../components/common/cards/PedidoCard'

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



const sections = [
  {
    title: 'Pedidos',
    content: (
      <Box>
        <VStack spacing={4} align="stretch">
        </VStack>
      </Box>
    )
  },
  {
    title: 'Flota',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Lcontenido flota
        </Text>
      </Box>
    )
  },
  {
    title: 'Mantenimiento',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          contenido mantenimiento
        </Text>
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

export default function CollapseSimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [section, setSection] = useState(sections[0].title)

  const currPath = useLocation().pathname.split('/').pop()

  const handleSectionChange = (section: string) => {
    setSection(section)
  }

  return (
    <Flex height="full" overflowY="auto">
      {/* <Box flex={1} p={4} bg={bgColor} h="full">
        <Routes>
          <Route path="pedidos" element={<PedidosPhaseCollapse />} />
          <Route path="simulacion" element={<SimulationPhase />} />
        </Routes>
      </Box>

      {currPath === "simulacion" && (
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
            //speed={simSpeed}
            //onStop={() => pauseSimulacion()}
            //onSpeedChange={(v) => setSimSpeed(v)}
          />
        </>
      )} */}
      <MyComponent></MyComponent>
    </Flex>
  )
}
