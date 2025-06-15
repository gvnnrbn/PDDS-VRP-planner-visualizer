import { VStack, useDisclosure, Drawer, DrawerOverlay, DrawerContent, DrawerCloseButton, DrawerHeader, DrawerBody, Button } from '@chakra-ui/react'
import { Link } from 'react-router-dom'
import { FiPackage, FiAlertCircle, FiTruck, FiHome } from 'react-icons/fi'
import React, { createContext, useContext } from 'react'

const SidebarContext = createContext<{ onOpen: () => void } | undefined>(undefined)

export const useSidebar = () => {
  const context = useContext(SidebarContext)
  if (!context) throw new Error('useSidebar must be used within SidebarProvider')
  return context
}

export const SidebarProvider = ({ children }: { children: React.ReactNode }) => {
  const disclosure = useDisclosure()
  return (
    <SidebarContext.Provider value={{ onOpen: disclosure.onOpen }}>
      <Sidebar isOpen={disclosure.isOpen} onClose={disclosure.onClose} />
      {children}
    </SidebarContext.Provider>
  )
}

export const Sidebar = ({ isOpen, onClose }: { isOpen: boolean, onClose: () => void }) => {
  const menuItems = [
    { name: 'Registrar Pedidos', path: '/pedidos', icon: <FiPackage /> },
    { name: 'Incidencias', path: '/incidencias', icon: <FiAlertCircle /> },
    { name: 'Vehículos', path: '/vehiculos', icon: <FiTruck /> },
    { name: 'Almacenes', path: '/almacen', icon: <FiHome /> },
  ]

  return (
    <Drawer isOpen={isOpen} placement="left" onClose={onClose}>
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader>Menú Principal</DrawerHeader>
        <DrawerBody>
          <VStack spacing={4} align="stretch">
            {menuItems.map((item) => (
              <Button
                key={item.path}
                as={Link}
                to={item.path}
                variant="ghost"
                justifyContent="flex-start"
                leftIcon={item.icon}
                fontWeight="bold"
                fontSize="lg"
                onClick={onClose}
              >
                {item.name}
              </Button>
            ))}
          </VStack>
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
} 