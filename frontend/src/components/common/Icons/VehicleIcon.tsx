import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useRef, forwardRef, useState } from "react";
import type Konva from "konva";

// Inyecta ícono
library.add(faTruck);

const createIconUrl = (icon: any, color: string = "red") => {
  const svgString = icon.icon[4];
  const fullSvg = `
    <svg width="40" height="40" viewBox="0 0 ${icon.icon[0]} ${icon.icon[1]}" xmlns="http://www.w3.org/2000/svg">
      <path d="${svgString}" fill="${color}" />
    </svg>
  `;
  return "data:image/svg+xml;base64," + btoa(fullSvg);
};

const getColorFromState = (estado: string): string => {
  switch (estado) {
    case "averiadoA":
      return "red";
    case "REPAIR":
      return "orange";
    case "MuevetePo":
      return "green";
    case "ONTHEWAY":
      return "blue";
    default:
      return "black";
  }
};

interface Props {
  vehiculo: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
  duration: number;
  onStep?: (index: number) => void;
  onClick?: (vehiculo: VehiculoSimulado) => void;
}


export const VehicleIcon = forwardRef<Konva.Image, Props>(
  ({ vehiculo, cellSize, gridHeight, duration, onStep,onClick }, ref) => {
    const [image] = useImage(
      createIconUrl(faTruck, getColorFromState("asdas"))
    );
    const shapeRef = useRef<Konva.Image | null>(null);

    const ruta = vehiculo.rutaActual ?? [];
    const puntosRuta = [
      [vehiculo.posicionX, vehiculo.posicionY],
      ...ruta.map((p) => [p.posX, p.posY]),
    ];

    const [pos, setPos] = useState<[number, number]>([
      vehiculo.posicionX,
      vehiculo.posicionY,
    ]);

    useEffect(() => {
      let index = 0;
      let startTime: number | null = null;
      let frameId: number;

      if (puntosRuta.length < 2) return;

      const stepDuration = duration / (puntosRuta.length - 1);

      const animateStep = (timestamp: number) => {
        if (!startTime) startTime = timestamp;
        const elapsed = timestamp - startTime;

        const t = Math.min(elapsed / stepDuration, 1);
        const [fromX, fromY] = puntosRuta[index];
        const [toX, toY] = puntosRuta[index + 1] || [fromX, fromY];

        const interpX = fromX + (toX - fromX) * t;
        const interpY = fromY + (toY - fromY) * t;

        setPos([interpX, interpY]);
        onStep?.(index);

        if (t < 1) {
          frameId = requestAnimationFrame(animateStep);
        } else {
          // ⚠️ el vehículo llegó al punto index + 1
          index++;

          // ⏹️ Si ya no hay más puntos, terminamos
          if (index >= puntosRuta.length - 1) return;

          const puntoLlegado = ruta[index - 1]; // llegamos a este punto
          const isDescarga = puntoLlegado?.accion === "Descarga";
          const delay = isDescarga ? duration * 0.2 : 0;

          setTimeout(() => {
            startTime = null;
            frameId = requestAnimationFrame(animateStep);
          }, delay);
        }
      };

      frameId = requestAnimationFrame(animateStep);
      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(ruta)]);

    useEffect(() => {
      if (typeof ref === "function") {
        ref(shapeRef.current);
      } else if (ref) {
        (ref as React.MutableRefObject<Konva.Image | null>).current = shapeRef.current;
      }
    }, [ref]);

    if (!image) return null;

    const pixelX = pos[0] * cellSize;
    const pixelY = (gridHeight - pos[1]) * cellSize;

    VehicleIcon.displayName = "VehicleIcon";

    return (
      <KonvaImage
        ref={shapeRef}
        image={image}
        x={pixelX - cellSize / 2}
        y={pixelY - cellSize / 2}
        width={cellSize}
        height={cellSize}
        onClick={() => onClick?.(vehiculo)}
        onTap={() => onClick?.(vehiculo)}
        listening={true} // Asegura que escuche eventos
        hitStrokeWidth={20} // Área de clic expandida
      />
    );
  }
);