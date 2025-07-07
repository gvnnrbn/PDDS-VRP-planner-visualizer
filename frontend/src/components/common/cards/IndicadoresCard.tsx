import React, { useMemo } from "react";
import {
  Box, Text, Flex, useColorModeValue, Divider, Center,
  background,
} from "@chakra-ui/react";
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
} from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';


// BarGLPChartAlt.tsx

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const backgroundColor = "#ECEFFC";

// CHART JS LIBRARY

ChartJS.register(ArcElement, Tooltip, Legend);

const COMPLETED_COLOR = "#3182CE";
const PENDING_COLOR = "lightblue";

interface PiePedidosChartProps {
  completed: number;
  total: number;
}

export const PiePedidosChartAlt = React.memo(({ completed, total }: PiePedidosChartProps) => {
  const data = {
    labels: ["Completados", "Restantes"],
    datasets: [
      {
        data: [completed, Math.max(total - completed, 0)],
        backgroundColor: [COMPLETED_COLOR, PENDING_COLOR],
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    cutout: "60%", // Hueco interior
    animation: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: function (ctx: any) {
            return `${ctx.label}: ${ctx.parsed}`;
          },
        },
      },
    },
  };

  return (
    <Box>
      <Text fontSize="sm" fontWeight="bold" textAlign="center" mb={2}>
        PEDIDOS COMPLETADOS
      </Text>

      <Center position="relative" w="100%" h="150px">
        <Box width="150px" height="150px">
          <Doughnut data={data} options={options} />
        </Box>
        <Box position="absolute" top="50%" left="50%" transform="translate(-50%, -50%)">
          <Text fontSize="xl" fontWeight="bold" color="blue.500">
            {completed} / {total}
          </Text>
        </Box>
      </Center>
    </Box>
  );
});

// PiePedidosChart.tsx
import { PieChart, Pie, Cell } from 'recharts';

interface PiePedidosChartProps {
  completed: number;
  total: number;
}

export const PiePedidosChart = React.memo(({ completed, total }: PiePedidosChartProps) => {
  const data = [
    { name: 'Completados', value: completed },
    { name: 'Restantes', value: Math.max(total - completed, 0) },
  ];

  return (
    <Box background={backgroundColor} borderRadius={10} p={3}>
      <Text fontSize="sm" fontWeight="bold" textAlign="center" mb={2}>PEDIDOS COMPLETADOS</Text>
      <Center position="relative" w="100%" h="100px">
        <PieChart width={150} height={150}>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={35}
            outerRadius={45}
            startAngle={90}
            endAngle={-270}
            paddingAngle={2}
            dataKey="value"
            isAnimationActive={false}
          >
            <Cell fill={COMPLETED_COLOR} />
            <Cell fill={PENDING_COLOR} />
          </Pie>
        </PieChart>
        <Box position="absolute" top="50%" left="50%" transform="translate(-50%, -50%)">
          <Text fontSize="l" fontWeight="bold" color="blue.500">
            {completed} / {total}
          </Text>
        </Box>
      </Center>
    </Box>
  );
});


ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend, ArcElement);

interface BarGLPChartProps {
  data: { name: string; value: number }[];
  total: number;
}
export const BarGLPChartAlt = React.memo(({ data, total }: BarGLPChartProps) => {
  const almacenColors = ["#48BB78", "#ED8936", "#38B2AC"];

  const chartData = {
    labels: data.map(d => d.name),
    datasets: [
      {
        label: 'GLP Recargado (Gal)',
        data: data.map(d => d.value),
        backgroundColor: almacenColors,
        barThickness: 25,
        barPercentage: 0.6,
        categoryPercentage: 0.7,
      },
    ],
  };

  const options = {
    responsive: true,
    animation: false,
    indexAxis: 'y' as const,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: function (ctx: any) {
            return `${ctx.label}: ${ctx.raw} Gal.`;
          },
        },
      },
      datalabels: {
        anchor: 'end',
        align: 'end',
        color: 'black',
        font: {
          weight: 'bold',
          size: 12,
        },
        formatter: (value: number) => `${value.toFixed(2)} Gal.`,
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: {
          precision: 0,
        },
      },
    },
  };

  return (
    <Box background={backgroundColor} borderRadius={10} p={3}>
      <Text fontSize="sm" fontWeight="bold" textAlign="center">
        GLP RECARGADO POR ALMACÉN
      </Text>
      <Box width="100%" height="200px">
        <Bar data={chartData} options={options} />
      </Box>
      <Text fontSize="sm" fontWeight="medium" textAlign="center" mt={0}>
        Total GLP recargado: <b>{total.toFixed(2)} Gal.</b>
      </Text>
    </Box>
  );
});

ChartJS.register(ArcElement, Tooltip, Legend);

const COLORS_PIE = ["#0341ae", "#ff971e", "#72cb3b", "#b20238"];

interface PieFuelChartProps {
  data: { name: string; value: number }[];
  total: number;
}

export const PieFuelChartAlt = React.memo(({ data, total }: PieFuelChartProps) => {
  const COLORS_PIE = ["#0341ae", "#ff971e", "#72cb3b", "#b20238"];

  const chartData = {
    labels: ["TA: " + data[0].value, "TB: " + data[1].value, "TC: " + data[2].value, "TD: " + data[3].value],
    datasets: [
      {
        data: [data[0].value, data[1].value, data[2].value, data[3].value],
        backgroundColor: COLORS_PIE,
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    animation: false,
    cutout: '0%', // hueco del doughnut
    plugins: {
      legend: {
        position: 'right',
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          boxWidth: 10,
          padding: 15,
        },
      },
      tooltip: {
        callbacks: {
          label: function (ctx: any) {
            return `${ctx.label}: ${ctx.parsed.toFixed(2)} Gal.`;
          },
        },
      }, 
    },
  };

  return (
    <Box background={backgroundColor} borderRadius={10} p={3}>
      <Text fontSize="sm" fontWeight="bold" textAlign="center">
        CONSUMO DE COMBUSTIBLE POR TIPO DE CAMIÓN
      </Text>
      <Box display="flex" justifyContent="center" alignItems="center">
        <Box width="180px" height="180px">
          <Doughnut data={chartData} options={options} />
        </Box>
      </Box>
      <Text fontSize="sm" textAlign="center">
        Total consumido: <b>{total.toFixed(2)} Gal.</b>
      </Text>
    </Box>
  );
});


// IndicadoresCard

interface IndicadoresSimulado {
  fuelCounterTA: number;
  fuelCounterTB: number;
  fuelCounterTC: number;
  fuelCounterTD: number;
  fuelCounterTotal: number;
  glpFilledNorth: number;
  glpFilledEast: number;
  glpFilledMain: number;
  glpFilledTotal: number;
  meanDeliveryTime: number;
  completedOrders: number;
  totalOrders: number;
}

interface IndicadoresCardProps {
  indicadores: IndicadoresSimulado;
}

const formatMinutesToHHMMSS = (minutos: number) => {
  const totalSeconds = Math.floor(minutos * 60);
  const h = Math.floor(totalSeconds / 3600).toString().padStart(2, "0");
  const m = Math.floor((totalSeconds % 3600) / 60).toString().padStart(2, "0");
  const s = (totalSeconds % 60).toString().padStart(2, "0");
  return `${h}h${m}m${s}s`;
};

export const IndicadoresCard = React.memo(({ indicadores }: IndicadoresCardProps) => {
  const cardColor = useColorModeValue("white", "gray.800");

  const fuelData = useMemo(() => [
    { name: "TA", value: indicadores.fuelCounterTA.toFixed(2) },
    { name: "TB", value: indicadores.fuelCounterTB.toFixed(2) },
    { name: "TC", value: indicadores.fuelCounterTC.toFixed(2) },
    { name: "TD", value: indicadores.fuelCounterTD.toFixed(2) },
  ], [indicadores]);

  const glpData = useMemo(() => [
    { name: "Norte", value: indicadores.glpFilledNorth.toFixed(2) },
    { name: "Este", value: indicadores.glpFilledEast.toFixed(2) },
    { name: "Principal", value: indicadores.glpFilledMain.toFixed(2) },
  ], [indicadores]);

  return (
    <Flex direction="column" bg={cardColor} borderRadius="10px" py={2} px={4} mx={-1} gap={3} boxShadow="md">
      <Flex gap={0} alignItems={'top'}>
        <PiePedidosChart completed={indicadores.completedOrders} total={indicadores.totalOrders} />
        
        <Box textAlign="center" background={backgroundColor} paddingX={4} paddingY={3} marginLeft={4} borderRadius={10}>
          <Text fontSize="m" fontWeight="bold">Tiempo Promedio <br />de Entrega</Text>
          <Text fontSize="xl" color="blue.500" fontWeight={700} mt={5}>{formatMinutesToHHMMSS(indicadores.meanDeliveryTime)}</Text>
        </Box>
      </Flex>
      <PieFuelChartAlt data={fuelData} total={indicadores.fuelCounterTotal} />
      <BarGLPChartAlt data={glpData} total={indicadores.glpFilledTotal}/>
    </Flex>
  );
}, (prev, next) => JSON.stringify(prev.indicadores) === JSON.stringify(next.indicadores));
