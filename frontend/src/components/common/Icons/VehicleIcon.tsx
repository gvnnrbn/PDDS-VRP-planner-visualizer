import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useRef, forwardRef, useState } from "react";
import type Konva from "konva";
import { Text, Label, Tag } from "react-konva";
import { Arrow } from "react-konva";

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
  switch (estado.toUpperCase()) {
    case "AVERIADO":
      return "red";
    case "REPARACION":
      return "orange";
    case "MOVIENDOSE":
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
      ...((vehiculo.rutaActual ?? []).map((p) => [p.posX, p.posY])),
    ];

    const [pos, setPos] = useState<[number, number]>([
      vehiculo.posicionX,
      vehiculo.posicionY,

    ]);


    useEffect(() => {
      const fullRuta: [number, number][] = [
        [vehiculo.posicionX, vehiculo.posicionY],
        ...((vehiculo.rutaActual ?? []).map(p => [p.posX, p.posY] as [number, number]))
      ];

      if (fullRuta.length < 2) return;

      let step = 0;
      let frameId: number;

      const stepDur = duration / (fullRuta.length - 1); // Duración uniforme por tramo

      const animarPaso = (from: [number, number], to: [number, number], onFinish: () => void) => {
        const start = performance.now();

        const animate = (now: number) => {
          const t = Math.min((now - start) / stepDur, 1);
          const interp: [number, number] = [
            from[0] + (to[0] - from[0]) * t,
            from[1] + (to[1] - from[1]) * t,
          ];
          setPos(interp);

          if (shapeRef.current) {
            shapeRef.current.x(interp[0] * cellSize - cellSize / 2);
            shapeRef.current.y((gridHeight - interp[1]) * cellSize - cellSize / 2);
            shapeRef.current.getLayer()?.batchDraw();
          }

          if (t < 1) {
            frameId = requestAnimationFrame(animate);
          } else {
            onFinish();
          }
        };

        frameId = requestAnimationFrame(animate);
      };

      const recorrerRuta = () => {
        if (step >= fullRuta.length - 1) return;

        const from = fullRuta[step];
        const to = fullRuta[step + 1];

        animarPaso(from, to, () => {
          step++;
          recorrerRuta();
        });
      };

      recorrerRuta();

      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(vehiculo.rutaActual), duration]);

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
        {(vehiculo.rutaActual?.length ?? 0) > 0 && (
        <Arrow
          points={[
            vehiculo.posicionX * cellSize,
            (gridHeight - vehiculo.posicionY) * cellSize,
            ...((vehiculo.rutaActual ?? []).flatMap(p => [
              p.posX * cellSize,
              (gridHeight - p.posY) * cellSize
            ]))
          ]}
          stroke="blue"
          strokeWidth={3}
          pointerLength={10}
          pointerWidth={10}
          fill="blue"
          lineCap="round"
          listening={false}
        />
        )}
        <KonvaImage
          ref={shapeRef}
          image={image}
          x={pixelX - cellSize / 2}
          y={pixelY - cellSize / 2}
          width={cellSize}
          height={cellSize}
          onClick={(e) => { e.cancelBubble = true; setIsTooltipVisible((prev) => !prev); }}
          onTap={(e) => { e.cancelBubble = true; setIsTooltipVisible((prev) => !prev); }}
          listening={true}
          hitStrokeWidth={30}
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
              text={`🚛 ${vehiculo.placa}\n📦 Estado: ${vehiculo.estado} \nPedido: ${vehiculo.idPedido} \nCombustible: ${vehiculo.combustible}`}
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