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
            {/* <FilterSortButtons entity={'Pedidos'}/> */}
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

interface ScheduleChunk {
  current?: any;
  next?: any;
}


export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [section, setSection] = useState(sections[0].title)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [ isSimulationCompleted, setIsSImulationCompleted ] = useState(false);

  const [selectedDate, setSelectedDate] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(true);
  const { connected, subscribe, unsubscribe, publish } = useStomp('http://localhost:8080/ws');
  const [hasStarted, setHasStarted] = useState(false);
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
  // Format the selected date/time into ISO format
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

  // delay after simulation starts
  // useEffect(() => {
  //   const timer = setTimeout(() => setIsSimulationLoading(false), 2000); // 2s simulado
  //   return () => clearTimeout(timer);
  // },[isSimulationLoading])
 
  const handleSubmit = () => {
    formatDateTime(); // Ensure we have the latest value
    setIsModalOpen(false);
    handleStartSimulation();
    setIsSimulationLoading(true);
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
    console.log("receiving message:", message.body);

    try {
      // Verifica si parece un JSON válido
      if (message.body.trim().startsWith('{') || message.body.trim().startsWith('[')) {
        const data = JSON.parse(message.body);
        console.log("Parsed data:", data);

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
        // Aquí puedes manejar mensajes tipo "COMPLETED"
        if (message.body === "COMPLETED") {
          setIsSImulationCompleted(true);
          console.log("Simulation completed");
        }
      }
    } catch (error) {
      console.error('Error parsing simulation data:', error);
    }
    }; 
    subscribe('/topic/simulation-data', handleIncomingData);
    return () => {
      unsubscribe('/topic/simulation-data');
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
      if (currentIndex >= simulationLength - 2) {
        console.log('Solicitando siguiente chunk...');
        publish('/app/request-chunk', JSON.stringify({}));
      }
    }
  }, [connected, selectedDate, scheduleChunk, currentIndex, hasStarted]);

  // 3. Handle chunk transition logic
  useEffect(() => {
    if (!scheduleChunk.current) return;

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
    if (!scheduleChunk.current) return;

    const timer = setInterval(() => {
      setCurrentIndex(prev => {
        // Prevent going beyond array bounds
        if (prev >= scheduleChunk.current!.simulacion.length - 1) {
          return prev;
        }
        return prev + 1;
      });
    }, 2000); // Adjust timing as needed

    return () => clearInterval(timer);
  }, [scheduleChunk]);


  // Render current simulation state
  const currentMinute = scheduleChunk.current?.simulacion[currentIndex]?.minuto;
  const currentData = scheduleChunk.current?.simulacion[currentIndex];
  // console.log(currentData);
  /*
   * HANDLE PANEL SECTIONS
   */
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
            <Text fontSize="sm" color="gray.500" mt={2}>
              Minuto actual: {currentMinute}
            </Text>
      {/*!isSimulationLoading && (
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
      )*/}
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
            <Button variant={'primary'} mr={3} onClick={handleSubmit}>
              Iniciar Simulación
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
      {/* <LoadingOverlay isVisible={isSimulationLoading} /> */}
    </Flex>
  );
}