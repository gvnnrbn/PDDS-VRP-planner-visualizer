import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useRef, forwardRef, useState } from "react";
import type Konva from "konva";
import { Text, Label, Tag } from "react-konva";

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
}


export const VehicleIcon = forwardRef<Konva.Image, Props>(
  ({ vehiculo, cellSize, gridHeight, duration, onStep }, ref) => {
    const [image] = useImage(createIconUrl(faTruck, getColorFromState("asdas")));
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
    const [isTooltipVisible, setIsTooltipVisible] = useState(false);

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
          index++;
          if (index >= puntosRuta.length - 1) return;

          const puntoLlegado = ruta[index - 1];
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

    return (
      <>
        <KonvaImage
          ref={shapeRef}
          image={image}
          x={pixelX - cellSize / 2}
          y={pixelY - cellSize / 2}
          width={cellSize}
          height={cellSize}
          onClick={() => setIsTooltipVisible((prev) => !prev)}
          onTap={() => setIsTooltipVisible((prev) => !prev)}
          listening={true}
          hitStrokeWidth={20}
        />

        {isTooltipVisible && (
          <Label
            x={pixelX}
            y={pixelY - cellSize / 2 - 10}
            listening={false}
          >
            <Tag
              fill="white"
              stroke="black"
              cornerRadius={4}
              pointerDirection="down"
              pointerWidth={10}
              pointerHeight={8}
              shadowColor="black"
              shadowBlur={4}
              shadowOffset={{ x: 2, y: 2 }}
              shadowOpacity={0.2}
            />
            <Text
              text={`🚛 ${vehiculo.placa}\n📦 Pedido: ${vehiculo.rutaActual?.[0]?.idPedido ?? 'N/A'}`}
              fontSize={12}
              fill="black"
              padding={5}
              align="center"
            />
          </Label>
        )}
      </>
    );
  }
);