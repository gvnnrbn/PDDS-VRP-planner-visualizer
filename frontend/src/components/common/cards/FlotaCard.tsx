import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { VehiculoSimuladoV2 } from "../../../core/types/vehiculo";

interface FlotaCardProps {
    vehiculo: VehiculoSimuladoV2,
    onClick: () => void,
}
export const FlotaCard = ({
    vehiculo,
    onClick,
}:FlotaCardProps) => {
    const estado = vehiculo.estado;
    const idPedido = vehiculo.idPedido || 0;// cambiar a como este el ultimo json
    const codigoPedido = `PE${idPedido.toString().padStart(3, '0')}`;
    const hasOrder = idPedido > 0;
    let cardColor = 'white'; // Default color
    let isFocus = false;
    let hasRoute = true;
    let combustiblePercentage = '';
    let isBroken = false;
    let estadoText = '';

    if(vehiculo.tipo == 'TA'){
        //console.log('➡️ Estado recibido:', JSON.stringify(vehiculo.eta), ` desde ${vehiculo.placa}`);
    }

    switch(estado.toUpperCase()){
        case 'STUCK': 
            cardColor = '#FFCFCF';
            isBroken = true;
            estadoText = 'Averiado';
        break;
        case 'REPAIR': 
            cardColor = '#FFCFCF';
            isBroken = true;
            estadoText = 'Averiado';
        break;
        case 'MAINTENANCE':
            cardColor = '#FFF9CD';
            estadoText = 'En Mantenimiento';
            break;
        case 'IDLE':
            hasRoute = false;
            isFocus = true;
            estadoText = 'Sin Programación';
            break;
        case 'ONTHEWAY':
            estadoText = 'En Ruta';
            break;
        case 'RETURNING_TO_BASE':
            estadoText = 'Regresando a almacén';
            break;
        case 'FINISHED':
            hasRoute = false;
            isFocus = true;
            estadoText = 'Sin Programación';
            break;
        default:
            cardColor = 'white';
            break;
    }
    if(vehiculo.estado.toUpperCase() != 'SIN PROGRAMACIÓN'){
        combustiblePercentage = (vehiculo.combustible / vehiculo.maxCombustible * 100).toFixed(2);
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
            <Flex gap={1} align='center'>
                <Text id={"placa"} fontWeight={600} fontSize={18} color='purple.200'>
                    {vehiculo.placa}
                </Text>
                <Text id={'state'} pl={4}>{estadoText}</Text>
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
               { hasOrder ? <Text id='pedidoId'>Pedido {codigoPedido}</Text> : <></>}
                <Flex direction={'row'} gap={1}>

                {isBroken
                ? <></>
                :
                    <>
                    {/* <Text id='eta'>ETA: {vehiculo.eta}</Text>
                    | */}
                    </>
                }
                <Text id='glp'>GLP: {vehiculo.currGLP}m³</Text>
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