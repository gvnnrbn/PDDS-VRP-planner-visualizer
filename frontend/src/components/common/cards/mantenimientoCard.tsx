import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IMantenimientoCard } from "../../../../core/types/mantenimiento.ts";

interface IncidenciaCardProps {
    mantenimientoCard: IMantenimientoCard,
    onClick: () => void,
}
export const MantenimientoCard = ({
    mantenimientoCard,
    onClick,
}:IncidenciaCardProps) => {
    let cardColor;
    let isFocus = false;
    let isEnCurso = false;
    const primaryTextSize = 18;
    switch(mantenimientoCard.estado.toUpperCase()){
            case 'EN CURSO': 
            cardColor = '#FFCFCF';
            isEnCurso = true;
        break;
        case 'TERMINADO':
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
                <Flex gap={1} align='center'>
                    <Text id={"placa"} fontWeight={600} fontSize={primaryTextSize} color='purple.200'>
                        {mantenimientoCard.vehiculo.placa}
                    </Text>
                    <Text id='state' variant='outline' pl={4}>{mantenimientoCard.estado}</Text>
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
        <Flex gap={1} color='grey' fontSize={14}>
            <Text id='tipo'>Tipo Vehículo: {mantenimientoCard.vehiculo.tipo}</Text>
            |
        {isEnCurso 
        ?
            <Text id='fechaFin'>Fin: {mantenimientoCard.fechaFin}</Text>
            :
            <Text id='fechaInicio'>Fecha: {mantenimientoCard.fechaInicio}</Text>
        }
        </Flex>
    </Flex>
  </>
);
}