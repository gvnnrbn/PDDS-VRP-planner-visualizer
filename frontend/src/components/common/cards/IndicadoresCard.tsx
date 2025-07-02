import { Box, Button, Flex, Progress, Text, VStack } from "@chakra-ui/react";
import { faArrowsToDot } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { MantenimientoSimulado } from "../../../core/types/manetenimiento";
import type { IndicadoresSimulado } from "../../../core/types/indicadores";

interface IndicadoresCardProps {
    indicadores: IndicadoresSimulado,
}
export const IndicadoresCard = ({
    indicadores,
}:IndicadoresCardProps) => {
    const cardColor = 'white';

    const fuelCounterTA = Number(indicadores.fuelCounterTA.toFixed(2));
    const fuelCounterTB = Number(indicadores.fuelCounterTB.toFixed(2));
    const fuelCounterTC = Number(indicadores.fuelCounterTC.toFixed(2));
    const fuelCounterTD = Number(indicadores.fuelCounterTD.toFixed(2));
    const fuelCounterTotal = Number(indicadores.fuelCounterTotal.toFixed(2));
    const glpFilledNorth = Number(indicadores.glpFilledNorth.toFixed(2));
    const glpFilledEast = Number(indicadores.glpFilledEast.toFixed(2));
    const glpFilledMain = Number(indicadores.glpFilledMain.toFixed(2));
    const glpFilledTotal = Number(indicadores.glpFilledTotal.toFixed(2));
    const meanDeliveryTime = Number(indicadores.meanDeliveryTime.toFixed(2));
   const completedOrders = indicadores.completedOrders;

  return (<>
    <Flex direction='column' bg={cardColor} borderRadius='10px' py={3} px={4} mx={-1} gap={1}>
        
    </Flex>
  </>
);
}