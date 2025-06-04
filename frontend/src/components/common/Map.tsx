import { useEffect, useRef, useState } from "react";
import { Stage, Layer, Line} from "react-konva";
import { VehicleIcon } from "./Icons/VehicleIcon";
import type {VehiculoSimulado} from "../../core/types/vehiculo";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { IncidenciaSimulado } from "../../core/types/incidencia";
import { OrderIcon} from "./Icons/OrderIcon"
import type { AlmacenSimulado } from "../../core/types/almacen";
import type { BloqueoSimulado } from "../../core/types/bloqueos";
import { WarehouseIcon } from "./Icons/WarehouseIcon";
import React from "react";
import type Konva from "konva";


const CELL_SIZE = 20; // Tamaño de cada celda
const GRID_WIDTH = 70; // Número de celdas a lo ancho
const GRID_HEIGHT = 50; // Número de celdas a lo alto

interface MinutoSimulacion {
  minuto: number;
  vehiculos: VehiculoSimulado[];
  pedidos: PedidoSimulado[];
  almacenes: AlmacenSimulado[];
  incidencias: IncidenciaSimulado[];
}

interface SimulacionJson {
  fechaInicio: string;
  simulacion: MinutoSimulacion[];
  bloqueos: BloqueoSimulado[];
}

interface MapGridProps {
  minuto: number;
  data: SimulacionJson;
}

export const MapGrid: React.FC<MapGridProps> = ({ minuto, data }) => {
    if (minuto < 0) return null;

    const stageRef = useRef<any>(null);
    const [scale, setScale] = useState(1);
    const [position, setPosition] = useState({ x: 0, y: 0 });

    const minutoActual = data.simulacion.find((m) => m.minuto === minuto);

    const vehiculosActuales = minutoActual?.vehiculos || [];

    const pedidos = minutoActual?.pedidos || [];
    const almacenes = minutoActual?.almacenes || [];

    const vehicleRefs = useRef<Record<number, Konva.Image | null>>({});
    const [progresoVehiculos, setProgresoVehiculos] = useState<Record<number, number>>({});

    const fechaSimulacionInicio = new Date(data.fechaInicio);
    const fechaActual = new Date(fechaSimulacionInicio);
    fechaActual.setDate(fechaSimulacionInicio.getDate() + minuto);
    //Bloqueos activos
    const bloqueosVisibles = data.bloqueos?.filter((b) => {
      const inicio = new Date(b.fechaInicio);
      const fin = new Date(b.fechaFin);
      return fechaActual >= inicio && fechaActual <= fin;
    }) || [];

    // Referencias
    useEffect(() => {
      vehicleRefs.current = {};
    }, []);

    //const almacenes = minutoData?.almacenes || [];

  // Generar líneas de la cuadrícula
    const gridLines = () => {
    const lines = [];

    for (let i = 0; i <= GRID_WIDTH; i++) {
      lines.push(
        <Line
          key={`v-${i}`}
          points={[i * CELL_SIZE, 0, i * CELL_SIZE, GRID_HEIGHT * CELL_SIZE]}
          stroke="#ddd"
          strokeWidth={1}
        />
      );
    }

    for (let j = 0; j <= GRID_HEIGHT; j++) {
      lines.push(
        <Line
          key={`h-${j}`}
          points={[0, j * CELL_SIZE, GRID_WIDTH * CELL_SIZE, j * CELL_SIZE]}
          stroke="#ddd"
          strokeWidth={1}
        />
      );
    }

    return lines;
  };

  useEffect(() => {
    const layer = stageRef.current?.getLayers?.()[0];
    if (layer) {
      layer.batchDraw();
    }
  }, [vehiculosActuales, pedidos, almacenes, bloqueosVisibles]);

  useEffect(() => {
    const container = stageRef.current?.container();
    if (container) {
      container.tabIndex = 1; // asegura que pueda recibir foco
    }
  }, []);


  return (
    <Stage
      width={window.innerWidth}
      height={window.innerHeight}
      draggable
      onDragMove={(e) => {
        const stage = e.target;
        setPosition({
          x: stage.x(),
          y: stage.y(),
        });
      }}
      ref={stageRef}
      onWheel={(e) => {
        e.evt.preventDefault();
        const scaleBy = 1.05;
        const stage = stageRef.current;

        const oldScale = scale;
        const pointer = stage.getPointerPosition();

        const mousePointTo = {
          x: (pointer.x - position.x) / oldScale,
          y: (pointer.y - position.y) / oldScale,
        };

        const direction = e.evt.deltaY > 0 ? -1 : 1;
        const newScale = direction > 0 ? oldScale * scaleBy : oldScale / scaleBy;

        const newPos = {
          x: pointer.x - mousePointTo.x * newScale,
          y: pointer.y - mousePointTo.y * newScale,
        };

        setScale(newScale);
        setPosition(newPos);
      }}
      scaleX={scale}
      scaleY={scale}
      x={position.x}
      y={position.y}
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        background: "#f8f8f8",
        overflow: "hidden",
        cursor: "grab",
      }}
    >
      <Layer>
        {gridLines()}
        {vehiculosActuales.map((v) => {
          const avance = progresoVehiculos[v.idVehiculo] ?? 0;

          return (
            <VehicleIcon
              key={v.idVehiculo}
              ref={(node) => {
                vehicleRefs.current[v.idVehiculo] = node;
              }}
              vehiculo={v}
              cellSize={CELL_SIZE}
              gridHeight={GRID_HEIGHT}
              duration={31250}
              recorridoHastaAhora={avance}
            />
          );
        })}
        {pedidos.map((v) => (
          <OrderIcon
            key={v.idPedido}
            pedido={v}
            cellSize={CELL_SIZE}
            gridHeight={GRID_HEIGHT}
          />
        ))}

        {almacenes.map((v) => (
          <WarehouseIcon
            key={v.idAlmacen}
            almacen={v}
            cellSize={CELL_SIZE}
            gridHeight={GRID_HEIGHT}
          />
        ))}

        {bloqueosVisibles.map((bloqueo) => {
          const puntos = bloqueo.segmentos
            .map((p) => [
              p.posX * CELL_SIZE,
              (GRID_HEIGHT - p.posY) * CELL_SIZE,
            ])
            .flat();

          return (
            <Line
              key={`bloqueo-${bloqueo.idBloqueo}`}
              points={puntos}
              stroke="black"
              strokeWidth={6}
              lineCap="round"
              lineJoin="round"
            />
          );
        })}
      </Layer>
    </Stage>
  );
  

};