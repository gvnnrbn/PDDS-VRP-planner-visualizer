import { Badge, Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IOrderCard } from "../../core/types/pedido.ts";

interface OrderCardProps {
    orderCard: IOrderCard,
    onClick: () => void,
}
export const OrderCard = ({
    orderCard,
    onClick,
}:OrderCardProps) => {
    let cardColor;
    let isFocus = false;
    switch(orderCard.state.toUpperCase()){
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
                    <Text id='orderid' fontWeight={600} fontSize={22} color='purple.200'>{orderCard.orderId}</Text>
                    <Text id='state' variant='outline'>{orderCard.state}</Text>
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
            <Text id='glp'>GLP: {orderCard.glp}m³</Text>
            |
            <Text id='deadline'>Fecha Límite: {orderCard.deadline}</Text>
            </Flex>
            <Flex id='vehicles' direction='column'>
            { orderCard.vehicles.map((vehicle) => (
                <Flex id='vehicle' gap={1} color='grey' fontSize={14}>
                <Text id='plaque'>{vehicle.plaque}</Text>
                |
                <Text id='eta'>ETA: {vehicle.eta}</Text>
            </Flex>
            ))}
        </Flex>
    </Flex>
  </>
);
}