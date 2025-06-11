import { faTruck } from "@fortawesome/free-solid-svg-icons";
import { library } from "@fortawesome/fontawesome-svg-core";
import  type { VehiculoSimulado} from "../../../core/types/vehiculo";
import { Image as KonvaImage } from "react-konva";
import useImage from "use-image";
import { useEffect, useRef, forwardRef, useState } from "react";
import type Konva from "konva";
import { Arrow, Text, Label, Tag } from "react-konva";
import { useSimulacion } from "../SimulacionContext";

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
    case "Funcional":
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
  recorridoHastaAhora: number;
}


export const VehicleIcon = forwardRef<Konva.Image, Props>(
  ({ vehiculo, cellSize, gridHeight, duration }, ref) => {
    const { actualizarVehiculo } = useSimulacion();
    const { actualizarPedido} = useSimulacion();
    const {actualizarAlmacen} = useSimulacion();
    const { almacenes } = useSimulacion();
    const [image] = useImage(createIconUrl(faTruck, getColorFromState("asdas")));
    const shapeRef = useRef<Konva.Image | null>(null);

    const [pos, setPos] = useState<[number, number]>([
      vehiculo.posicionX,
      vehiculo.posicionY,
    ]);

    

    const [isTooltipVisible, setIsTooltipVisible] = useState(false);

    //PROXIMIDAD
    const fullRuta: [number, number][] = [[vehiculo.posicionX, vehiculo.posicionY]];

    vehiculo.rutaActual?.forEach(bloque => {
      bloque.puntos.forEach(p => {
        fullRuta.push([p.posX, p.posY]);
      });
    });
    

    // Animación de punto a punto
    useEffect(() => {
      // Construcción correcta de la ruta completa punto a punto
      const ruta: [number, number][] = [[vehiculo.posicionX, vehiculo.posicionY]];
      vehiculo.rutaActual?.forEach(bloque => {
        bloque.puntos.forEach(p => {
          ruta.push([p.posX, p.posY]);
        });
      });

      if (ruta.length < 2) return;
      console.log('Full ruta:', fullRuta);
      let step = 0;
      let frameId: number;

      const stepDur = duration / (ruta.length - 1); // distribuir duración equitativamente

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
        if (step >= ruta.length - 1) return;

        const from = ruta[step];
        const to = ruta[step + 1];

        animarPaso(from, to, () => {
          step++;
          recorrerRuta();
        });
      };

      recorrerRuta();

      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(vehiculo.rutaActual)]);


    //RUTA
    const rutaEnPixeles = fullRuta.map(([x, y]) => [
      x * cellSize,
      (gridHeight - y) * cellSize,
    ]);
    const flattened = rutaEnPixeles.flat();

    useEffect(() => {
      if (typeof ref === "function") {
        ref(shapeRef.current);
      } else if (ref) {
        ref.current = shapeRef.current;
      }
    }, [ref]);

    

    const pixelX = pos[0] * cellSize;
    const pixelY = (gridHeight - pos[1]) * cellSize;


    useEffect(() => {
      if (image && shapeRef.current) {
        shapeRef.current.cache();
        shapeRef.current.drawHitFromCache();
      }
    }, [image]);

    if (!image) return null;
    

    return (
      <>
        {flattened.length >= 4 && (
          <Arrow
            points={flattened}
            stroke="blue"
            strokeWidth={3}
            pointerLength={10}
            pointerWidth={10}
            fill="blue"
            lineCap="round"
          />
        )}

        {isTooltipVisible && (
          <Label x={pixelX} y={pixelY - cellSize / 2 - 10} listening={false}>
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
              text={`🚛 ${vehiculo.placa}\n📦 Pedido: ${vehiculo.rutaActual?.[0]?.idEntidad ?? 'N/A'} \nGLP: ${vehiculo.currGLP}`}
              fontSize={12}
              fill="black"
              padding={5}
              align="center"
            />
          </Label>
        )}

        <KonvaImage
          ref={shapeRef}
          image={image}
          x={pixelX - cellSize / 2}
          y={pixelY - cellSize / 2}
          width={cellSize}
          height={cellSize}
          onClick={(e) => {
            console.log('SE CLICKEO')
            e.cancelBubble = true;
            setIsTooltipVisible((prev) => !prev);
          }}
          onTap={() => setIsTooltipVisible((prev) => !prev)}
          listening={true}
          hitStrokeWidth={20}
        />  
      </>
    );
  }
);