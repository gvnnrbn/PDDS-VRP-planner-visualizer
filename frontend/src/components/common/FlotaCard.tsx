import { Badge, Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IFlotaCard } from "../../core/types/flota.ts";

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
    let isEstimada = false;
    switch(flotaCard.estado.toUpperCase()){
            case 'AVERIADO': 
            cardColor = '#FFCFCF';
        break;
        case 'EN MANTENIMIENTO':
            cardColor = '#FFF9CD';
            isEstimada = true;
            break;
        case 'SIN PROGRAMACIÓN':
            cardColor = 'white';
            break;
        default:
            cardColor = 'white';
            break;
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
                {isEstimada ?
                <Flex gap={1} align='center'>
                    <Text id={"placa"} fontWeight={600} fontSize={20} color='purple.200'>
                        {flotaCard.placa}
                    </Text>
                    |
                    <Text id={"turno"} fontWeight={600} fontSize={20} color='purple.200'>
                        {"Turno "+flotaCard.turno.replace("T", "")}
                    </Text>
                    <Text id='state' variant='outline' pl={4}>{flotaCard.estado}</Text>
                </Flex>
                :
                <Flex gap={4} align='center'>
                    <Text id='fechaInicio' fontWeight={600} fontSize={20} color='purple.200'>
                        {flotaCard.fechaInicio}
                    </Text>
                    <Text id='estado' variant='outline'>{flotaCard.estado}</Text>
                </Flex>
                }
                
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
        {isEstimada 
        ?
            <Text id='tipo'>Incidente tipo {flotaCard.tipo.replace("TI", "")}</Text>
            :
            <>
            <Text id='placa'>{flotaCard.placa}</Text>
            |
            <Text id='fechaFin'>Fin: {flotaCard.fechaFin}</Text>
            |
            <Text id='tipo'>Incidente Tipo {flotaCard.tipo.replace("TI", "")}</Text>
            </>
        }
        </Flex>
    </Flex>
  </>
);
}