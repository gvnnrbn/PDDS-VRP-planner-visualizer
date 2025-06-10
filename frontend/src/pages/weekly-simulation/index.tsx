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
import { OrderCard } from '../../components/common/cards/PedidoCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { MantenimientoCard } from '../../components/common/cards/mantenimientoCard'
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
      console.log('Sent date to backend:', selectedDate);
    }
  };

  // end simulation
  // useEffect(() => {
  //   if (!connected) return;
  //   var finished = false;

  //   const handleNoMoreChunks = (message: string) => {
  //     if(message == "COMPLETED"){
  //       finished = true;
  //       console.log("simulation completed");
  //     }
  //   }
  //   subscribe('/topic/simulation-data', handleNoMoreChunks);

  //   return () => {
  //     // Cleanup subscription if needed
  //   };
  // }, [connected, subscribe]);

   // 1. Handle incoming WebSocket messages
  useEffect(() => {
    if (!connected) return;

    const handleIncomingData = (message: any) => {
      try {
        console.log("receiving message");
        const data: any = JSON.parse(message.body);
        console.log("message: "+ data);
        setScheduleChunk(prev => {
          // First message goes to current
          if (!prev.current) {
            return { current: data };
          } 
          // Subsequent messages go to next
          else {
            return { ...prev, next: data };
          }
          
        });
      } catch (error) {
        console.error('Error parsing simulation data:', error);
      }
    };

    subscribe('/topic/simulation-data', handleIncomingData);

    return () => {
      // Cleanup subscription if needed
    };
  }, [connected, subscribe]);

  // 2. Request next chunk when needed
  useEffect(() => {
    if (connected && !scheduleChunk.next && !scheduleChunk.current) {
      publish('/app/request-chunk', JSON.stringify({}));
      console.log('Requested next chunk');
    }
  }, [connected, scheduleChunk, publish]);

  // 3. Handle chunk transition logic
  useEffect(() => {
    if (!scheduleChunk.current) return;

    const simulationArray = scheduleChunk.current.simulacion;
    const isLastElement = currentIndex >= simulationArray.length - 1;
    const hasNextChunk = scheduleChunk.next;

    if (isLastElement && hasNextChunk) {
      // Transition to next chunk
      setScheduleChunk({
        current: scheduleChunk.next,
        next: undefined
      });
      setCurrentIndex(0);
      console.log('Transitioned to next chunk');
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
    }, 1000); // Adjust timing as needed

    return () => clearInterval(timer);
  }, [scheduleChunk]);

  useEffect(()=> {
    console.log(scheduleChunk);

  },[scheduleChunk]);

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
              isSimulationLoading ? <></> : <SimulationPhase />
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
            <Button variant={'primary'} mr={3} onClick={handleSubmit}>
              Iniciar Simulación
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
      <LoadingOverlay isVisible={isSimulationLoading} />
    </Flex>
  );
}