import { Box, Button, Flex, Text } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { IncidenciaSimulada } from "../../../core/types/incidencia";

interface IncidenciaCardProps {
    incidencia: IncidenciaSimulada,
    onClick: () => void,
}
export const IncidenciaCard = ({
    incidencia,
    onClick,
}:IncidenciaCardProps) => {
    let cardColor;
    let isEstimada = false;
    let estadoText = '';
    const primaryTextSize = 18;
    //console.log('➡️ Estado recibido:', JSON.stringify(incidencia.estado), ` desde ${incidencia.fechaInicio}`);
    switch(incidencia.estado.toUpperCase()){
        case 'EN CURSO': 
            estadoText = 'En curso';
            cardColor = '#FFCFCF';
            break;
        case 'RESUELTA':
            estadoText = 'Resuelta';
            cardColor = '#C4C4C4'
            break;
        case 'INMINENTE':
            estadoText = 'Inminente';
            cardColor = 'white';
            isEstimada=true;
            break;
        default:
            estadoText = incidencia.estado;
            isEstimada=true;
            cardColor = 'gray.100';
            break;
    }
  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        <Flex align='end'>
            <Box flex='1'>
                {isEstimada ?
                <Flex gap={1} align='center'>
                    <Text id={"placa"} fontWeight={600} fontSize={primaryTextSize} color='purple.200'>
                        {incidencia.placa}
                    </Text>
                    |
                    <Text id={"turno"} fontWeight={600} fontSize={primaryTextSize} color='purple.200'>
                        {"Turno "+incidencia.turno.replace("T", "")}
                    </Text>
                    <Text id='state' variant='outline' pl={4}>{estadoText}</Text>
                </Flex>
                :
                <Flex gap={4} align='center'>
                    <Text id='fechaInicio' fontWeight={600} fontSize={primaryTextSize} color='purple.200'>
                        {incidencia.fechaInicio}
                    </Text>
                    <Text id='estado'>{estadoText}</Text>
                </Flex>
                }
                
            </Box>
        </Flex>
        <Flex gap={1} color='grey' fontSize={14}>
        {isEstimada 
        ?
        <>
            <Text id='tipo'>Incidente Tipo {incidencia.tipo.replace("Ti", "")}</Text>
        </>
            :
            <>
            <Text id='placa'>{incidencia.placa}</Text>
            |
            <Text id='fechaFin'>Fin: {incidencia.fechaFin}</Text>
            |
            <Text id='tipo'>Incidente Tipo {incidencia.tipo.replace("Ti", "")}</Text>
            </>
        }
        </Flex>
    </Flex>
  </>
);
}