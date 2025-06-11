import { Box, Button, Modal, ModalBody, ModalContent, ModalHeader, ModalOverlay, useColorModeValue, VStack, Text, useDisclosure } from '@chakra-ui/react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useState } from 'react'
import { Flex } from '@chakra-ui/react'

import SimulationPhase from './SimulationPhase'


import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FilterSortButtons } from '../../components/common/cards/FilterSortButtons'
import useStomp from './useStomp'
import type { Message } from '@stomp/stompjs'

//Data de prueba
import jsonData from "../../data/simulacionV2.json";
import { SimulacionProvider } from '../../components/common/SimulacionContext'
import { formatDateTime } from '../../utils/dateFormatter';
import BottomLeftControls from '../../components/common/MapActions'

export default function WeeklySimulation() {
  const { connected, subscribe, unsubscribe, publish } = useStomp('http://localhost:8080/ws');
  const [log, setLog] = useState<string>();
  
  // Add a button to trigger the simulation
  const handleStartSimulation = () => {
    if (connected) {
      // Send a static date (you can modify this to use a dynamic date)
      const now = new Date();
      const formattedDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}T${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      
      publish('/app/simulacion-start', formattedDate);
      console.log('Sent date to backend:', formattedDate);
    }
  };

  //WEBSOCKET
  useEffect(() => {
    if (!connected) return;
    const suscribeUrl = '/topic/simulacion-start';
    const handleSimulacion = (message: Message) => {
      try {
        const payload = JSON.parse(message.body);
        // console.log('Received simulation data:', payload);
        setLog(payload);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    };

    subscribe(suscribeUrl, handleSimulacion);
    return () => {
      unsubscribe(suscribeUrl);
    };
  }, [connected, subscribe, unsubscribe]);

  useEffect(() => {
    if (connected) {
      console.log(log);
    }
  },[log])
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  
  const currPath = useLocation().pathname.split('/').pop()

  //PANTALLA DE CARGA
  useEffect(() => {
    if (currPath === "simulacion") {
      setIsLoading(true);
      const timer = setTimeout(() => setIsLoading(false), 10000); // 10s simulado
      return () => clearTimeout(timer);
    }
  }, [currPath]);

  

  //AVANCE DE TIEMPO
  const [data, setData] = useState(jsonData);
  const [minutoIndex, setMinutoIndex] = useState(0);
  const [speedMs, setSpeedMs] = useState(31250); // default speed
  const [isPaused, setIsPaused] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  // Simulation clock
  useEffect(() => {
    if (!data || isPaused) return;
    const interval = setInterval(() => {
      setMinutoIndex(prev => {
        if (prev < data.simulacion.length - 1) return prev + 1;
        clearInterval(interval);
        return prev;
      });
    }, speedMs);
    return () => clearInterval(interval);
  }, [data, speedMs, isPaused]);

  //INFORMACION DE SECTION BAR
  const currentData = data.simulacion[minutoIndex];
  const currentTime = new Date(currentData?.minuto);


  const sections = [
    {
      title: 'Pedidos',
      content: (
        <Box>
          <VStack spacing={4} align="stretch">
            <PanelSearchBar onSubmit={() => console.log('searching pedidos...')} />
            {(currentData?.pedidos ?? []).map((pedido: any) => (
              <Box key={pedido.idPedido}>
                <PedidoCard
                  pedido={{
                    idPedido: pedido.idPedido,
                    estado: pedido.estado,
                    glp: pedido.cantidad,
                    fechaLimite: pedido.fechaLimite,
                    vehiculosAtendiendo: pedido.vehiculos ?? [],
                    posX: pedido.posX,
                    posY: pedido.posY,
                  }}
                  onClick={() => console.log('Enfocando pedido', pedido.idPedido)}
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
            <PanelSearchBar onSubmit={() => console.log('searching flota...')} />
            {(currentData?.vehiculos ?? []).map((vehiculo: any) => (
              <Box key={vehiculo.idVehiculo}>
                <FlotaCard
                  vehiculo={vehiculo}
                  onClick={() => console.log('Enfocando vehiculo', vehiculo.idVehiculo)}
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
            <PanelSearchBar onSubmit={() => console.log('searching averias...')} />
            {(currentData?.incidencias ?? []).map((incidencia: any) => (
              <Box key={incidencia.idIncidencia}>
                <IncidenciaCard
                  incidencia={incidencia}
                  onClick={() => console.log('Enfocando incidencia', incidencia.id)}
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
            <PanelSearchBar onSubmit={() => console.log('searching mantenimiento...')} />
            {(currentData?.mantenimientos ?? []).map((mantenimiento: any) => (
              <Box key={mantenimiento.id}>
                <MantenimientoCard
                  mantenimiento={mantenimiento}
                  onClick={() => console.log('Enfocando mantenimiento', mantenimiento.id)}
                />
              </Box>
            ))}
          </VStack>
        </Box>
      )
    }
  ];

  //CAMBIO DE SECTIONS
  const [section, setSection] = useState(sections[0].title)
  const handleSectionChange = (section: string) => {
    setSection(section)
  }
  useEffect(() => {
    if(isCollapsed){
      setSection('')
    }
  }, [isCollapsed]);

  //MODAL FINAL
  const [simulacionFinalizada, setSimulacionFinalizada] = useState(false);
  const totalMinutos = jsonData.simulacion.length;
  const fechaInicio = new Date(jsonData.simulacion[0].minuto);
  const fechaFin = new Date(jsonData.simulacion[totalMinutos - 1].minuto);
  const { isOpen, onOpen, onClose } = useDisclosure();

  useEffect(() => {
    console.log(`aahhhh causa Simulation Phase MINUTO:${minutoIndex} con CURRENTTIME:${currentTime}  \n\nINICIO:${fechaInicio}  
      \n FIN: ${fechaFin} y totalMinutos: ${totalMinutos}`)
    if (minutoIndex >= totalMinutos - 1 && !isOpen && !simulacionFinalizada) {
      setSimulacionFinalizada(true);
      onOpen();
    }
  }, [ totalMinutos, isOpen, simulacionFinalizada]);

  const displayDate = `${currentTime.toLocaleDateString()} | ${currentTime.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    })}`;
  
  const handleSpeedChange = (newSpeed: string) => {
    if (newSpeed === "Velocidad x1") {
      setSpeedMs(31250);
    } else if (newSpeed === "Velocidad x2") {
      setSpeedMs(15625);
    }
  };

  const handleStop = () => {
    setIsPaused(true);
    setSimulacionFinalizada(true);
    onOpen();
  };

  const bloqueos = jsonData.bloqueos;

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
              isLoading 
              ? <></> 
              : 
                <SimulacionProvider
                  initialVehiculos={jsonData.simulacion[0]?.vehiculos ?? []}
                  initialPedidos={jsonData.simulacion[0]?.pedidos ?? []}
                  initialAlmacenes={jsonData.simulacion[0]?.almacenes ?? []}
                >
                  <SimulationPhase
                    data={currentData}
                    currentTime={currentTime}
                    bloqueos={bloqueos}
                    setSpeedMs={setSpeedMs}
                    setIsPaused={setIsPaused}
                  />
                  <BottomLeftControls variant="full" date={displayDate} onSpeedChange={handleSpeedChange} onStop={handleStop}/>
                  {/* ✅ Modal al finalizar */}
                  <Modal isOpen={isOpen} onClose={onClose} isCentered size="xl">
                    <ModalOverlay bg="blackAlpha.700" />
                    <ModalContent bg="white" textAlign="center" p={8}>
                      <ModalHeader>
                        <Text fontSize="2xl" fontWeight="extrabold">
                          ✅ Simulación completada
                        </Text>
                      </ModalHeader>
                      <hr />
                      <ModalBody>
                        <VStack align="start" spacing={3} fontSize="md">
                          <Text>
                            <strong>Fecha y Hora de inicio:</strong> {formatDateTime(fechaInicio)}
                          </Text>
                          <Text>
                            <strong>Fecha y Hora de fin:</strong> {formatDateTime(fechaFin)}
                          </Text>
                          <Text>
                            <strong>Duración:</strong> {totalMinutos} días
                          </Text>
                          <Text>
                            <strong>Pedidos entregados:</strong> 504 {/* reemplazar dinámico si deseas */}
                          </Text>
                          <Text>
                            <strong>Consumo en petróleo:</strong> 456 {/* reemplazar dinámico si deseas */}
                          </Text>
                          <Text>
                            <strong>Tiempo de planificación:</strong> 00:25:35 {/* opcional */}
                          </Text>
                        </VStack>
                        <Flex justify="flex-end" mt={6}>
                          <Button colorScheme="purple" onClick={onClose}>
                            Aceptar
                          </Button>
                        </Flex>
                      </ModalBody>
                    </ModalContent>
                  </Modal>
                </SimulacionProvider>
            }
          />
        </Routes>
      </Box>

      {currPath === "simulacion" && !isLoading && (
        <>
          <Button variant={'primary'} onClick={handleStartSimulation} disabled={!connected}>
          {connected ? 'Start Simulation' : 'Connecting...'}
            </Button>
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

      <LoadingOverlay isVisible={currPath === "simulacion" && isLoading} />
    </Flex>
  );
}