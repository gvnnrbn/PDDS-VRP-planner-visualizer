import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, Text, useColorModeValue, VStack, HStack, useDisclosure, useToast } from '@chakra-ui/react'
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
import useStomp from './useStomp'
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
  const { connected, subscribe, unsubscribe, publish } = useStomp('http://localhost:8080/ws');
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

useEffect(() => {
  if (connected && pendingStart) {
    handleStartSimulation();
    setPendingStart(false);
  }
}, [connected, pendingStart]);

const handleSubmit = () => {
  formatDateTime();
  setIsModalOpen(false);
  setIsSimulationLoading(true);
  setPendingStart(true); // ⏳ esperar conexión
};
 

  const handleStartSimulation = () => {
    if (connected) {
      publish('/app/simulation-start', selectedDate);
      setHasStarted(true);
      console.log('Sent date to backend:', selectedDate);
    }
  };
  const [minuteBuffer, setMinuteBuffer] = useState<SimulacionMinuto[]>([]);
  const [currentMinuteData, setCurrentMinuteData] = useState<SimulacionMinuto | null>(null);

  useEffect(() => {
    if (!connected) return;

    const handleIncomingData = (message: any) => {
      const data: SimulacionMinuto = JSON.parse(message.body);
      setMinuteBuffer(prev => [...prev, data]);
      console.log(data)
    };

    subscribe('/topic/simulation', handleIncomingData);
    return () => unsubscribe('/topic/simulation');
  }, [connected]);

  // Espera 3 segundos antes de consumir el buffer, luego consume cada 2 segundos
  useEffect(() => {
    if (!connected) return;
    let interval: NodeJS.Timeout;
    const timeout = setTimeout(() => {
      setIsSimulationLoading(false);
      interval = setInterval(() => {
        setMinuteBuffer(prev => {
          if (prev.length === 0) return prev;
          const [nextMinute, ...rest] = prev;
          setCurrentMinuteData(nextMinute);
          return rest;
        });
      }, 2000);
    }, 3000);

    return () => {
      clearTimeout(timeout);
      if (interval) clearInterval(interval);
    };
  }, [connected]);

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
          {currentMinuteData?.pedidos?.map((pedido: PedidoSimulado) => (
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
        {currentMinuteData?.vehiculos?.map((vehiculo :VehiculoSimulado) => (
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
      <AveriasPanel currentMinuteData={currentMinuteData} />
    )
  },
  {
    title: 'Mantenimiento',
    content: (
      <Box>
        <VStack spacing={4} align="stretch">
        <PanelSearchBar onSubmit={()=>console.log('searching...')}/>
        {currentMinuteData?.mantenimientos?.map((mantenimiento: MantenimientoSimulado) => (
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
    // {
    //   title: 'Indicadores',
    //   content: (
    //     <Box>
    //       <Text fontSize="sm" color="gray.600" _dark={{ color: "gray.400" }}>
    //         contenido indicadores
    //       </Text>
    //     </Box>
    //   )
    // },
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
          {/* <Route path="pedidos" element={<PedidosPhase />} />
          <Route path="incidencias" element={<IncidenciasPhase />} />
          <Route path="vehiculos" element={<VehiculosPhase />} />
          <Route path="almacen" element={<AlmacenPhase />} /> */}
          <Route
            path="simulacion"
            element={
              isSimulationLoading 
              ? <></> 
              : 
              <>
              {currentMinuteData && (
                <SimulationProvider initialData={currentMinuteData}>
                  <SimulationPhase
                    data={currentMinuteData}
                    speedMs={speedMs}
                    setSpeedMs={setSpeedMs}
                    setIsPaused={setIsPaused}
                  />
              </SimulationProvider>)}
                </>
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
            <Button variant={'primary'} mr={3} onClick={handleSubmit} disabled={!connected}>
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
        {currentMinuteData?.incidencias?.map((incidencia: any) => (
          <Box key={incidencia.idIncidencia}>
            <IncidenciaCard 
              incidencia={incidencia} 
              onClick={() => console.log('Enfocando vehiculo')}
            />
          </Box>
        ))}
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