import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IFlotaCard } from "../../../core/types/vehiculo";

interface FlotaCardProps {
    flotaCard: IFlotaCard,
    onClick: () => void,
}
export const FlotaCard = ({
    flotaCard,
    onClick,
}:FlotaCardProps) => {
    let cardColor;
    let isFocus = false;
    let hasRoute = true;
    let combustiblePercentage = 0;
    let isBroken = false;

    switch(flotaCard.estado.toUpperCase()){
            case 'AVERIADO': 
            cardColor = '#FFCFCF';
            isBroken = true;
        break;
        case 'EN MANTENIMIENTO':
            cardColor = '#FFF9CD';
            break;
        case 'SIN PROGRAMACIÓN':
            cardColor = 'white';
            hasRoute = false;
            isFocus = true;
            break;
        default:
            cardColor = 'white';
            break;
    }
    if(flotaCard.estado.toUpperCase() != 'SIN PROGRAMACIÓN'){
        combustiblePercentage = flotaCard.combustible / flotaCard.maxCombustible * 100;
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
            <Flex gap={1} align='center'>
                <Text id={"placa"} fontWeight={600} fontSize={18} color='purple.200'>
                    {flotaCard.placa}
                </Text>
                <Text id={'state'} pl={4}>{flotaCard.estado}</Text>
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
        <Flex gap={1} color='grey' fontSize={14} direction={'column'}>
        {!hasRoute 
        ?
            <></>
            :
            <>
                <Text id='pedidoId'>Pedido {flotaCard.pedidoId}</Text>
                <Flex direction={'row'} gap={1}>

                {isBroken
                ? <></>
                :
                    <>
                    <Text id='eta'>ETA: {flotaCard.eta}</Text>
                    |
                    </>
                }
                <Text id='glp'>GLP: {flotaCard.glp}m³</Text>
                |
                <Text id='combustible'>Combustible: {combustiblePercentage}%</Text>
                </Flex>
            </>
        }
        </Flex>
    </Flex>
  </>
);
}