import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculoSimulado";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useState } from "react";

// Inyecta CSS para usar FontAwesome en runtime
library.add(faTruck);

const createIconUrl = (icon: any) => {
  const svgString = icon.icon[4]; // el raw SVG path data
  const fullSvg = `
    <svg width="40" height="40" viewBox="0 0 ${icon.icon[0]} ${icon.icon[1]}" xmlns="http://www.w3.org/2000/svg" fill="black">
      <path d="${svgString}" />
    </svg>
  `;
  return "data:image/svg+xml;base64," + btoa(fullSvg);
};

interface Props {
  vehiculo: VehiculoSimulado;
  cellSize: number;
  gridHeight: number;
}

export const VehicleIcon: React.FC<Props> = ({ vehiculo, cellSize, gridHeight }) => {
  const { posicionX, posicionY } = vehiculo;
  const x = posicionX * cellSize;
  const y = (gridHeight - posicionY) * cellSize;

  const [iconUrl, setIconUrl] = useState<string | null>(null);
  const [image] = useImage(iconUrl || "");

  useEffect(() => {
    setIconUrl(createIconUrl(faTruck));
  }, []);

  if (!image) return null;

  return (
    <KonvaImage
      x={x - cellSize / 2}
      y={y - cellSize / 2}
      width={cellSize}
      height={cellSize}
      image={image}
    />
  );
};