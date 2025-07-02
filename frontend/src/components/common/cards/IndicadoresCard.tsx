import type { IndicadoresSimulado } from "../../../core/types/indicadores";
import {
  Box, Text, Flex, useColorModeValue, Divider,
  Center,
} from "@chakra-ui/react";
import React from "react";
import {
  PieChart, Pie, Cell, RadialBarChart, RadialBar,
  BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer,
} from "recharts";


interface IndicadoresCardProps {
    indicadores: IndicadoresSimulado,
}

const COLORS = ["#4FD1C5", "#F687B3", "#63B3ED", "#9F7AEA"];
const almacenColors = ["#48BB78", "#ED8936", "#38B2AC"];

const formatMinutesToHHMMSS = (minutos: number) => {
  const totalSeconds = Math.floor(minutos * 60);
  const h = Math.floor(totalSeconds / 3600).toString().padStart(2, "0");
  const m = Math.floor((totalSeconds % 3600) / 60).toString().padStart(2, "0");
  const s = (totalSeconds % 60).toString().padStart(2, "0");
  return `${h}:${m}:${s}`;
};

export const IndicadoresCard = React.memo(({ indicadores }: IndicadoresCardProps) => {
    const cardColor = useColorModeValue("white", "gray.800");

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
   const totalOrders = indicadores.totalOrders;
const fuelData = [
    { name: "TA", value: fuelCounterTA },
    { name: "TB", value: fuelCounterTB },
    { name: "TC", value: fuelCounterTC },
    { name: "TD", value: fuelCounterTD },
];

const glpData = [
    { name: "Norte", value: glpFilledNorth },
    { name: "Este", value: glpFilledEast },
    { name: "Principal", value: glpFilledMain },
];

  const pedidosData = [
    {
      name: "Completados",
      value: completedOrders,
    },
    {
      name: "Pendientes",
      value: Math.max(totalOrders - completedOrders, 0),
    },
  ];
    const COMPLETED_COLOR = "#3182CE";
    const PENDING_COLOR = "#E2E8F0"; // gris claro
    // const COLORS_PIE = ["#8464A0", "#9B3192", "#9370DB", "#2B0B3F"]; // TA, TB, TC, TD
    const COLORS_PIE = ["#0341ae", "#ff971e", "#72cb3b", "#b20238"]; // TA, TB, TC, TD

  return (
    <Flex direction="column" bg={cardColor} borderRadius="10px" py={3} px={4} mx={-1} gap={4} boxShadow="md">
      {/* Pedidos Completados */}
<Box>
    <Text fontSize="sm" fontWeight="bold" textAlign="center" mb={2}>
        PEDIDOS COMPLETADOS
    </Text>
    <br />
    <br />
    <Center position="relative" w="100%" h="130px">
        <PieChart width={150} height={150} key={1}>
        <Pie
            isAnimationActive={false}
            data={[
            { name: "Completados", value: indicadores.completedOrders },
            {
                name: "Restantes",
                value: Math.max(indicadores.totalOrders - indicadores.completedOrders, 0),
            },
            ]}
            cx="50%"
            cy="50%"
            innerRadius={45}
            outerRadius={55}
            startAngle={90}
            endAngle={-270}
            paddingAngle={2}
            dataKey="value"
        >
            <Cell fill={COMPLETED_COLOR} />
            <Cell fill={PENDING_COLOR} />
        </Pie>
        </PieChart>

        <Box position="absolute" top="50%" left="50%" transform="translate(-50%, -50%)">
        <Text fontSize="xl" fontWeight="bold" color="blue.500">
            {indicadores.completedOrders} / {indicadores.totalOrders}
        </Text>
        </Box>
    </Center>
    </Box>

      {/* Tiempo Promedio */}
      <Box textAlign="center">
        <Text fontSize="sm" fontWeight="bold">Tiempo Promedio de Entrega</Text>
        <Text fontSize="xl" color="blue.500" fontWeight={700}>{formatMinutesToHHMMSS(indicadores.meanDeliveryTime)}</Text>
      </Box>

      <Divider />
    <br />

      {/* Consumo de Combustible */}
    <Box>
        <Box textAlign="center">
            <Text fontSize="sm" fontWeight="bold" mb={2}>CONSUMO DE COMBUSTIBLE POR TIPO DE CAMIÓN</Text>
        </Box>
        <ResponsiveContainer width="100%" height={240}>
            <PieChart key={2}>
            <Pie
                isAnimationActive={false}
                data={fuelData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label={({ name, value }) => `${name}: ${value.toFixed(2)} Gal.`}
            >
                {fuelData.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS_PIE[index % COLORS_PIE.length]} />
                ))}
            </Pie>
            <Tooltip formatter={(value: any, name: any) => [`${value.toFixed(2)} Gal.`, `Camión ${name}`]} />
            <Legend verticalAlign="bottom" height={36} />
            </PieChart>
        </ResponsiveContainer>

        <Text mt={2} fontSize="sm" fontWeight="medium" textAlign="center">
            Total consumido: <b>{fuelCounterTotal.toFixed(2)} Gal.</b>
        </Text>
    </Box>
    <br />

      <Divider />

      {/* GLP por almacén */}
      <Box>
        <Box textAlign="center">
            <Text fontSize="sm" fontWeight="bold" mb={2} >GLP RECARGADO POR ALMACÉN</Text>
        </Box>
        <ResponsiveContainer width="100%" height={180}>
          <BarChart
            key={3}
            data={glpData}
            layout="vertical"
            margin={{ top: 5, right: 20, left: 30, bottom: 5 }}
            >
            <XAxis type="number" />
            <YAxis dataKey="name" type="category" />
            <Tooltip />
            <Bar dataKey="value">
              isAnimationActive={false}
              {glpData.map((_, index) => (
                <Cell key={`bar-${index}`} fill={almacenColors[index % almacenColors.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </Flex>
  );
}, (prev, next) => {
  return JSON.stringify(prev.indicadores) === JSON.stringify(next.indicadores);
});