import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, VStack, HStack, Text, Button, Modal, ModalOverlay, ModalContent, ModalBody, useDisclosure, ModalHeader, ModalFooter } from '@chakra-ui/react'
import { PedidoForm } from '../../components/PedidoForm'
import { PedidoTable } from '../../components/PedidoTable'
import { PedidoService } from '../../core/services/PedidoService'
import { useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

export default function PedidosPhase() {
  const [showForm, setShowForm] = useState(false)
  const [selectedPedido, setSelectedPedido] = useState<any>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [errorImport, setErrorImport] = useState<string | null>(null);
    const { isOpen: isErrorOpen, onOpen: onErrorOpen, onClose: onErrorClose } = useDisclosure();

  const handleFormFinish = () => {
    setShowForm(false)
    setSelectedPedido(null)
  }

  const handleFormCancel = () => {
    setShowForm(false)
    setSelectedPedido(null)
  }

  const handlePedidoSelect = (pedido: any) => {
    setSelectedPedido(pedido)
    setShowForm(true)
  }

  const pedidoService = new PedidoService()

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    console.log('📁 handleFileUpload activado');
    if (!file) return;

    pedidoService.importarPedidos(file)
      .then(() => {
        queryClient.invalidateQueries({ queryKey: ['pedidos'] });
      })
      .catch((error: any) => {
        console.error('❌ Error importación (catch directo):', error);
        setErrorImport(error.message);
        onErrorOpen();
      })
      .finally(() => {
        // ✅ Resetear el input para permitir reimportar el mismo archivo
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      });
  };

  return (
    <Box p={4}>
      <VStack spacing={4} align="stretch">
        <HStack justify="space-between" align="center">
          <Box />
          <Text fontSize="2xl" fontWeight="bold" textAlign="center" width="100%">
            Gestión de Pedidos
          </Text>
          <Link to={'/incidencias'}>
            <Button variant='primary' width="15rem">Siguiente: Incidencias</Button>
          </Link>
        </HStack>
        <PedidoTable
          onPedidoSelect={handlePedidoSelect}
          onNuevoPedido={() => {
            setSelectedPedido(null)
            setShowForm(true)
          }}
          onImportarArchivo={() => fileInputRef.current?.click()}
        />
        <input
          type="file"
          accept=".txt"
          ref={fileInputRef}
          onChange={handleFileUpload}
          hidden
        />
        <Modal isOpen={showForm} onClose={handleFormCancel} isCentered size="lg">
          <ModalOverlay />
          <ModalContent>
            <ModalBody>
              <PedidoForm
                pedido={selectedPedido}
                onFinish={handleFormFinish}
                onCancel={handleFormCancel}
              />
            </ModalBody>
          </ModalContent>
        </Modal>
        <Modal isOpen={isErrorOpen} onClose={onErrorClose} isCentered size="md">
                <ModalOverlay />
                <ModalContent>
                  <ModalHeader color="red.500">❌ Error al importar pedidos</ModalHeader>
                  <ModalBody>
                    <Text>{errorImport}</Text>
                  </ModalBody>
                  <ModalFooter>
                    <Button colorScheme="red" onClick={onErrorClose}>
                      Cerrar
                    </Button>
                  </ModalFooter>
                </ModalContent>
              </Modal>
      </VStack>
    </Box>
  )
}
