import { Box, Text, useColorModeValue, VStack } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useState } from 'react'
import { Flex } from '@chakra-ui/react'
import PedidosPhase from './PedidosPhase'
import IncidenciasPhase from './IncidenciasPhase'
import SimulationPhase from './SimulationPhase'
import VehiculosPhase from './VehiculosPhase'

import { OrderCard } from '../../components/common/OrderCard'
import type { IOrderCard } from '../../core/types/pedido'
const ordersOutput = [
  {
    orderId: 'PED-001',
    state: 'En Proceso',
    glp: 150,
    deadline: '2023-10-15',
    vehicles: [
      { plaque: 'ABC123', eta: '2023-10-14 12:00' },
      { plaque: 'XYZ789', eta: '2023-10-14 14:30' }
    ]
  },
  {
    orderId: 'PED-002',
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
          {ordersOutput.map((order) => (
            <Box key={order.orderId}>
              <OrderCard 
                orderCard={order} 
                onClick={() => console.log('Enfocar pedido clicked')}
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
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Lcontenido flota
        </Text>
      </Box>
    )
  },
  {
    title: 'Averias',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          contenido averias
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

export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [section, setSection] = useState(sections[0].title)

  const currPath = useLocation().pathname.split('/').pop()

  const handleSectionChange = (section: string) => {
    setSection(section)
  }

  return (
    <Flex height="full" overflowY="auto">
      <Box flex={1} p={4} bg={bgColor} h="full">
        <Routes>
          <Route path="pedidos" element={<PedidosPhase />} />
          <Route path="incidencias" element={<IncidenciasPhase />} />
          <Route path="simulacion" element={<SimulationPhase />} />
          <Route path="vehiculos" element={<VehiculosPhase />} />
        </Routes>
      </Box>

      {currPath == "simulacion" && (
        <SectionBar
          sections={sections}
          onSectionChange={handleSectionChange}
          currentSection={section}
          isCollapsed={isCollapsed}
          onToggleCollapse={() => setIsCollapsed(!isCollapsed)}
        />
      )}
    </Flex>
  )
}
