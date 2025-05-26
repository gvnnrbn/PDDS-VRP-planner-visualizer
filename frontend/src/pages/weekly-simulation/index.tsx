import { Box, Button, defineStyle, defineStyleConfig, extendTheme, Text, useColorModeValue } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useState } from 'react'
import { Flex } from '@chakra-ui/react'
import PedidosPhase from './PedidosPhase'
import IncidenciasPhase from './IncidenciasPhase'
import SimulationPhase from './SimulationPhase'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowsToDot } from '@fortawesome/free-solid-svg-icons';

const sections = [
  {
    title: 'Pedidos',
    content: (
      <Flex bg='white' borderRadius='10px' py={3} px={4} mx={-1}>
        <Box flex='1'>
          <Flex gap={4} align='end'>
            <Text id='orderid' fontSize={20} color='purple'>P-0123</Text>
            <Text id='state'>Entregando</Text>
          </Flex>
          <Flex gap={1}>
            <Text id='glp'>GLP: {50}m³</Text>
            |
            <Text id='deadline'>Fecha Límite: {'D1 17:00'}</Text>
          </Flex>
          <Flex id='vehicles' direction='column'>
            <Flex id='vehicle' gap={1} color='grey' fontSize={14}>
              <Text id='plaque'>{'VH3-A2S'}</Text>
              |
              <Text id='eta'>ETA: {'13:43'}</Text>
            </Flex>
            <Flex id='vehicle' gap={1} color='grey' fontSize={14}>
              <Text id='plaque'>{'VH3-A2S'}</Text>
              |
              <Text id='eta'>ETA: {'13:43'}</Text>
            </Flex>

          </Flex>
        </Box>
        <Box>
          <Button gap={1} variant='primary'>
            Enfocar
            <FontAwesomeIcon icon={faArrowsToDot} />
          </Button>
        </Box>
      </Flex>
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
