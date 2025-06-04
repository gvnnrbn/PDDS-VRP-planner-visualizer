import { useRef, useState } from "react";
import { Stage, Layer, Line } from "react-konva";
import { VehicleIcon } from "./Icons/VehicleIcon";
import type {VehiculoSimulado} from "../../core/types/vehiculo";
import type { PedidoSimulado } from "../../core/types/pedido";
import { OrderIcon} from "./Icons/OrderIcon"
import type { AlmacenSimulado } from "../../core/types/almacen";
import type { BloqueoSimulado } from "../../core/types/bloqueos";
import { WarehouseIcon } from "./Icons/WarehouseIcon";
import React from "react";
import { VehicleRouteLine } from "./Icons/VehicleRouteLine";
import type Konva from "konva";

const CELL_SIZE = 20; // Tamaño de cada celda
const GRID_WIDTH = 70; // Número de celdas a lo ancho
const GRID_HEIGHT = 50; // Número de celdas a lo alto

interface MinutoSimulacion {
  minuto: number;
  vehiculos: VehiculoSimulado[];
  pedidos: PedidoSimulado[];
  almacenes: AlmacenSimulado[];
}

interface SimulacionJson {
  fechaInicio: string;
  simulacion: MinutoSimulacion[];
  bloqueos: BloqueoSimulado[];
}

interface MapGridProps {
  minuto: number;
  data: any;
}

function calcularAvanceEnRuta(v: VehiculoSimulado): number {
  if (!v.rutaActual || v.rutaActual.length === 0) return 0;

  const actualIndex = v.rutaActual.findIndex(
      (p) => p.posX === v.posicionX && p.posY === v.posicionY
  );

  return Math.max(actualIndex, 0);
}


export const MapGrid: React.FC<MapGridProps> = ({ minuto, data }) => {
    if (minuto < 0) return null;

    const stageRef = useRef<any>(null);
    const [scale, setScale] = useState(1);
    const [position, setPosition] = useState({ x: 0, y: 0 });

    const minutoActual = data.simulacion?.find((m) => m.minuto === minuto);
    const minutoSiguiente = data.simulacion?.find((m) => m.minuto === minuto + 1);

    const vehiculosActuales = minutoActual?.vehiculos || [];
    const vehiculosSiguientes = minutoSiguiente?.vehiculos || [];

    const pedidos = minutoActual?.pedidos || [];
    const almacenes = minutoActual?.almacenes || [];

    const vehicleRefs = useRef<Record<number, Konva.Image | null>>({});

    const fechaSimulacionInicio = new Date(data.fechaInicio);
    const fechaActual = new Date(fechaSimulacionInicio);
    fechaActual.setDate(fechaSimulacionInicio.getDate() + minuto);
    //Bloqueos activos
    const bloqueosVisibles = data.bloqueos?.filter((b) => {
      const inicio = new Date(b.fechaInicio);
      const fin = new Date(b.fechaFin);
      return fechaActual >= inicio && fechaActual <= fin;
    }) || [];

    // Funciones de animación

    //const almacenes = minutoData?.almacenes || [];

    // Manejo de zoom
    const handleWheel = (e: any) => {
      e.evt.preventDefault();
      const scaleBy = 1.05;
      const stage = stageRef.current;

      const oldScale = stage.scaleX();
      const pointer = stage.getPointerPosition();

      // Punto relativo al stage antes de escalar
      const mousePointTo = {
        x: (pointer.x - stage.x()) / oldScale,
        y: (pointer.y - stage.y()) / oldScale,
      };

      const direction = e.evt.deltaY > 0 ? -1 : 1;
      const newScale = direction > 0 ? oldScale * scaleBy : oldScale / scaleBy;

      // Aplica nueva escala
      stage.scale({ x: newScale, y: newScale });

      // Calcula nueva posición para mantener el puntero "fijo"
      const newPos = {
        x: pointer.x - mousePointTo.x * newScale,
        y: pointer.y - mousePointTo.y * newScale,
      };

      stage.position(newPos);
      stage.batchDraw();

      setScale(newScale);
      setPosition(newPos);
    };

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

  return (
    <Stage
      width={window.innerWidth}
      height={window.innerHeight}
      draggable
      ref={stageRef}
      onWheel={handleWheel}
      scaleX={scale}
      scaleY={scale}
      x={position.x}
      y={position.y}
      style={{ 
        position: "absolute",
        top: 0,
        left: 0,
        background: "#f8f8f8", // Color de fondo del contenedor
        overflow: "hidden",
        cursor: "grab" }}
    >
      <Layer>
        {gridLines()}
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
        {vehiculosActuales.map((v) => {
          const next = vehiculosSiguientes.find((n) => n.idVehiculo === v.idVehiculo);
          const avance = calcularAvanceEnRuta(v);

          return (
            <React.Fragment key={v.idVehiculo}>
              <VehicleRouteLine
                vehiculo={v}
                shapeRef={vehicleRefs.current[v.idVehiculo]}
                cellSize={CELL_SIZE}
                gridHeight={GRID_HEIGHT}
                recorridoHastaAhora={avance}
              />
              <VehicleIcon
                ref={(node) => { vehicleRefs.current[v.idVehiculo] = node }}
                vehiculo={v}
                nextVehiculo={next}
                cellSize={CELL_SIZE}
                gridHeight={GRID_HEIGHT}
                duration={1000}
              />
            </React.Fragment>
          );
        })}
        {bloqueosVisibles.map((bloqueo) => {
          const puntos = bloqueo.segmentos.map(p => [
            p.posX * CELL_SIZE,
            (GRID_HEIGHT - p.posY) * CELL_SIZE
          ]).flat();

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

        {/*  */}
      </Layer>
    </Stage>
  );
};