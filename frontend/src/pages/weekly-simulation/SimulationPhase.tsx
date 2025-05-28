import { MapGrid } from '../../components/common/Map'
import { useState, useEffect } from 'react';
import jsonData from "../../data/simulacion.json";

export default function SimulationPhase() {
  const [minuto, setMinuto] = useState(0);

  // Parámetro de velocidad: milisegundos por minuto de simulación
  const speed = 5000;

  useEffect(() => {
    const totalMinutos = jsonData.simulacion.length;
    console.log(minuto)

    if (minuto >= totalMinutos - 1) return;
    
    const interval = setTimeout(() => {
      setMinuto((prev) => prev + 1);
    }, speed);

    return () => clearTimeout(interval);
  }, [minuto]);

  return (
    <div>
      <MapGrid minuto={minuto} data={jsonData} />
    </div>
  );
}