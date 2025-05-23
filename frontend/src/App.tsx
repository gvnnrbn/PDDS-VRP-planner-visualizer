import { ChakraProvider, Box } from '@chakra-ui/react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import WeeklySimulation from './pages/weekly-simulation'
import CollapseSimulation from './pages/collapse-simulation'
import DailyOperation from './pages/daily-operation'

function App() {
  return (
    <ChakraProvider>
      <Router>
        <Box h="100vh" display="flex" flexDirection="column">
          <Navbar />
          <Box flex={1} overflowY="auto">
            <Routes>
              <Route path="/weekly-simulation" element={<WeeklySimulation />} />
              <Route path="/collapse-simulation" element={<CollapseSimulation />} />
              <Route path="/daily-operation" element={<DailyOperation />} />
            </Routes>
          </Box>
        </Box>
      </Router>
    </ChakraProvider>
  )
}

export default App
