import { faLocationDot } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import type { PedidoSimulado } from "../../../core/types/pedido";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useState } from "react";
import { useSimulacion } from "../SimulacionContext";

// Inyecta CSS para usar FontAwesome en runtime
library.add(faLocationDot);

interface Props {
  pedido: PedidoSimulado;
  cellSize: number;
  gridHeight: number;
}

export const OrderIcon: React.FC<Props> = ({ pedido, cellSize, gridHeight }) => {
  const { pedidos } = useSimulacion();
  const pedidoActual = pedidos.find(p => p.idPedido === pedido.idPedido);

  if (!pedidoActual || (pedidoActual.glp ?? 1) <= 0) return null;

  const { posX, posY } = pedidoActual;
  const x = posX * cellSize;
  const y = (gridHeight - posY) * cellSize;

  const [iconUrl, setIconUrl] = useState<string | null>(null);
  const [image] = useImage(iconUrl || "");

  useEffect(() => {
    const svg = faLocationDot.icon[4];
    const fullSvg = `
      <svg width="40" height="40" viewBox="0 0 ${faLocationDot.icon[0]} ${faLocationDot.icon[1]}" xmlns="http://www.w3.org/2000/svg">
        <path d="${svg}" fill="red"/>
      </svg>
    `;
    setIconUrl("data:image/svg+xml;base64," + btoa(fullSvg));
  }, []);
  // Verificar si el pedido aún existe
  const pedidoExistente = pedidos.find((p) => p.idPedido === pedido.idPedido);
  if (!pedidoExistente) return null;

  if (!image) return null;

  return (
    <KonvaImage
      x={x - cellSize / 2}
      y={y - cellSize / 2}
      width={cellSize}
      height={cellSize}
      image={image}
      offsetY={cellSize / 2}
    />
  );
};