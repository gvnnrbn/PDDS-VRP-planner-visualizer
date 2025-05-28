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

export const VehicleRouteLine: React.FC<Props> = ({vehiculo,shapeRef,cellSize,gridHeight,recorridoHastaAhora}) => {
    const ruta = vehiculo.rutaActual ?? [];

    // Incluye posición actual como primer punto
    const currentX = shapeRef?.x() ?? vehiculo.posicionX * cellSize;
    const currentY = shapeRef?.y() ?? (gridHeight - vehiculo.posicionY) * cellSize;


    const puntosRuta = [
    [currentX, currentY],
    ...ruta.map((p) => [p.posX * cellSize, (gridHeight - p.posY) * cellSize]),
    ];

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