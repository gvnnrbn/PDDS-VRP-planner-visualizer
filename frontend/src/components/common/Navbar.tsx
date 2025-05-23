import { Box, Flex, Link, useColorModeValue } from '@chakra-ui/react'
import { Link as RouterLink, useLocation } from 'react-router-dom'

const NavLink = ({ to, children }: { to: string; children: React.ReactNode }) => {
  const location = useLocation()
  const isActive = location.pathname.includes(to.split('/')[1])
  
  return (
    <Link
      as={RouterLink}
      to={to}
      h="full"
      display="flex"
      alignItems="center"
      justifyContent="center"
      px={6}
      fontSize="lg"
      fontWeight="bold"
      _hover={{
        bg: '#7f8ecf',
        color: '#483190',
      }}
      sx={{
        ...(isActive && {
          bg: '#7f8ecf',
          color: '#483190',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        }),
      }}
    >
      {children}
    </Link>
  )
}

export const Navbar = () => {
  const bgColor = useColorModeValue('#483190', '#2d1c5b')
  const textColor = useColorModeValue('#e8edef', '#ffffff')

  return (
    <Box
      bg={bgColor}
      color={textColor}
      px={8}
      borderColor={useColorModeValue('gray.200', 'gray.700')}
      height="4rem"
    >
      <Flex h="full" alignItems="center" justifyContent="flex-end">
        <NavLink to="/weekly-simulation/pedidos">
          Simulación Semanal
        </NavLink>
        <NavLink to="/collapse-simulation">
          Simulación hasta el colapso
        </NavLink>
        <NavLink to="/daily-operation">
          Operación día a día
        </NavLink>
      </Flex>
    </Box>
  )
}
