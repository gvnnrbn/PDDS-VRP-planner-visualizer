import { Box, ChakraProvider } from '@chakra-ui/react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navbar } from './components/common/Navbar'
import { SidebarProvider } from './components/common/Sidebar'
import WeeklySimulation from './pages/weekly-simulation'
import CollapseSimulation from './pages/collapse-simulation'
import DailyOperation from './pages/daily-operation'


import { customTheme } from './components/ui/theme'
import PedidosPhase from './pages/weekly-simulation/PedidosPhase'
import IncidenciasPhase from './pages/weekly-simulation/IncidenciasPhase'
import VehiculosPhase from './pages/weekly-simulation/VehiculosPhase'
import AlmacenPhase from './pages/weekly-simulation/AlmacenPhase'
import SimulationDetails from './pages/weekly-simulation/SimulationDetails'
import Home from './pages/Home'
import { SimulationProvider } from './components/common/SimulationContextSemanal';

// Create a client
const queryClient = new QueryClient()

function App() {
  const initialSimData = {
  minuto: '08/05/2025 08:00',
  vehiculos: [],
  pedidos: [],
  almacenes: [],
  incidencias: [],
  mantenimientos: []
};
  return (
    <ChakraProvider theme={customTheme}>
      <QueryClientProvider client={queryClient}>
        <SimulationProvider>
        <Router>
          <SidebarProvider>
            <Box h="100vh" display="flex" flexDirection="column">
              <Navbar />
              <Box flex={1} overflowY="auto">
                <Routes>
                  <Route path="/pedidos" element={<PedidosPhase />} />
                  <Route path="/incidencias" element={<IncidenciasPhase />} />
                  <Route path="/vehiculos" element={<VehiculosPhase />} />
                  <Route path="/almacen" element={<AlmacenPhase />} />
                  <Route path="/semanal/*" element={<WeeklySimulation />} />
                  <Route path="/weekly-simulation/details" element={<SimulationDetails />} />
                  <Route path="/colapso/*" element={<CollapseSimulation />} />
                  <Route path="/dia-a-dia/*" element={<DailyOperation />} />
                  <Route path="/" element={<Home />} />
                </Routes>
              </Box>
            </Box>
          </SidebarProvider>
        </Router>
        </SimulationProvider>
      </QueryClientProvider>
    </ChakraProvider>
  )
}

export default App
