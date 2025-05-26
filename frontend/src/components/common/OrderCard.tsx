import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IOrderCard } from "../../core/types/pedido.tsx";

interface OrderCardProps {
    orderCard: IOrderCard,
    onClick: () => void,
}
export const OrderCard = ({
    orderCard,
    onClick,
}:OrderCardProps) => {
  return (
    <Flex bg='white' borderRadius='10px' py={3} px={4} mx={-1}>
        <Box flex='1'>
            <Flex gap={4} align='end'>
            <Text id='orderid' fontSize={20} color='purple'>{orderCard.orderId}</Text>
            <Text id='state'>{orderCard.state}</Text>
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
        </Box>
        <Box>
            <Button gap={1} variant='primary' onClick={onClick}>
            Enfocar
            <FontAwesomeIcon icon={faArrowsToDot} />
            </Button>
        </Box>
    </Flex>
);
}