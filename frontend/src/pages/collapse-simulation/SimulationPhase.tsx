
import { MapGrid } from '../../components/common/Map'
import { useState } from 'react';
import jsonData from "../../data/simulacion.json";

export default function SimulationPhase() {
  const [minuto, setMinuto] = useState(0);
  return (
    <div>
      <MapGrid minuto={minuto} data={jsonData} />
    </div>
      

  )
}