import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useRef, forwardRef, useState } from "react";
import type Konva from "konva";

// Inyecta CSS para usar FontAwesome en runtime
library.add(faTruck);

const createIconUrl = (icon: any, color: string = "red") => {
  const svgString = icon.icon[4]; // el raw SVG path data
  const fullSvg = `
    <svg width="40" height="40" viewBox="0 0 ${icon.icon[0]} ${icon.icon[1]}" xmlns="http://www.w3.org/2000/svg" fill="black">
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
  nextVehiculo?: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
  duration: number;
  
}

export const VehicleIcon = forwardRef<Konva.Image, Props>(({vehiculo,nextVehiculo,cellSize,gridHeight,duration}, ref) => {
  const [image] = useImage(createIconUrl(faTruck, getColorFromState(vehiculo.estado)));
  const fromX = vehiculo.posicionX * cellSize;
  const fromY = (gridHeight - vehiculo.posicionY) * cellSize;
  const toX = (nextVehiculo?.posicionX ?? vehiculo.posicionX) * cellSize;
  const toY = (gridHeight - (nextVehiculo?.posicionY ?? vehiculo.posicionY)) * cellSize;

  const [progress, setProgress] = useState(0);

  const shapeRef = useRef<Konva.Image | null>(null);

  useEffect(() => {
    let start: number | null = null;
    let frameId: number;

    const animate = (timestamp: number) => {
      if (!start) start = timestamp;
      const elapsed = timestamp - start;
      const t = Math.min(elapsed / duration, 1);
      setProgress(t);

      if (t < 1) {
        frameId = requestAnimationFrame(animate);
      }
    };

    frameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frameId);
  }, [fromX, fromY, toX, toY]);

  // ✅ Asignar ref externo si existe
  useEffect(() => {
    if (typeof ref === "function") {
      ref(shapeRef.current);
    } else if (ref) {
      (ref as React.MutableRefObject<Konva.Image | null>).current = shapeRef.current;
    }
  }, [ref]);

  const x = fromX + (toX - fromX) * progress;
  const y = fromY + (toY - fromY) * progress;

  if (!image) return null;
  VehicleIcon.displayName = "VehicleIcon";
  return (
    <KonvaImage
      ref={shapeRef}
      image={image}
      x={x - cellSize / 2}
      y={y - cellSize / 2}
      width={cellSize}
      height={cellSize}
    />
  );
  
}) 