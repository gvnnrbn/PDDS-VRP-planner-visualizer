import { MapGrid } from '../../components/common/Map'
import { useState } from 'react';
import jsonData from "../../data/simulacion.json";
import { min } from 'date-fns';

export default function SimulationPhase() {
  const [minuto, setMinuto] = useState(0);

  console.log(minuto)
  return (
    <div>
      <MapGrid minuto={minuto} data={jsonData} />
    </div>
  );
}