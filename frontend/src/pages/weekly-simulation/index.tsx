import { Box, Button, FormControl, FormLabel, Input, Modal, ModalBody, ModalContent, ModalFooter, ModalHeader, ModalOverlay, NumberDecrementStepper, NumberIncrementStepper, NumberInput, NumberInputField, NumberInputStepper, Stack, useColorModeValue, VStack, HStack, useDisclosure, useToast, Menu, MenuList, MenuItem, MenuButton } from '@chakra-ui/react'
import { Route, Routes } from 'react-router-dom'
import { SectionBar } from '../../components/common/SectionBar'
import { useEffect, useRef, useState } from 'react'
import { Flex } from '@chakra-ui/react'
import LegendPanel from '../../components/common/Legend'
import LoadingOverlay from '../../components/common/LoadingOverlay'
import { PanelSearchBar } from '../../components/common/PanelSearchBar'
import { PedidoCard } from '../../components/common/cards/PedidoCard'
import { FlotaCard } from '../../components/common/cards/FlotaCard'
import { IncidenciaCard } from '../../components/common/cards/IncidenciaCard'
import { MantenimientoCard } from '../../components/common/cards/MantenimientoCard'
import SimulationControlPanel from './SimulationControlPanel'
import { PedidoService } from '../../core/services/PedidoService'
import { IncidenciaService } from '../../core/services/IncidenciaService'
import { IndicadoresCard } from '../../components/common/cards/IndicadoresCard'
import { useSimulation } from '../../components/common/SimulationContextSemanal'


export default function WeeklySimulation() {
  const bgColor = useColorModeValue('white', '#1a1a1a')
  const [isCollapsed, setIsCollapsed] = useState(true)
  const [isSimulationLoading, setIsSimulationLoading] = useState(false);
  const [ isSimulationCompleted, setIsSImulationCompleted ] = useState(false);

  const [selectedDate, setSelectedDate] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
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
  const PedidosSection = () => {
    const { currentMinuteData } = useSimulation();
    const { isOpen, onOpen, onClose } = useDisclosure();
    const toast = useToast();
    const pedidoService = new PedidoService();
    const inputRef = useRef<HTMLInputElement>(null);

    const handleImport = async (file: File) => {
      try {
        await pedidoService.importarPedidos(file);
        toast({ title: 'Importación exitosa', status: 'success', duration: 3000 });
      } catch (error: any) {
        toast({ title: 'Error al importar', description: error.message, status: 'error', duration: 4000 });
      }
    };
    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {/* Modal de registro de pedidos */}
          {/* <Modal isOpen={isOpen} onClose={onClose} isCentered size="lg">
            <ModalOverlay />
            <ModalContent>
              <ModalBody>
                <PedidoForm onFinish={onClose} onCancel={onClose} />
              </ModalBody>
            </ModalContent>
          </Modal> */}
          {/* Boton con acciones desplegables */}
          {/* <Menu>
          <MenuButton
            as={Button}
            leftIcon={<FaPlus />}
            variant="secondary"
          >
            Agregar
          </MenuButton>
          <MenuList>
            <MenuItem onClick={onOpen} color="purple.700">Crear un pedido</MenuItem>
            <MenuItem onClick={() => inputRef.current?.click()} color="purple.700">Importar desde archivo
              <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={e => {
                if (e.target.files && e.target.files[0]) {
                  handleImport(e.target.files[0]);
                  e.target.value = '';
                }
              }} />
            </MenuItem>
          </MenuList>
        </Menu> */}
          {currentMinuteData?.pedidos
            ?.slice()
            .map((pedido) => (
              <PedidoCard key={pedido.idPedido} pedido={pedido} onClick={() => console.log('enfocando...')} />
            ))}
        </VStack>
      </Box>
    );
  };

  const FlotaSection = () => {
    const { currentMinuteData } = useSimulation();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {currentMinuteData?.vehiculos?.map((v) => (
            <FlotaCard key={v.idVehiculo} vehiculo={v} onClick={()=>console.log('enfocando...')}/>
          ))}
        </VStack>
      </Box>
    );
  };

  const AveriaSection = () => {
    const { currentMinuteData } = useSimulation();
    const { isOpen, onOpen, onClose } = useDisclosure();
    const incidenciaService = new IncidenciaService();
    const toast = useToast();
      const inputRef = useRef<HTMLInputElement>(null);

      // Solo registrar avería, no importar
      const handleRegister = async (data: Record<string, unknown>) => {
        try {
          await incidenciaService.createIncidencia(data);
          toast({ title: 'Avería registrada', status: 'success', duration: 3000 });
          onClose();
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : 'Error desconocido';
          toast({ title: 'Error al registrar', description: errorMessage, status: 'error', duration: 4000 });
        }
      };

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          {/* <PanelSearchBar onSubmit={() => console.log('searching...')} /> */}
          {/* Modal para crear avería */}
          {/* <ModalInsertAveria
            isOpen={isOpen}
            onClose={onClose}
            handleSubmit={handleRegister}
            plaque={''}
          />
          <Menu>
            <MenuButton
              as={Button}
              leftIcon={<FaPlus />}
              variant="secondary"
            >
              Agregar
            </MenuButton>
            <MenuList>
              <MenuItem onClick={onOpen}>Crear una avería</MenuItem>
              <MenuItem onClick={() => inputRef.current?.click()}>Importar desde archivo
                <Input type="file" display="none" ref={inputRef} accept=".csv,.xlsx,.xls,.txt" onChange={() => {}} />
              </MenuItem>
            </MenuList>
          </Menu> */}
          {currentMinuteData?.incidencias?.map((i) => (
            <IncidenciaCard key={i.idIncidencia} incidencia={i} onClick={()=>console.log('enfocando...')}/>
          ))}
        </VStack>
      </Box>
    );
  };

  const MantenimientoSection = () => {
    const { currentMinuteData } = useSimulation();

    return (
      <Box>
        <VStack spacing={4} align="stretch">
          <PanelSearchBar onSubmit={() => console.log('searching...')} />
          {currentMinuteData?.mantenimientos?.map((m) => (
            <MantenimientoCard key={m.idMantenimiento} mantenimiento={m} onClick={()=>console.log('enfocando...')}/>
          ))}
        </VStack>
      </Box>
    );
  };

  const IndicadoresSection = () => {
    const { currentMinuteData } = useSimulation();
     const [indicadoresVisibles, setIndicadoresVisibles] = useState(currentMinuteData?.indicadores);

    useEffect(() => {
      const interval = setInterval(() => {
        setIndicadoresVisibles(currentMinuteData?.indicadores);
      }, 2000000); // cada 10 segundos

      return () => clearInterval(interval); // limpieza al desmontar
    }, [currentMinuteData?.indicadores]); // reinicia si cambian los indicadores (opcional)

    const indicadores = indicadoresVisibles?? {
      fuelCounterTA: 0,
      fuelCounterTB: 0,
      fuelCounterTC: 0,
      fuelCounterTD: 0,
      fuelCounterTotal: 0,
      glpFilledNorth: 0,
      glpFilledEast: 0,
      glpFilledMain: 0,
      glpFilledTotal: 0,
      meanDeliveryTime: 0,
      completedOrders: 0,
      totalOrders: 0,
    };
    return (
      <Box>
        <VStack spacing={4} align="stretch">
            <IndicadoresCard key={'indicadores-default'} indicadores={indicadores}/>
        </VStack>
      </Box>
    );
  };

  const sections = [
  {
    title: 'Pedidos',
    content: <PedidosSection/>
  },
  {
    title: 'Flota',
    content: <FlotaSection/>
  },
  {
    title: 'Averias',
    content: <AveriaSection/>
  },
  {
    title: 'Mantenimiento',
    content: <MantenimientoSection/>
  },
  {
    title: 'Indicadores',
    content: <IndicadoresSection/>
  },
  ];

  const [section, setSection] = useState(sections[0].title);
  const { currentMinuteData, setCurrentMinuteData } = useSimulation();

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
                    <SimulationControlPanel setData={setCurrentMinuteData} data={currentMinuteData}/>
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
