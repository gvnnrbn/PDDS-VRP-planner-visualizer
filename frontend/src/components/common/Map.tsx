import { useEffect, useRef, useState } from "react";
import { Stage, Layer, Line, Label, Tag } from "react-konva";
import { VehicleIcon } from "./Icons/VehicleIcon";
import type {VehiculoSimulado} from "../../core/types/vehiculo";
import type { PedidoSimulado } from "../../core/types/pedido";
import type { IncidenciaSimulado } from "../../core/types/incidencia";
import { OrderIcon} from "./Icons/OrderIcon"
import type { AlmacenSimulado } from "../../core/types/almacen";
import type { BloqueoSimulado } from "../../core/types/bloqueos";
import { WarehouseIcon } from "./Icons/WarehouseIcon";
import React from "react";
import { VehicleRouteLine } from "./Icons/VehicleRouteLine";
import type Konva from "konva";
import { Text } from "react-konva";

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

    const minutoActual = data.simulacion.find((m) => m.minuto === minuto);

    const vehiculosActuales = minutoActual?.vehiculos || [];

    const pedidos = minutoActual?.pedidos || [];
    const almacenes = minutoActual?.almacenes || [];

    const vehicleRefs = useRef<Record<number, Konva.Image | null>>({});
    const [progresoVehiculos, setProgresoVehiculos] = useState<Record<number, number>>({});
    const [vehiculoSeleccionadoId, setVehiculoSeleccionadoId] = useState<number | null>(null);
    const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });

    const [tooltipVehiculo, setTooltipVehiculo] = useState<{
      id: number;
      x: number;
      y: number;
    } | null>(null);

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

  const handleVehicleStep = (idVehiculo: number, index: number) => {
    setProgresoVehiculos((prev) => ({
      ...prev,
      [idVehiculo]: index,
    }));

    const shape = vehicleRefs.current[idVehiculo];
    if (shape) {
      const iconX = shape.x();
      const iconY = shape.y();
      const tooltipOffset = 10;

      setTooltipPosition({
        x: iconX + CELL_SIZE / 2,
        y: iconY - tooltipOffset,
      });
    }
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      const clickedOnStage = stageRef.current?.getStage().getIntersection({
        x: e.clientX,
        y: e.clientY,
      });

      // Si no hay intersección con elementos Konva (es decir, clic en el fondo o vacío)
      if (!clickedOnStage) {
        setVehiculoSeleccionadoId(null);
      }
    };

    window.addEventListener("click", handleClickOutside);
    return () => {
      window.removeEventListener("click", handleClickOutside);
    };
  }, []);



  return (
    <Stage
      width={window.innerWidth}
      height={window.innerHeight}
      draggable
      ref={stageRef}
      onWheel={(e) => {
        e.evt.preventDefault();
        const scaleBy = 1.05;
        const stage = stageRef.current;

        const oldScale = stage.scaleX();
        const pointer = stage.getPointerPosition();

        const mousePointTo = {
          x: (pointer.x - stage.x()) / oldScale,
          y: (pointer.y - stage.y()) / oldScale,
        };

        const direction = e.evt.deltaY > 0 ? -1 : 1;
        const newScale = direction > 0 ? oldScale * scaleBy : oldScale / scaleBy;

        stage.scale({ x: newScale, y: newScale });

        const newPos = {
          x: pointer.x - mousePointTo.x * newScale,
          y: pointer.y - mousePointTo.y * newScale,
        };

        stage.position(newPos);
        stage.batchDraw();

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
            <React.Fragment key={v.idVehiculo}>
              <VehicleRouteLine
                vehiculo={v}
                cellSize={CELL_SIZE}
                gridHeight={GRID_HEIGHT}
                recorridoHastaAhora={avance}
              />
              <VehicleIcon
                ref={(node) => {
                  vehicleRefs.current[v.idVehiculo] = node;
                }}
                vehiculo={v}
                cellSize={CELL_SIZE}
                gridHeight={GRID_HEIGHT}
                duration={31250}
                onStep={() => {
                  const shape = vehicleRefs.current[v.idVehiculo];
                  if (shape) {
                    setTooltipVehiculo((prev) =>
                      prev?.id === v.idVehiculo
                        ? {
                            id: v.idVehiculo,
                            x: shape.x() + CELL_SIZE / 2,
                            y: shape.y() - 10,
                          }
                        : prev
                    );
                  }
                }}
                onClick={() => {
                  const shape = vehicleRefs.current[v.idVehiculo];
                  if (!shape) return;

                  const x = shape.x() + CELL_SIZE / 2;
                  const y = shape.y() - 10;

                  setTooltipVehiculo((prev) =>
                    prev?.id === v.idVehiculo ? null : { id: v.idVehiculo, x, y }
                  );
                }}
              />
            </React.Fragment>
          );
        })}

        {/* 🔽 TOOLTIP VEHÍCULO */}
        {tooltipVehiculo && (() => {
          const v = vehiculosActuales.find(
            (veh) => veh.idVehiculo === tooltipVehiculo.id
          );
          console.log(`ToolTip ID ${tooltipVehiculo.id} CoordenadasToolTip ${tooltipVehiculo.x} + ${tooltipVehiculo.y}`);
          if (!v) return null;

          return (
            <Label
              x={tooltipVehiculo.x}
              y={tooltipVehiculo.y}
              listening={false}
            >
              <Tag
                fill="white"
                stroke="black"
                cornerRadius={4}
                pointerDirection="down"
                pointerWidth={10}
                pointerHeight={8}
                shadowColor="black"
                shadowBlur={4}
                shadowOffset={{ x: 2, y: 2 }}
                shadowOpacity={0.2}
              />
              <Text
                text={`🚛 ${v.placa}\n📦 Pedido: ${
                  v.rutaActual?.[0]?.idPedido ?? "N/A"
                }`}
                fontSize={12}
                fill="black"
                padding={5}
                align="center"
              />
            </Label>
          );
        })()}

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