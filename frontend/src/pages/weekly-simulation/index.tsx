import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, useColorModeValue, VStack, HStack, useDisclosure, useToast } from '@chakra-ui/react'
import { Route, Routes } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useState } from 'react'
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
import { FaSort, FaFilter, FaRegClock } from 'react-icons/fa'
import { IncidenciaForm } from '../../components/IncidenciaForm'
import { MapGrid } from '../../components/common/Map'
import { SimulationProvider } from '../../components/common/SimulationContextSemanal'
import SimulationPhase from './SimulationPhase'

interface SimulacionMinuto {
  minuto: string,
  bloqueos: any[],
  almacenes: any[],
  vehiculos: any[],
  pedidos: any[],
  incidencias: any[],
  mantenimientos: any[],
}

export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [ isSimulationCompleted, setIsSImulationCompleted ] = useState(false);

  const [selectedDate, setSelectedDate] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [hasStarted, setHasStarted] = useState(false);
  const [ isPaused, setIsPaused ] = useState(false)
  const [ speedMs, setSpeedMs ] = useState(1000);
  

  const [dateValue, setDateValue] = useState<string>(() => {
    const now = new Date();
    return now.toISOString().split('T')[0];
  });
  const [hourValue, setHourValue] = useState<number>(new Date().getHours());
  const [minuteValue, setMinuteValue] = useState<number>(new Date().getMinutes());

  /**
   * HANDLE DATE
   */
  const formatDateTime = () => {
    const [year, month, day] = dateValue.split('-');
    const formattedDate = `${year}-${month}-${day}T${String(hourValue).padStart(2, '0')}:${String(minuteValue).padStart(2, '0')}`;
    setSelectedDate(formattedDate);
  };

  useEffect(() => {
    formatDateTime();
  }, [dateValue, hourValue, minuteValue]);

  /**
   * START SIMULATION
  */

  const [pendingStart, setPendingStart] = useState(false);

  const handleSubmit = () => {
    formatDateTime();
    setIsModalOpen(false);
    setIsSimulationLoading(true);
    setPendingStart(true); // ⏳ esperar conexión
  };
 

  /*
   * HANDLE PANEL SECTIONS
   */

  const sections = [
  {
    title: 'Pedidos',
    content: (
      <Box>
        <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
            {/* <FilterSortButtons entity={'Pedidos'}/> */}
          {null}
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
        {null}
        </VStack>
      </Box>
    )
  },
  {
    title: 'Averias',
    content: (
      <AveriasPanel currentMinuteData={null} />
    )
  },
  {
    title: 'Mantenimiento',
    content: (
      <Box>
        <VStack spacing={4} align="stretch">
        <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
        {null}
        </VStack>
      </Box>
    )
  },
  ];

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
          <Route
            path="simulacion"
            element={
              isSimulationLoading 
                ? <></> 
                : (
                  <SimulationProvider initialData={{
                    minuto: "",
                    vehiculos: [],
                    pedidos: [],
                    almacenes: [],
                    incidencias: [],
                    mantenimientos: [],
                  }}>
                    <SimulationPhase
                      speedMs={speedMs}
                      setSpeedMs={setSpeedMs}
                      setIsPaused={setIsPaused}
                    />
                  </SimulationProvider>
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
      <Modal isOpen={isModalOpen} onClose={()=>{}}>
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Seleccione la fecha de inicio de la simulación</ModalHeader>
          <ModalBody pb={6}>
            <Stack spacing={4}>
              <FormControl>
                <FormLabel>Fecha</FormLabel>
                <Input
                  type="date"
                  value={dateValue}
                  onChange={(e) => setDateValue(e.target.value)}
                />
              </FormControl>

              <FormControl>
                <FormLabel>Hora</FormLabel>
                <NumberInput
                  min={0}
                  max={23}
                  value={hourValue}
                  onChange={(_, value) => setHourValue(value)}
                >
                  <NumberInputField />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </FormControl>

              <FormControl>
                <FormLabel>Minutos</FormLabel>
                <NumberInput
                  min={0}
                  max={59}
                  value={minuteValue}
                  onChange={(_, value) => setMinuteValue(value)}
                >
                  <NumberInputField />
                  <NumberInputStepper>
                    <NumberIncrementStepper />
                    <NumberDecrementStepper />
                  </NumberInputStepper>
                </NumberInput>
              </FormControl>
            </Stack>
          </ModalBody>

          <ModalFooter justifyContent={'center'}>
            <Button variant={'primary'} mr={3} onClick={handleSubmit} disabled={!true}>
              Iniciar Simulación
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
      <LoadingOverlay isVisible={isSimulationLoading} />
    </Flex>
  );
}

function AveriasPanel({ currentMinuteData }: { currentMinuteData: any }) {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const [showForm, setShowForm] = useState(false);
  return (
    <Box>
      <VStack spacing={4} align="stretch">
        <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
        <HStack spacing={2} mt={2}>
          <Button
            leftIcon={<FaSort />} colorScheme="purple" variant="outline" size="sm"
            borderRadius="md" fontWeight="bold" borderWidth="2px" borderColor="purple.500"
            color="purple.700" bg="white"
            _hover={{ bg: 'purple.100', color: 'purple.700' }}
            _active={{ bg: 'purple.500', color: 'white', borderColor: 'purple.700' }}
          >
            Ordenar
          </Button>
          <Button
            leftIcon={<FaFilter />} colorScheme="purple" variant="outline" size="sm"
            borderRadius="md" fontWeight="bold" borderWidth="2px" borderColor="purple.500"
            color="purple.700" bg="white"
            _hover={{ bg: 'purple.100', color: 'purple.700' }}
            _active={{ bg: 'purple.500', color: 'white', borderColor: 'purple.700' }}
          >
            Filtrar
          </Button>
          <Button
            leftIcon={<FaRegClock />}
            colorScheme="purple"
            variant="outline"
            size="sm"
            borderRadius="md"
            fontWeight="bold"
            borderWidth="2px"
            borderColor="purple.500"
            color="purple.700"
            bg="white"
            _hover={{ bg: 'purple.100', color: 'purple.700' }}
            _active={{ bg: 'purple.600', color: 'white', borderColor: 'purple.700' }}
            _expanded={{ bg: 'purple.600', color: 'white', borderColor: 'purple.700' }}
            onClick={onOpen}
          >
            Programar
          </Button>
        </HStack>
        {null}
      </VStack>
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
        <ModalOverlay />
        <ModalContent>
          <ModalBody>
            <IncidenciaForm onFinish={() => { onClose(); toast({ title: 'Incidencia programada', status: 'success', duration: 3000 }); }} onCancel={onClose} />
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  );
}