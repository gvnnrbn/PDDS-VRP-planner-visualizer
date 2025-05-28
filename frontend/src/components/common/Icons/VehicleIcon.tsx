import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import Konva from "konva";
import useImage from "use-image";
import { useEffect, useRef } from "react";

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

interface Props {
  vehiculo: VehiculoSimulado;
  nextVehiculo?: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
  duration: number
}

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

export const VehicleIcon: React.FC<Props> = ({ vehiculo, nextVehiculo, cellSize, gridHeight, duration }) => {
  const [image] = useImage(createIconUrl(faTruck, getColorFromState(vehiculo.estado)));
  const shapeRef = useRef<any>(null);

  const fromX = vehiculo.posicionX * cellSize;
  const fromY = (gridHeight - vehiculo.posicionY) * cellSize;

  const toX = (nextVehiculo?.posicionX ?? vehiculo.posicionX) * cellSize;
  const toY = (gridHeight - (nextVehiculo?.posicionY ?? vehiculo.posicionY)) * cellSize;

  useEffect(() => {
    if (shapeRef.current && (fromX !== toX || fromY !== toY)) {
      shapeRef.current.to({
        x: toX - cellSize / 2,
        y: toY - cellSize / 2,
        duration: duration / 1000,
        easing: Konva.Easings.EaseInOut,
      });
    }
  }, [toX, toY]);

  if (!image) return null;

  return (
    <KonvaImage
      ref={shapeRef}
      image={image}
      x={fromX - cellSize / 2}
      y={fromY - cellSize / 2}
      width={cellSize}
      height={cellSize}
    />
  );
};