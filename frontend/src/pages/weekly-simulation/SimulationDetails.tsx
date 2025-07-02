import React, { useState } from 'react';
import {
  Box,
  VStack,
  HStack,
  Text,
  Button,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Badge,
  Accordion,
  AccordionItem,
  AccordionButton,
  AccordionPanel,
  AccordionIcon,
  useColorModeValue,
  Flex,
  Stat,
  StatLabel,
  StatNumber,
  StatGroup,
  Grid,
  GridItem,
  Card,
  CardBody,
  CardHeader,
  Heading,
  Divider
} from '@chakra-ui/react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaArrowLeft, FaDownload, FaChartBar, FaTruck, FaBox, FaExclamationTriangle, FaFilePdf } from 'react-icons/fa';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

interface SimulationDetailsProps {
  simulationData?: any;
}

const SimulationDetails: React.FC<SimulationDetailsProps> = ({ simulationData }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const bgColor = useColorModeValue('white', '#1a1a1a');
  const [selectedMinute, setSelectedMinute] = useState<number | null>(null);

  // Obtener datos de la simulación desde el estado de navegación o props
  const data = simulationData || location.state?.simulationData;

  // Debug: mostrar información de los datos recibidos
  console.log("SimulationDetails - Data recibida:", data);
  console.log("SimulationDetails - Location state:", location.state);

  if (!data) {
    return (
      <Box p={4}>
        <Text>No hay datos de simulación disponibles</Text>
        <Text fontSize="sm" color="gray.500" mt={2}>
          Debug: location.state = {JSON.stringify(location.state)}
        </Text>
        <Button onClick={() => navigate('/weekly-simulation')} mt={4}>
          Volver
        </Button>
      </Box>
    );
  }

  const handleExportData = () => {
    const dataStr = JSON.stringify(data, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `simulacion_${data.fechaInicio.replace(/[\/\s:]/g, '_')}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleExportPDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(16);
    doc.text('Resumen de la Simulación', 14, 18);
    doc.setFontSize(12);
    let y = 28;
    doc.text(`Fecha de Inicio: ${data.fechaInicio}`, 14, y); y += 8;
    doc.text(`Fecha de Fin: ${data.fechaFin}`, 14, y); y += 8;
    doc.text(`Duración: ${data.duracion}`, 14, y); y += 8;
    doc.text(`Tiempo de Planificación: ${data.tiempoPlanificacion}`, 14, y); y += 8;
    doc.text(`Pedidos Entregados: ${data.pedidosEntregados}`, 14, y); y += 8;
    doc.text(`Consumo de Petróleo: ${data.consumoPetroleo} L`, 14, y); y += 8;
    if (data.estadisticas) {
      doc.text(`Vehículos Totales: ${data.estadisticas.totalVehiculos}`, 14, y); y += 8;
      doc.text(`Máx. Vehículos Activos en un Minuto: ${data.estadisticas.maxVehiculosActivosEnUnMinuto}`, 14, y); y += 8;
      doc.text(`Vehículos Activos Únicos: ${data.estadisticas.vehiculosActivosUnicos}`, 14, y); y += 8;
      doc.text(`Minutos Simulados: ${data.estadisticas.minutosSimulados}`, 14, y); y += 8;
    }
    doc.save(`Resumen_Simulacion_${data.fechaInicio.replace(/[\/\s:]/g, '_')}.pdf`);
  };

  const getStatusColor = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'DELIVERED':
        return 'green';
      case 'ONTHEWAY':
        return 'blue';
      case 'DELIVERING':
        return 'yellow';
      case 'IDLE':
        return 'gray';
      default:
        return 'gray';
    }
  };
// aquí se muestra el resumen de la simulación
  return (
    <Box p={6} bg={bgColor} minH="100vh">
      <VStack spacing={6} align="stretch">
        {/* Header */}
        <HStack justify="space-between" align="center">
          <HStack>
            <Button
              leftIcon={<FaArrowLeft />}
              variant="ghost"
              onClick={() => navigate('/weekly-simulation')}
            >
              Volver
            </Button>
            <Heading size="lg">Detalles de la Simulación</Heading>
          </HStack>
          <HStack spacing={2}>
            <Button
              leftIcon={<FaFilePdf />}
              colorScheme="red"
              onClick={handleExportPDF}
            >
              Exportar PDF
            </Button>
            <Button
              leftIcon={<FaDownload />}
              colorScheme="blue"
              onClick={handleExportData}
            >
              Exportar Datos
            </Button>
          </HStack>
        </HStack>

        <Divider />

        {/* Resumen General */}
        <Card>
          <CardHeader>
            <Heading size="md">
              Resumen General
            </Heading>
          </CardHeader>
          <CardBody>
            <StatGroup>
              <Stat>
                <StatLabel>Fecha de Inicio</StatLabel>
                <StatNumber fontSize="lg">{data.fechaInicio}</StatNumber>
              </Stat>
              <Stat>
                <StatLabel>Fecha de Fin</StatLabel>
                <StatNumber fontSize="lg">{data.fechaFin}</StatNumber>
              </Stat>
              <Stat>
                <StatLabel>Duración</StatLabel>
                <StatNumber fontSize="lg">{data.duracion}</StatNumber>
              </Stat>
              <Stat>
                <StatLabel>Tiempo de Planificación</StatLabel>
                <StatNumber fontSize="lg">{data.tiempoPlanificacion}</StatNumber>
              </Stat>
            </StatGroup>
          </CardBody>
        </Card>

        {/* Estadísticas */}
        {data.estadisticas && (
          <Card>
            <CardHeader>
              <Heading size="md">Estadísticas</Heading>
            </CardHeader>
            <CardBody>
              <Grid templateColumns="repeat(auto-fit, minmax(200px, 1fr))" gap={4}>
                <GridItem>
                  <Stat>
                    <StatLabel>Pedidos Entregados</StatLabel>
                    <StatNumber color="green.500">{data.pedidosEntregados}</StatNumber>
                  </Stat>
                </GridItem>
                <GridItem>
                  <Stat>
                    <StatLabel>Consumo de Petróleo</StatLabel>
                    <StatNumber color="orange.500">{data.consumoPetroleo} L</StatNumber>
                  </Stat>
                </GridItem>
                <GridItem>
                  <Stat>
                    <StatLabel>Vehículos Totales</StatLabel>
                    <StatNumber>{data.estadisticas.totalVehiculos}</StatNumber>
                  </Stat>
                </GridItem>
                <GridItem>
                  <Stat>
                    <StatLabel>Máx. Vehículos Activos en un Minuto</StatLabel>
                    <StatNumber color="blue.500">{data.estadisticas.maxVehiculosActivosEnUnMinuto}</StatNumber>
                  </Stat>
                </GridItem>
                <GridItem>
                  <Stat>
                    <StatLabel>Vehículos Activos Únicos</StatLabel>
                    <StatNumber color="purple.500">{data.estadisticas.vehiculosActivosUnicos}</StatNumber>
                  </Stat>
                </GridItem>
                <GridItem>
                  <Stat>
                    <StatLabel>Minutos Simulados</StatLabel>
                    <StatNumber>{data.estadisticas.minutosSimulados}</StatNumber>
                  </Stat>
                </GridItem>
              </Grid>
            </CardBody>
          </Card>
        )}

        {/* Detalles por Minuto */}
        {data.simulacionCompleta && (
          <Card>
            <CardHeader>
              <Heading size="md">Detalles por Minuto</Heading>
            </CardHeader>
            <CardBody>
              <Accordion allowToggle>
                {data.simulacionCompleta.map((minuto: any, index: number) => (
                  <AccordionItem key={index}>
                    <AccordionButton>
                      <Box flex="1" textAlign="left">
                        <Text fontWeight="bold">
                          Minuto {index + 1}: {minuto.minuto}
                        </Text>
                        <Text fontSize="sm" color="gray.500">
                          {minuto.vehiculos?.length || 0} vehículos, {minuto.pedidos?.length || 0} pedidos
                        </Text>
                      </Box>
                      <AccordionIcon />
                    </AccordionButton>
                    <AccordionPanel>
                      <VStack spacing={4} align="stretch">
                        {/* Vehículos */}
                        {minuto.vehiculos && minuto.vehiculos.length > 0 && (
                          <Box>
                            <Text fontWeight="semibold" mb={2}>
                              <FaTruck style={{ display: 'inline', marginRight: '8px' }} />
                              Vehículos ({minuto.vehiculos.length})
                            </Text>
                            <Table size="sm" variant="simple">
                              <Thead>
                                <Tr>
                                  <Th>ID</Th>
                                  <Th>Placa</Th>
                                  <Th>Estado</Th>
                                  <Th>Combustible</Th>
                                  <Th>GLP</Th>
                                  <Th>ETA</Th>
                                </Tr>
                              </Thead>
                              <Tbody>
                                {minuto.vehiculos.map((vehiculo: any, vIndex: number) => (
                                  <Tr key={vIndex}>
                                    <Td>{vehiculo.idVehiculo}</Td>
                                    <Td>{vehiculo.placa}</Td>
                                    <Td>
                                      <Badge colorScheme={getStatusColor(vehiculo.estado)}>
                                        {vehiculo.estado}
                                      </Badge>
                                    </Td>
                                    <Td>{vehiculo.combustible}/{vehiculo.maxCombustible}</Td>
                                    <Td>{vehiculo.currGLP}/{vehiculo.maxGLP}</Td>
                                    <Td>{vehiculo.eta || '-'}</Td>
                                  </Tr>
                                ))}
                              </Tbody>
                            </Table>
                          </Box>
                        )}

                        {/* Pedidos */}
                        {minuto.pedidos && minuto.pedidos.length > 0 && (
                          <Box>
                            <Text fontWeight="semibold" mb={2}>
                              <FaBox style={{ display: 'inline', marginRight: '8px' }} />
                              Pedidos ({minuto.pedidos.length})
                            </Text>
                            <Table size="sm" variant="simple">
                              <Thead>
                                <Tr>
                                  <Th>ID</Th>
                                  <Th>Estado</Th>
                                  <Th>GLP</Th>
                                  <Th>Fecha Límite</Th>
                                  <Th>Posición</Th>
                                </Tr>
                              </Thead>
                              <Tbody>
                                {minuto.pedidos.map((pedido: any, pIndex: number) => (
                                  <Tr key={pIndex}>
                                    <Td>{pedido.idPedido}</Td>
                                    <Td>
                                      <Badge colorScheme={getStatusColor(pedido.estado)}>
                                        {pedido.estado}
                                      </Badge>
                                    </Td>
                                    <Td>{pedido.glp}</Td>
                                    <Td>{pedido.fechaLimite}</Td>
                                    <Td>({pedido.posX}, {pedido.posY})</Td>
                                  </Tr>
                                ))}
                              </Tbody>
                            </Table>
                          </Box>
                        )}

                        {/* Incidencias */}
                        {minuto.incidencias && minuto.incidencias.length > 0 && (
                          <Box>
                            <Text fontWeight="semibold" mb={2}>
                              <FaExclamationTriangle style={{ display: 'inline', marginRight: '8px' }} />
                              Incidencias ({minuto.incidencias.length})
                            </Text>
                            <Table size="sm" variant="simple">
                              <Thead>
                                <Tr>
                                  <Th>ID</Th>
                                  <Th>Tipo</Th>
                                  <Th>Placa</Th>
                                  <Th>Estado</Th>
                                  <Th>Fecha Inicio</Th>
                                  <Th>Fecha Fin</Th>
                                </Tr>
                              </Thead>
                              <Tbody>
                                {minuto.incidencias.map((incidencia: any, iIndex: number) => (
                                  <Tr key={iIndex}>
                                    <Td>{incidencia.idIncidencia}</Td>
                                    <Td>{incidencia.tipo}</Td>
                                    <Td>{incidencia.placa}</Td>
                                    <Td>
                                      <Badge colorScheme={incidencia.estado === 'ACTIVE' ? 'red' : 'gray'}>
                                        {incidencia.estado}
                                      </Badge>
                                    </Td>
                                    <Td>{incidencia.fechaInicio}</Td>
                                    <Td>{incidencia.fechaFin}</Td>
                                  </Tr>
                                ))}
                              </Tbody>
                            </Table>
                          </Box>
                        )}
                      </VStack>
                    </AccordionPanel>
                  </AccordionItem>
                ))}
              </Accordion>
            </CardBody>
          </Card>
        )}
      </VStack>
    </Box>
  );
};

export default SimulationDetails; 