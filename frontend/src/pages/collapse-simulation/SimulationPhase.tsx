
import { MapGrid } from '../../components/common/Map'
import { vehiculosEjemplo } from '../../components/common/Icons/vehiculosEjemplo'

export default function SimulationPhase() {

  return (
    <div>
      <MapGrid vehiculos={vehiculosEjemplo}>
      </MapGrid>
    </div>
      

  )
}