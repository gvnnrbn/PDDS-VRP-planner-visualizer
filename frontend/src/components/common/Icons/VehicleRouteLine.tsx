import { Arrow } from "react-konva";
import React, { useMemo } from "react";
import type { VehiculoSimulado } from "../../../core/types/vehiculo";


interface Props {
  vehiculo: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
  recorridoHastaAhora: number; // índice desde el cual empieza la ruta
}

export const VehicleRouteLine: React.FC<Props> = ({
  vehiculo,
  cellSize,
  gridHeight,
  recorridoHastaAhora,
}) => {
  const ruta = vehiculo.rutaActual ?? [];

  const puntosRuta = useMemo(() => [
    [vehiculo.posicionX * cellSize, (gridHeight - vehiculo.posicionY) * cellSize],
    ...ruta.map((p) => [p.posX * cellSize, (gridHeight - p.posY) * cellSize]),
  ], [vehiculo, cellSize, gridHeight]);
  // ⚡ Elimina los puntos que ya fueron recorridos
  const puntosRestantes = puntosRuta.slice(recorridoHastaAhora);

  if (puntosRestantes.length < 2) return null;

  const flattened = puntosRestantes.flat();

  return (
    <Arrow
      points={flattened}
      stroke="blue"
      strokeWidth={3}
      pointerLength={10}
      pointerWidth={10}
      fill="blue"
      lineCap="round"
      listening={false}
    />
  );
};