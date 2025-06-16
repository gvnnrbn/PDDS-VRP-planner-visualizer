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
  velocidad: number;
  tiempoLimiteMs: number;
  onStep?: (index: number) => void;
}


export const VehicleIcon = forwardRef<Konva.Image, Props>(
  ({ vehiculo, cellSize, gridHeight, velocidad, tiempoLimiteMs, onStep }, ref) => {
    const [image] = useImage(createIconUrl(faTruck, getColorFromState("asdas")));
    const shapeRef = useRef<Konva.Image | null>(null);

    const puntosRuta = [
      [vehiculo.posicionX, vehiculo.posicionY],
      ...((vehiculo.rutaActual ?? []).map((p) => [p.posX, p.posY])),
    ];

    const [pos, setPos] = useState<[number, number]>([
      vehiculo.posicionX,
      vehiculo.posicionY,

    ]);

    const [recorridoHastaAhora, setRecorridoHastaAhora] = useState(0);


    useEffect(() => {
      const fullRuta: [number, number][] = [
        [vehiculo.posicionX, vehiculo.posicionY],
        ...(vehiculo.rutaActual?.map(p => [p.posX, p.posY] as [number, number]) ?? [])
      ];

      if (fullRuta.length < 2) return;

      let step = 0;
      let frameId: number;
      let tiempoTranscurrido = 0;

      const calcularDistancia = ([x1, y1]: [number, number], [x2, y2]: [number, number]) => {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
      };

      const recorrerRuta = () => {
        if (step >= fullRuta.length - 1) return;

        const from = fullRuta[step];
        const to = fullRuta[step + 1];
        const distancia = calcularDistancia(from, to);
        const stepDurMs = (distancia / velocidad) * 1000;

        const start = performance.now();

        const animate = (now: number) => {
          const t = Math.min((now - start) / stepDurMs, 1);
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
            tiempoTranscurrido += stepDurMs;
            if (tiempoTranscurrido < tiempoLimiteMs) {
              step++;
              recorrerRuta();
            }
            // si no, se queda quieto
          }
        };

        frameId = requestAnimationFrame(animate);
      };

      recorrerRuta();

      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(vehiculo.rutaActual), velocidad, tiempoLimiteMs]);

    const [isTooltipVisible, setIsTooltipVisible] = useState(false);

    useEffect(() => {
      if (puntosRuta.length < 2) return;

      let index = 0;
      let frameId: number;
      let tiempoTranscurrido = 0;

      const calcularDistancia = ([x1, y1]: [number, number], [x2, y2]: [number, number]) => {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
      };

      const recorrerTramo = () => {
        if (index >= puntosRuta.length - 1) return;

        const from = puntosRuta[index] as [number, number]; // <--- ¡Corrección aquí!
        const to = puntosRuta[index + 1] as [number, number];   // <--- ¡Y aquí!
        const distancia = calcularDistancia(from, to);
        const duracionTramoMs = (distancia / velocidad) * 1000;

        const start = performance.now();

        const animar = (timestamp: number) => {
          const elapsed = timestamp - start;
          const t = Math.min(elapsed / duracionTramoMs, 1);

          const interpX = from[0] + (to[0] - from[0]) * t;
          const interpY = from[1] + (to[1] - from[1]) * t;
          setPos([interpX, interpY]);

          onStep?.(index);
          setRecorridoHastaAhora(index);

          if (t < 1 && tiempoTranscurrido + elapsed < tiempoLimiteMs) {
            frameId = requestAnimationFrame(animar);
          } else {
            tiempoTranscurrido += duracionTramoMs;
            index++;
            if (tiempoTranscurrido < tiempoLimiteMs && index < puntosRuta.length - 1) {
              recorrerTramo();
            }
            // Si se termina tiempo o ruta, se queda quieto
          }
        };

        frameId = requestAnimationFrame(animar);
      };

      recorrerTramo();

      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(puntosRuta), velocidad, tiempoLimiteMs]);

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


    const puntosRestantes = puntosRuta.slice(recorridoHastaAhora);

    return (
      <>
        {puntosRestantes.length > 0 && (
          <Arrow
            points={puntosRestantes.flatMap(([x, y]) => [
              x * cellSize,
              (gridHeight - y) * cellSize
            ])}
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