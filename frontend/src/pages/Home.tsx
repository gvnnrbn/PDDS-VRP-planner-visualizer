import { Button, Stack } from "@chakra-ui/react";
import { Link } from "react-router-dom";

export default function Home() {

  return (<>

    <Stack m={2} gap={6}>
    <Link to={'/pedidos'}>
        <Button width={'300px'}variant={'secondary'}>Registrar Pedidos</Button>
    </Link>
    <Link to={'/incidencias'}>
        <Button width={'300px'}variant={'secondary'}>Registrar Incidencias</Button>
    </Link>
    <Link to={'/vehiculos'}>
        <Button width={'300px'}variant={'secondary'}>Registrar Vehículos</Button>
    </Link>
    <Link to={'/almacen'}>
        <Button width={'300px'}variant={'secondary'}>Registrar Almacenes</Button>
    </Link>
    </Stack>
  </>);
}