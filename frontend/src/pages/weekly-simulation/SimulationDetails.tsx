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
      <VStack spacing={6} align="center" maxW="1100px" mx="auto">
        {/* Header */}
        <HStack justify="space-between" align="center" w="100%">
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
        <Card maxW="900px" mx="auto" w="100%">
          <CardHeader>
            <Heading size="md" textAlign="center">
              Resumen General
            </Heading>
          </CardHeader>
          <CardBody>
            <StatGroup justifyContent="center" gap={6}>
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
          <Card maxW="900px" mx="auto" w="100%">
            <CardHeader>
              <Heading size="md" textAlign="center">Estadísticas</Heading>
            </CardHeader>
            <CardBody>
              <Grid templateColumns="repeat(auto-fit, minmax(180px, 1fr))" gap={3} justifyContent="center">
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

        {/* Tabla Resumen de Pedidos */}
        {data.simulacionCompleta && (
          <Card maxW="900px" mx="auto" w="100%">
            <CardHeader>
              <Heading size="md" textAlign="center">Resumen de Pedidos</Heading>
            </CardHeader>
            <CardBody>
              {(() => {
                // Procesar pedidos minuto a minuto
                const minutos = data.simulacionCompleta;
                const resumen: Record<string, any> = {};
                minutos.forEach((minutoSnap: any) => {
                  minutoSnap.pedidos.forEach((pedido: any) => {
                    if (!resumen[pedido.idPedido]) {
                      resumen[pedido.idPedido] = {
                        id: pedido.idPedido,
                        glp: pedido.glp,
                        fechaLimite: pedido.fechaLimite,
                        creadoEn: minutos[0].minuto, // O buscar el primer minuto donde aparece
                        estadoFinal: pedido.estado,
                        entregadoEn: null,
                        vehiculoEntrego: null,
                        pos: `(${pedido.posX}, ${pedido.posY})`,
                        destino: pedido.destino || '-',
                        vehiculosAtendiendo: pedido.vehiculosAtendiendo || [],
                      };
                    }
                    // Si se entregó en este minuto y no se había registrado antes
                    if (
                      pedido.estado === "COMPLETADO" &&
                      !resumen[pedido.idPedido].entregadoEn
                    ) {
                      resumen[pedido.idPedido].entregadoEn = minutoSnap.minuto;
                      // Buscar el vehículo que entregó (primer placa en vehiculosAtendiendo si existe)
                      if (pedido.vehiculosAtendiendo && pedido.vehiculosAtendiendo.length > 0) {
                        resumen[pedido.idPedido].vehiculoEntrego = pedido.vehiculosAtendiendo[0].placa;
                      } else {
                        resumen[pedido.idPedido].vehiculoEntrego = '-';
                      }
                    }
                    // Actualiza el estado final y los vehículos atendiendo
                    resumen[pedido.idPedido].estadoFinal = pedido.estado;
                    resumen[pedido.idPedido].vehiculosAtendiendo = pedido.vehiculosAtendiendo || [];
                  });
                });
                let pedidosResumen = Object.values(resumen);

                // Filtro por estado
                const [estadoFiltro, setEstadoFiltro] = React.useState<string>('');
                if (estadoFiltro) {
                  pedidosResumen = pedidosResumen.filter((pedido: any) => pedido.estadoFinal === estadoFiltro);
                }

                // Paginación
                const [pagina, setPagina] = React.useState(1);
                const porPagina = 10;
                const totalPaginas = Math.ceil(pedidosResumen.length / porPagina);
                const pedidosPagina = pedidosResumen.slice((pagina - 1) * porPagina, pagina * porPagina);

                // Estados únicos para el filtro
                const estadosUnicos = Array.from(new Set(Object.values(resumen).map((p: any) => p.estadoFinal)));

                return (
                  <Box>
                    {/* Filtros */}
                    <HStack mb={4}>
                      <Text>Filtrar por estado:</Text>
                      <select value={estadoFiltro} onChange={e => { setEstadoFiltro(e.target.value); setPagina(1); }}>
                        <option value="">Todos</option>
                        {estadosUnicos.map((estado) => (
                          <option key={estado} value={estado}>{estado}</option>
                        ))}
                      </select>
                    </HStack>
                    <Table variant="simple" size="sm">
                      <Thead>
                        <Tr>
                          <Th>ID</Th>
                          <Th>Estado Final</Th>
                          <Th>GLP</Th>
                          <Th>Fecha Límite</Th>
                          <Th>Posición</Th>
                          <Th>Destino</Th>
                        </Tr>
                      </Thead>
                      <Tbody>
                        {pedidosPagina.map((pedido: any) => (
                          <Tr key={pedido.id}>
                            <Td>{pedido.id}</Td>
                            <Td>
                              <Badge colorScheme={pedido.estadoFinal === "COMPLETADO" ? "green" : "yellow"}>
                                {pedido.estadoFinal}
                              </Badge>
                            </Td>
                            <Td>{pedido.glp}</Td>
                            <Td>{pedido.fechaLimite || '-'}</Td>
                            <Td>{pedido.pos}</Td>
                            <Td>{pedido.destino}</Td>
                          </Tr>
                        ))}
                      </Tbody>
                    </Table>
                    {/* Paginación */}
                    <HStack mt={4} justify="center">
                      <Button size="sm" onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={pagina === 1}>Anterior</Button>
                      <Text>Página {pagina} de {totalPaginas}</Text>
                      <Button size="sm" onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={pagina === totalPaginas}>Siguiente</Button>
                    </HStack>
                  </Box>
                );
              })()}
            </CardBody>
          </Card>
        )}

        {/* Tabla Resumen de Mantenimientos */}
        {data.simulacionCompleta && (() => {
          // Procesar mantenimientos minuto a minuto
          const minutos = data.simulacionCompleta;
          const resumen: Record<string, any> = {};
          minutos.forEach((minutoSnap: any) => {
            (minutoSnap.mantenimientos || []).forEach((mnt: any) => {
              if (!resumen[mnt.idMantenimiento]) {
                resumen[mnt.idMantenimiento] = { ...mnt };
              } else {
                resumen[mnt.idMantenimiento] = { ...resumen[mnt.idMantenimiento], ...mnt };
              }
            });
          });
          let mantenimientosResumen = Object.values(resumen);
          if (mantenimientosResumen.length === 0) return null;

          // Filtro por estado
          const [estadoFiltro, setEstadoFiltro] = React.useState<string>('');
          if (estadoFiltro) {
            mantenimientosResumen = mantenimientosResumen.filter((mnt: any) => mnt.estado === estadoFiltro);
          }
          // Estados únicos para el filtro
          const estadosUnicos = Array.from(new Set(mantenimientosResumen.map((mnt: any) => mnt.estado)));

          // Paginación
          const [pagina, setPagina] = React.useState(1);
          const porPagina = 10;
          const totalPaginas = Math.ceil(mantenimientosResumen.length / porPagina);
          const mantenimientosPagina = mantenimientosResumen.slice((pagina - 1) * porPagina, pagina * porPagina);

          // Verificar si la columna tipo está vacía en todos los registros
          const mostrarTipo = mantenimientosResumen.some((mnt: any) => mnt.vehiculo?.tipo && mnt.vehiculo.tipo !== '-');

          return (
            <Card mt={4} maxW="900px" mx="auto" w="100%">
              <CardHeader>
                <Heading size="md" textAlign="center">Resumen de Mantenimientos</Heading>
              </CardHeader>
              <CardBody>
                <HStack mb={4}>
                  <Text>Filtrar por estado:</Text>
                  <select value={estadoFiltro} onChange={e => { setEstadoFiltro(e.target.value); setPagina(1); }}>
                    <option value="">Todos</option>
                    {estadosUnicos.map((estado) => (
                      <option key={estado} value={estado}>{estado}</option>
                    ))}
                  </select>
                </HStack>
                <Table variant="simple" size="sm">
                  <Thead>
                    <Tr>
                      <Th>ID</Th>
                      <Th>Placa</Th>
                      {mostrarTipo && <Th>Tipo</Th>}
                      <Th>Estado</Th>
                      <Th>Fecha Inicio</Th>
                      <Th>Fecha Fin</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {mantenimientosPagina.map((mnt: any) => (
                      <Tr key={mnt.idMantenimiento}>
                        <Td>{mnt.idMantenimiento}</Td>
                        <Td>{mnt.vehiculo?.placa || '-'}</Td>
                        {mostrarTipo && <Td>{mnt.vehiculo?.tipo || '-'}</Td>}
                        <Td>{mnt.estado || '-'}</Td>
                        <Td>{mnt.fechaInicio || '-'}</Td>
                        <Td>{mnt.fechaFin || '-'}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
                <HStack mt={4} justify="center">
                  <Button size="sm" onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={pagina === 1}>Anterior</Button>
                  <Text>Página {pagina} de {totalPaginas}</Text>
                  <Button size="sm" onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={pagina === totalPaginas}>Siguiente</Button>
                </HStack>
              </CardBody>
            </Card>
          );
        })()}

        {/* Tabla Resumen de Averías (Incidencias) */}
        {data.simulacionCompleta && (() => {
          // Procesar incidencias minuto a minuto
          const minutos = data.simulacionCompleta;
          const resumen: Record<string, any> = {};
          minutos.forEach((minutoSnap: any) => {
            (minutoSnap.incidencias || []).forEach((inc: any) => {
              if (!resumen[inc.idIncidencia]) {
                resumen[inc.idIncidencia] = { ...inc };
              } else {
                resumen[inc.idIncidencia] = { ...resumen[inc.idIncidencia], ...inc };
              }
            });
          });
          let incidenciasResumen = Object.values(resumen);
          if (incidenciasResumen.length === 0) return null;

          // Filtro por estado y tipo
          const [estadoFiltro, setEstadoFiltro] = React.useState<string>('');
          const [tipoFiltro, setTipoFiltro] = React.useState<string>('');
          if (estadoFiltro) {
            incidenciasResumen = incidenciasResumen.filter((inc: any) => inc.estado === estadoFiltro);
          }
          if (tipoFiltro) {
            incidenciasResumen = incidenciasResumen.filter((inc: any) => inc.tipo === tipoFiltro);
          }
          // Estados y tipos únicos para los filtros
          const estadosUnicos = Array.from(new Set(incidenciasResumen.map((inc: any) => inc.estado)));
          const tiposUnicos = Array.from(new Set(incidenciasResumen.map((inc: any) => inc.tipo)));

          // Paginación
          const [pagina, setPagina] = React.useState(1);
          const porPagina = 10;
          const totalPaginas = Math.ceil(incidenciasResumen.length / porPagina);
          const incidenciasPagina = incidenciasResumen.slice((pagina - 1) * porPagina, pagina * porPagina);

          return (
            <Card mt={4} maxW="900px" mx="auto" w="100%">
              <CardHeader>
                <Heading size="md" textAlign="center">Resumen de Averías</Heading>
              </CardHeader>
              <CardBody>
                <HStack mb={4} gap={4}>
                  <Text>Filtrar por estado:</Text>
                  <select value={estadoFiltro} onChange={e => { setEstadoFiltro(e.target.value); setPagina(1); }}>
                    <option value="">Todos</option>
                    {estadosUnicos.map((estado) => (
                      <option key={estado} value={estado}>{estado}</option>
                    ))}
                  </select>
                  <Text>Filtrar por tipo:</Text>
                  <select value={tipoFiltro} onChange={e => { setTipoFiltro(e.target.value); setPagina(1); }}>
                    <option value="">Todos</option>
                    {tiposUnicos.map((tipo) => (
                      <option key={tipo} value={tipo}>{tipo}</option>
                    ))}
                  </select>
                </HStack>
                <Table variant="simple" size="sm">
                  <Thead>
                    <Tr>
                      <Th>ID</Th>
                      <Th>Placa</Th>
                      <Th>Tipo</Th>
                      <Th>Estado</Th>
                      <Th>Fecha Inicio</Th>
                      <Th>Fecha Fin</Th>
                      <Th>Turno</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {incidenciasPagina.map((inc: any) => (
                      <Tr key={inc.idIncidencia}>
                        <Td>{inc.idIncidencia}</Td>
                        <Td>{inc.placa || '-'}</Td>
                        <Td>{inc.tipo || '-'}</Td>
                        <Td>{inc.estado || '-'}</Td>
                        <Td>{inc.fechaInicio || '-'}</Td>
                        <Td>{inc.fechaFin || '-'}</Td>
                        <Td>{inc.turno || '-'}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
                <HStack mt={4} justify="center">
                  <Button size="sm" onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={pagina === 1}>Anterior</Button>
                  <Text>Página {pagina} de {totalPaginas}</Text>
                  <Button size="sm" onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={pagina === totalPaginas}>Siguiente</Button>
                </HStack>
              </CardBody>
            </Card>
          );
        })()}

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