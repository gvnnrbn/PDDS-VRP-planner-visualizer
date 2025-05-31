import { Arrow } from "react-konva";
import React from "react";
import type { VehiculoSimulado } from "../../../core/types/vehiculo";
import type Konva from "konva";

interface Props {
  vehiculo: VehiculoSimulado;
  shapeRef?: Konva.Image | null;
  cellSize: number;
  gridHeight: number;
  recorridoHastaAhora: number;
}

export const VehicleRouteLine: React.FC<Props> = ({
  vehiculo,
  shapeRef,
  cellSize,
  gridHeight,
}) => {
  const ruta = vehiculo.rutaActual ?? [];

  // Si shapeRef es válido, usamos su posición interpolada actual
  const currentX = shapeRef?.x()
    ? Math.round(shapeRef.x() / cellSize) * cellSize
    : vehiculo.posicionX * cellSize;

  const currentY = shapeRef?.y()
    ? Math.round(shapeRef.y() / cellSize) * cellSize
    : (gridHeight - vehiculo.posicionY) * cellSize;

  const puntosRuta = [
    [currentX, currentY], // posición exacta actual del vehículo
    ...ruta.map((p) => [p.posX * cellSize, (gridHeight - p.posY) * cellSize]),
  ];

  const flattened = puntosRuta.flat();

  if (flattened.length < 4) return null;

  return (
    <Arrow
      points={flattened}
      stroke="blue"
      strokeWidth={3}
      pointerLength={10}
      pointerWidth={10}
      fill="blue"
      lineCap="round"
    />
  );
};