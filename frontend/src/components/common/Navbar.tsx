import { Box, Flex, Link, useColorModeValue, IconButton } from '@chakra-ui/react'
import { HamburgerIcon } from '@chakra-ui/icons'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { useSidebar } from './Sidebar'

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
        bg: 'purple.200',
        color: '#e8edef',
      }}
      sx={{
        ...(isActive && {
          bg: 'purple.200',
          color: '#e8edef',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        }),
      }}
    >
      {children}
    </Link>
  )
}

export const Navbar = () => {
  const bgColor = useColorModeValue('purple.100', '#2d1c5b')
  const textColor = useColorModeValue('#e8edef', '#ffffff')
  const { onOpen } = useSidebar();

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
        {/* Lado izquierdo: ÍCONO + LOGO */}
        <Flex alignItems="center" gap={2}>
          <IconButton
            aria-label="Menu"
            icon={<HamburgerIcon />}
            onClick={onOpen}
            variant="ghost"
            colorScheme="whiteAlpha"
            size="md"
          />
          <Link
            as={RouterLink}
            to="/"
            h="full"
            display="flex"
            alignItems="center"
            justifyContent="center"
            px={2}
            fontSize="lg"
            fontWeight="bold"
            _hover={{ textDecoration: "none" }}
          >
            PLG-System
          </Link>
        </Flex>
        {/* Lado derecho: NAV LINKS */}
        <Flex h="full" gap={4} alignItems="center">
          <NavLink to="/dia-a-dia">Operación día a día</NavLink>
          <NavLink to="/semanal/simulacion">Simulación Semanal</NavLink>
          <NavLink to="/colapso/simulacion">Simulación hasta el colapso</NavLink>
        </Flex>
      </Flex>
    </Box>
  );
};