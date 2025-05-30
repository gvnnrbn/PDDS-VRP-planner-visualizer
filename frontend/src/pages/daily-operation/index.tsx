import { Box, Text, VStack, Flex, useColorModeValue } from '@chakra-ui/react'
import { SectionBar } from '../../components/common/SectionBar'
import { useState } from 'react'

const sections = [
  {
    title: 'Pedidos',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Pedidos
        </Text>
      </Box>
    )
  },
  {
    title: 'Flota',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Flota
        </Text>
      </Box>
    )
  },
  {
    title: 'Averias',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Averías
        </Text>
      </Box>
    )
  },
  {
    title: 'Mantenimiento',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Mantenimiento
        </Text>
      </Box>
    )
  },
  {
    title: 'Indicadores',
    content: (
      <Box>
        <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
          Contenido de la sección Indicadores
        </Text>
      </Box>
    )
  }
]

export default function DailyOperation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [currentSection, setCurrentSection] = useState('Pedidos')
  const [isCollapsed, setIsCollapsed] = useState(false)

  return (
    <Flex height="full" overflowY="auto">
      <Box flex={1} p={4} bg={bgColor} h="full">
        <VStack spacing={4} align="stretch">
          <Text fontSize="2xl" fontWeight="bold">
            Operación día a día
          </Text>
        </VStack>
      </Box>

      <SectionBar
        sections={sections}
        onSectionChange={setCurrentSection}
        currentSection={currentSection}
        isCollapsed={isCollapsed}
        onToggleCollapse={() => setIsCollapsed(!isCollapsed)}
      />
    </Flex>
  )
}
