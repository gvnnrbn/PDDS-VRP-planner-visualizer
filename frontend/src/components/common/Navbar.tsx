import { Box, Flex, Link, useColorModeValue } from '@chakra-ui/react'
import { Link as RouterLink, useLocation } from 'react-router-dom'

const NavLink = ({ to, children }: { to: string; children: React.ReactNode }) => {
  const location = useLocation()
  const isActive = to === "/" ? location.pathname === "/" : location.pathname.includes(to.split('/')[1]);
  
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
  const bgColor = useColorModeValue("#483190", "#2d1c5b");
  const textColor = useColorModeValue("#e8edef", "#ffffff");

  return (
    <Box
      bg={bgColor}
      color={textColor}
      px={8}
      borderColor={useColorModeValue("gray.200", "gray.700")}
      height="4rem"
      zIndex={20}
    >
      <Flex h="full" alignItems="center" justifyContent="space-between">
        {/* Lado izquierdo: LOGO (sin hover ni estado activo) */}
        <Link
          as={RouterLink}
          to="/"
          h="full"
          display="flex"
          alignItems="center"
          justifyContent="center"
          px={6}
          fontSize="lg"
          fontWeight="bold"
          _hover={{ textDecoration: "none" }}
        >
          PLG-PapuSystem
        </Link>

        {/* Lado derecho: NAV LINKS */}
        <Flex h="full" gap={4} alignItems="center">
          <NavLink to="/daily-operation">Operación día a día</NavLink>
          <NavLink to="/weekly-simulation/pedidos">Simulación Semanal</NavLink>
          <NavLink to="/collapse-simulation/pedidos">Simulación hasta el colapso</NavLink>
        </Flex>
      </Flex>
    </Box>
  );
};