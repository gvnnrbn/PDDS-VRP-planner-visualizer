import { Box, ChakraProvider } from '@chakra-ui/react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navbar } from './components/common/Navbar'
import WeeklySimulation from './pages/weekly-simulation'
import CollapseSimulation from './pages/collapse-simulation'
import DailyOperation from './pages/daily-operation'

// Create a client
const queryClient = new QueryClient()

function App() {
  return (
    <ChakraProvider>
      <QueryClientProvider client={queryClient}>
        <Router>
          <Box h="100vh" display="flex" flexDirection="column">
            <Navbar />
            <Box flex={1} overflowY="auto">
              <Routes>
                <Route path="/weekly-simulation/*" element={<WeeklySimulation />} />
                <Route path="/collapse-simulation/*" element={<CollapseSimulation />} />
                <Route path="/daily-operation/*" element={<DailyOperation />} />
              </Routes>
            </Box>
          </Box>
        </Router>
      </QueryClientProvider>
    </ChakraProvider>
  )
}

export default App
