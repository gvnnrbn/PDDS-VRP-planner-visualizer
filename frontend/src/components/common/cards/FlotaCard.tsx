import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { VehiculoSimulado } from "../../../core/types/vehiculo";

interface FlotaCardProps {
    vehiculo: VehiculoSimulado,
    onClick: () => void,
}
export const FlotaCard = ({
    vehiculo,
    onClick,
}:FlotaCardProps) => {
    let cardColor;
    let isFocus = false;
    let hasRoute = true;
    let combustiblePercentage = 0;
    let isBroken = false;

    switch(vehiculo.estado.toUpperCase()){
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
    if(vehiculo.estado.toUpperCase() != 'SIN PROGRAMACIÓN'){
        combustiblePercentage = vehiculo.combustible / vehiculo.maxCombustible * 100;
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
            <Flex gap={1} align='center'>
                <Text id={"placa"} fontWeight={600} fontSize={18} color='purple.200'>
                    {vehiculo.placa}
                </Text>
                <Text id={'state'} pl={4}>{vehiculo.estado}</Text>
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
                <Text id='pedidoId'>Pedido {vehiculo.pedidoId}</Text>
                <Flex direction={'row'} gap={1}>

                {isBroken
                ? <></>
                :
                    <>
                    <Text id='eta'>ETA: {vehiculo.eta}</Text>
                    |
                    </>
                }
                <Text id='glp'>GLP: {vehiculo.glp}m³</Text>
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