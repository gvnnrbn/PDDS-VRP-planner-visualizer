import { faLocationDot } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import type { PedidoSimulado } from "../../../core/types/pedido";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useState } from "react";

// Inyecta CSS para usar FontAwesome en runtime
library.add(faLocationDot);

const createIconUrl = (icon: any, color: string = "red") => {
  const svgPath = icon.icon[4];
  const width = icon.icon[0];
  const height = icon.icon[1];

  const fullSvg = `
    <svg width="40" height="40" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
      <path d="${svgPath}" fill="${color}" />
    </svg>
  `;

  return "data:image/svg+xml;base64," + btoa(fullSvg);
};

interface Props {
  pedido: PedidoSimulado;
  cellSize: number;
  gridHeight: number;
}

export const OrderIcon: React.FC<Props> = ({ pedido, cellSize, gridHeight }) => {
  const { posX, posY } = pedido;
  const x = posX * cellSize;
  const y = (gridHeight - posY) * cellSize;

  const [iconUrl, setIconUrl] = useState<string | null>(null);
  const [image] = useImage(iconUrl || "");

  useEffect(() => {
    setIconUrl(createIconUrl(faLocationDot));
  }, []);

  if (!image) return null;

  return (
    <KonvaImage
      x={x - cellSize / 2}
      y={y - cellSize / 2}
      width={cellSize}
      height={cellSize}
      image={image}
      offsetY={cellSize/2}
    />
  );
};