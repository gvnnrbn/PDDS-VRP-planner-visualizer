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
    const [currentIndex, setCurrentIndex] = useState(0); // 🆕 índice dinámico

    //PROXIMIDAD
    const estaCerca = (pos1: [number, number], pos2: [number, number], umbral: number = 5): boolean => {
      const [x1, y1] = pos1;
      const [x2, y2] = pos2;
      const distancia = Math.hypot(x2 - x1, y2 - y1);
      return distancia <= umbral;
    };

    

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

        if (t < 1) {
          frameId = requestAnimationFrame(animateStep);
        } else {
          index++;
          if (index >= puntosRuta.length - 1) return;

          const puntoAnterior = ruta[index - 1];
          const { accion, traspasoGLP = 0, idPedido } = puntoAnterior || {};

          const delay = accion === "Descarga" ? duration * 0.2 : 0;

          setTimeout(() => {
          // ⚡ Aplicar efectos DESPUÉS del delay
          if (accion === "Descarga") {
            actualizarPedido(idPedido, traspasoGLP);
            actualizarVehiculo(vehiculo.idVehiculo, (prev) => ({
              ...prev,
              currGLP: Math.max(0, prev.currGLP - traspasoGLP),
            }));
            console.log(`Deposita en pedido ${idPedido} la cantidad de ${traspasoGLP}
              \n GLP del nuevo vehiculo ahora: ${vehiculo.currGLP}` );
          } else if (accion === "Recarga") {
            // Buscar almacén cercano
            const puntoActual = puntosRuta[index];
            if (!puntoActual || puntoActual.length !== 2) return;
            const almacenCercano = almacenes.find(a =>
              a.posicion && estaCerca([a.posicion.posX, a.posicion.posY], puntoActual as [number, number])
            );
            if (almacenCercano) {
              actualizarAlmacen(almacenCercano.idAlmacen, traspasoGLP);
            }
            actualizarVehiculo(vehiculo.idVehiculo, (prev) => ({
              ...prev,
              currGLP: prev.currGLP + traspasoGLP,
            }));
            console.log(`Recarga en almacen ${almacenCercano?.idAlmacen} con GLP ${almacenCercano?.currentGLP}
              \n GLP del nuevo vehiculo ahora: ${vehiculo.currGLP} ` );
          }

          // Continuar con siguiente paso de animación
          startTime = null;
          frameId = requestAnimationFrame(animateStep);
        }, delay);
        }
      };

      frameId = requestAnimationFrame(animateStep);
      return () => cancelAnimationFrame(frameId);
    }, [JSON.stringify(ruta)]);


    //NO

    useEffect(() => {
      if (typeof ref === "function") {
        ref(shapeRef.current);
      } else if (ref) {
        ref.current = shapeRef.current;
      }
    }, [ref]);

    

    const pixelX = pos[0] * cellSize;
    const pixelY = (gridHeight - pos[1]) * cellSize;

    const rutaEnPixeles = puntosRuta.map(([x, y]) => [
      x * cellSize,
      (gridHeight - y) * cellSize,
    ]);
    const rutaRestante = rutaEnPixeles.slice(currentIndex);
    const flattened = rutaRestante.flat();

    useEffect(() => {
      if (shapeRef.current) {
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
              text={`🚛 ${vehiculo.placa}\n📦 Pedido: ${vehiculo.rutaActual?.[0]?.idPedido ?? 'N/A'} \nGLP: ${vehiculo.currGLP}`}
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