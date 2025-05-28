import { useRef, useState } from "react";
import { Stage, Layer, Line } from "react-konva";
import { VehicleIcon } from "./Icons/VehicleIcon";
import type {VehiculoSimulado} from "../../core/types/vehiculoSimulado";
import type { PedidoSimulado } from "../../core/types/pedidoSimulado";
import { OrderIcon} from "./Icons/OrderIcon"

const CELL_SIZE = 20; // Tamaño de cada celda
const GRID_WIDTH = 70; // Número de celdas a lo ancho
const GRID_HEIGHT = 50; // Número de celdas a lo alto

interface MinutoSimulacion {
  minuto: number;
  vehiculos: VehiculoSimulado[];
  pedidos: PedidoSimulado[];
}

interface SimulacionJson {
  simulacion: MinutoSimulacion[];
}

interface MapGridProps {
  minuto: number;
  data: SimulacionJson;
}

export const MapGrid: React.FC<MapGridProps> = ({ minuto, data }) => {
    const stageRef = useRef<any>(null);
    const [scale, setScale] = useState(1);
    const [position, setPosition] = useState({ x: 0, y: 0 });

    const minutoData = data.simulacion.find((m: any) => m.minuto === minuto);
    const vehiculos = minutoData?.vehiculos || [];
    const pedidos = minutoData?.pedidos || [];
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
        {vehiculos.map((v) => (
          <VehicleIcon
            key={v.idVehiculo}
            vehiculo={v}
            cellSize={CELL_SIZE}
            gridHeight={GRID_HEIGHT}
          />
        ))}
        {pedidos.map((v) => (
          <OrderIcon
            key={v.idPedido}
            pedido={v}
            cellSize={CELL_SIZE}
            gridHeight={GRID_HEIGHT}
          />
        ))}
        {/*  */}
      </Layer>
    </Stage>
  );
};