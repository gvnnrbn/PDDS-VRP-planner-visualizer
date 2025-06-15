import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, Text, useColorModeValue, VStack } from '@chakra-ui/react'
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

interface ScheduleChunk {
  current?: any;
  next?: any;
}


export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [ isSimulationCompleted, setIsSimulationCompleted ] = useState(false);

  const [selectedDate, setSelectedDate] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(true);
  const { connected, subscribe, unsubscribe, publish } = useStomp('http://localhost:8080/ws');
  const [hasStarted, setHasStarted] = useState(false);
  const [ isPaused, setIsPaused ] = useState(false)
  const [ speedMs, setSpeedMs ] = useState(1000)  
  const [scheduleChunk, setScheduleChunk] = useState<ScheduleChunk>({});
  const [currentIndex, setCurrentIndex] = useState(0);

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

 
  const handleSubmit = () => {
    if(!connected) return;
    formatDateTime(); // Ensure we have the latest value
    setIsModalOpen(false);
    setIsSimulationLoading(true);
    const timer = setTimeout(() => {
      setIsSimulationLoading(false);
      setHasStarted(true);
      handleStartSimulation();
    }, 3000);
    return () => clearTimeout(timer);
    // if (connected) {
      // handleStartSimulation();
    // } else {
    //   const waitUntilConnected = setInterval(() => {
    //     if (connected) {
    //       clearInterval(waitUntilConnected);
    //       handleStartSimulation();
    //     }
    //   }, 250);
    // }
  };

  const handleStartSimulation = () => {
    if (connected) {
      publish('/app/simulation-start', selectedDate);
      setHasStarted(true);
      console.log('Sent date to backend:', selectedDate);
    }
  };

   // 1. Handle incoming WebSocket messages
  useEffect(() => {
    if (!connected) return;

    const handleIncomingData = (message: any) => {

    try {
      // Verifica si parece un JSON válido
      if (message.body.trim().startsWith('{') || message.body.trim().startsWith('[')) {
        const data = JSON.parse(message.body);
        console.log(data);

        setScheduleChunk(prev => {
          if (!prev.current) {
            return { current: data };
          } else {
            return { ...prev, next: data };
          }
        });
        publish('/app/chunk-received', {});
      } else {
        console.warn("Received non-JSON message:", message.body);
        if (message.body === "COMPLETED") {
          setIsSimulationCompleted(true);
          console.log("Simulation completed");
        }
      }
    } catch (error) {
      console.error('Error parsing simulation data:', error);
    }
    }; 
    subscribe('/topic/simulation-data', handleIncomingData);
    return () => {
      unsubscribe("/topic/simulation-data");
    };
  }, [connected]);

  // 2. Request next chunk when needed
  useEffect(() => {
    if (!connected || !selectedDate || !hasStarted) return;

    // Caso 1: Primera vez que se solicita
    if (!scheduleChunk.current) {
      const timer = setTimeout(() => {
        console.log("Solicitando primer chunk...");
        publish('/app/request-chunk', JSON.stringify({}));
      }, 500); // Delay para el backend

      return () => clearTimeout(timer);
    }

    // Caso 2: Ya hay un chunk actual, pero nos acercamos al final
    if (!scheduleChunk.next) {
      const simulationLength = scheduleChunk.current.simulacion.length;
      if (currentIndex >= simulationLength - 1) {
        console.log('Solicitando siguiente chunk...');
        publish('/app/request-chunk', JSON.stringify({}));
      }
    }
  }, [connected, selectedDate, scheduleChunk, currentIndex, hasStarted]);

  // 3. Handle chunk transition logic
  useEffect(() => {
    if (!connected || !scheduleChunk.current) return;

    const simulationArray = scheduleChunk.current.simulacion;
    const isLastElement = currentIndex >= simulationArray.length - 1;
    const hasNextChunk = scheduleChunk.next;

    if (isLastElement && hasNextChunk) {
      // Espera un segundo extra para mostrar el último minuto
      setTimeout(() => {
        setScheduleChunk({
          current: scheduleChunk.next,
          next: undefined
        });
        setCurrentIndex(0);
        console.log('Transitioned to next chunk');
      }, 1000)
    }
  }, [currentIndex, scheduleChunk]);

  // 4. Visualization timer (example)
  useEffect(() => {
    if (!connected || !scheduleChunk.current) return;

    const timer = setInterval(() => {
      setCurrentIndex(prev => {
        const isLast = prev >= scheduleChunk.current!.simulacion.length - 1;
        if (isLast && !scheduleChunk.next) return prev; // Detener si no hay más datos
        return prev + 1;
      });

    }, 2000); // Adjust timing as needed

    return () => clearInterval(timer);
  }, [scheduleChunk]);
  // 5. reset states on reload
  useEffect(() => {
  if (connected) {
    // Reset all state on new connection if desired
    setHasStarted(false);
    setIsSimulationCompleted(false);
    setCurrentIndex(0);
    setScheduleChunk({});
  }
}, [connected]);



  // Render current simulation state
  const currentMinute = scheduleChunk.current?.simulacion[currentIndex]?.minuto;
  const currentData = scheduleChunk.current?.simulacion[currentIndex];
  // console.log(currentData);
  
  
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
          {currentData?.pedidos?.map((pedido: PedidoSimulado) => (
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
        {currentData?.vehiculos?.map((vehiculo :VehiculoSimulado) => (
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
        {currentData?.incidencias?.map((incidencia: IncidenciaSimulada) => (
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
        {currentData?.mantenimientos?.map((mantenimiento: MantenimientoSimulado) => (
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
              {/* <SimulationPhase 
                minuto={currentMinute}
                // setMinuto={setMinuto} 
                data={currentData}
                // isPaused={isPaused}
                setIsPaused={setIsPaused}
                // speedMs={speedMs}
                setSpeedMs={setSpeedMs}
                fechaVisual={currentMinute}
                /> */}
                </>
            }
          />
        </Routes>
      </Box>
            <Text fontSize="sm" color="gray.500" mt={2} mr={20}>
              Minuto actual: {currentMinute}
            </Text>
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