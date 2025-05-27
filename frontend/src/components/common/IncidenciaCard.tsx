import { Badge, Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IIncidenciaCard } from "../../core/types/incidencia.ts";

interface IncidenciaCardProps {
    incidenciaCard: IIncidenciaCard,
    onClick: () => void,
}
export const IncidenciaCard = ({
    incidenciaCard,
    onClick,
}:IncidenciaCardProps) => {
    let cardColor;
    let isFocus = false;
    let isEstimada = false;
    switch(incidenciaCard.estado.toUpperCase()){
            case 'EN CURSO': 
            cardColor = '#FFCFCF';
        break;
        case 'ESTIMADA':
            cardColor = 'white';
            isEstimada = true;
            break;
        case 'RESUELTA':
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
                {isEstimada ?
                <Flex gap={1} align='center'>
                    <Text id={"placa"} fontWeight={600} fontSize={20} color='purple.200'>
                        {incidenciaCard.placa}
                    </Text>
                    |
                    <Text id={"turno"} fontWeight={600} fontSize={20} color='purple.200'>
                        {"Turno "+incidenciaCard.turno.replace("T", "")}
                    </Text>
                    <Text id='state' variant='outline' pl={4}>{incidenciaCard.estado}</Text>
                </Flex>
                :
                <Flex gap={4} align='center'>
                    <Text id='fechaInicio' fontWeight={600} fontSize={20} color='purple.200'>
                        {incidenciaCard.fechaInicio}
                    </Text>
                    <Text id='estado' variant='outline'>{incidenciaCard.estado}</Text>
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
            <Text id='tipo'>Incidente tipo {incidenciaCard.tipo.replace("TI", "")}</Text>
            :
            <>
            <Text id='placa'>{incidenciaCard.placa}</Text>
            |
            <Text id='fechaFin'>Fin: {incidenciaCard.fechaFin}</Text>
            |
            <Text id='tipo'>Incidente Tipo {incidenciaCard.tipo.replace("TI", "")}</Text>
            </>
        }
        </Flex>
    </Flex>
  </>
);
}