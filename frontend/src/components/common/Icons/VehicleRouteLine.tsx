import { Arrow } from "react-konva";
import React from "react";
import type { VehiculoSimulado } from "../../../core/types/vehiculo";

interface Props {
  vehiculo: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
  recorridoHastaAhora: number; // índice de avance en la ruta
}

export const VehicleRouteLine: React.FC<Props> = ({
  vehiculo,
  cellSize,
  gridHeight,
  recorridoHastaAhora,
}) => {
  const ruta = vehiculo.rutaActual ?? [];

  // Incluye posición actual como primer punto
  const puntosRuta = [
    [vehiculo.posicionX, vehiculo.posicionY],
    ...ruta.map((p) => [p.posX, p.posY]),
  ].map(([x, y]) => [x * cellSize, (gridHeight - y) * cellSize]);

  // Recorta la línea desde el punto actual
  const puntosRecortados = puntosRuta.slice(recorridoHastaAhora);
  const flattened = puntosRecortados.flat();

  // Se necesitan al menos 2 puntos para trazar
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