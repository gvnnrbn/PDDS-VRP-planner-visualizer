import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { PedidoSimulado } from "../../../core/types/pedido";

interface PedidoCardProps {
    pedido: PedidoSimulado,
    onClick: () => void,
}
export const PedidoCard = ({
    pedido,
    onClick,
}:PedidoCardProps) => {
    let cardColor;
    let isFocus = false;
    const codigoPedido = `PE${pedido.idPedido.toString().padStart(3, '0')}`;

    switch(pedido.estado.toUpperCase()){
            case 'EN CURSO': 
            cardColor = 'yellow';
        break;
        case 'PROGRAMADO':
            cardColor = 'white';
            break;
        case 'COMPLETADO':
            cardColor = '#C4C4C4'
            isFocus = true;
            break;
        default:
            cardColor = 'white';
            break;
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
                <Flex gap={4} align='center'>
                    <Text id='orderid' fontWeight={600} fontSize={18} color='purple.200'>{pedido.idPedido}</Text>
                    <Text id='state' variant='outline'>{pedido.estado}</Text>
                </Flex>
                
            </Box>
            <Box>
                {!isFocus 
                ? 
                <Button disabled={isFocus} size='sm' gap={1} variant='primary' onClick={onClick}>
                Enfocar
                <FontAwesomeIcon icon={faArrowsToDot} />
                </Button>
                :
                <></>}
            </Box>
        </Flex>
        <Flex gap={1}>
            <Text id='glp'>GLP: {pedido.glp}m³</Text>
            |
            <Text id='deadline'>Fecha Límite: {pedido.fechaLimite}</Text>
            </Flex>
            <Flex id='vehicles' direction='column'>
            {
            pedido.vehiculosAtendiendo.map((vehiculo) => (
            <Flex key={`vehiculo-${pedido.idPedido}x${vehiculo.placa}`} gap={1} color='grey' fontSize={14}>
                <Text>{vehiculo.placa}</Text>
                |
                <Text>ETA: {vehiculo.eta}</Text>
            </Flex>
            ))}
        </Flex>
    </Flex>
  </>
);
}