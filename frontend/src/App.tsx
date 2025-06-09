import { Box, ChakraProvider, } from '@chakra-ui/react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navbar } from './components/common/Navbar'
import WeeklySimulation from './pages/weekly-simulation'
import CollapseSimulation from './pages/collapse-simulation'
import DailyOperation from './pages/daily-operation'
import { customTheme } from './components/ui/theme'


// Create a client
const queryClient = new QueryClient()

function App() {
  return (
    <ChakraProvider theme={customTheme}>
      <QueryClientProvider client={queryClient}>
        <Router>
          <Box h="100vh" display="flex" flexDirection="column">
            <Navbar />
            <Box flex={1} overflowY="auto">
              <Routes>
                <Route path="/semanal/*" element={<WeeklySimulation />} />
                <Route path="/colapso/*" element={<CollapseSimulation />} />
                <Route path="/dia-a-dia/*" element={<DailyOperation />} />
              </Routes>
            </Box>
          </Box>
        </Router>
      </QueryClientProvider>
    </ChakraProvider>
  )
}

export default App
