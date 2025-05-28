import { faWarehouse } from "@fortawesome/free-solid-svg-icons";
import { faIndustry } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import type { AlmacenSimulado } from "../../../core/types/almacen";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useState } from "react";

// Inyecta CSS para usar FontAwesome en runtime
library.add(faWarehouse);
library.add(faIndustry);

const createIconUrl = (icon: any, color: string = "black") => {
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
  almacen: AlmacenSimulado;
  cellSize: number;
  gridHeight: number;
}

export const WarehouseIcon: React.FC<Props> = ({ almacen, cellSize, gridHeight }) => {
    if (!almacen.posicion) return null;

    const { posX, posY } = almacen.posicion;
    const x = posX * cellSize;
    const y = (gridHeight - posY) * cellSize;

    const [iconUrl, setIconUrl] = useState<string | null>(null);
    const [image] = useImage(iconUrl || "");

    useEffect(() => {
        let icon = faWarehouse;
        let color = "black";

        if (!almacen.isMain) {
        icon = faIndustry;
        if (almacen.currentGLP !== undefined && almacen.maxGLP !== undefined) {
            color = almacen.currentGLP < almacen.maxGLP / 2 ? "red" : "green";
        } else {
            color = "gray";
        }
        }

        setIconUrl(createIconUrl(icon, color));
    }, [almacen]);

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