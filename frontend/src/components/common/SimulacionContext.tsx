import type { PedidoSimulado } from "../../core/types/pedido"; 
import type { AlmacenSimulado } from "../../core/types/almacen"; 
import type { VehiculoSimulado } from "../../core/types/vehiculo";

import React, { createContext, useContext, useState } from 'react';

interface SimulacionContextProps {
  pedidos: PedidoSimulado[];
  almacenes: AlmacenSimulado[];
  vehiculos: VehiculoSimulado[];
  setPedidos: React.Dispatch<React.SetStateAction<PedidoSimulado[]>>;
  setAlmacenes: React.Dispatch<React.SetStateAction<AlmacenSimulado[]>>;
  setVehiculos: React.Dispatch<React.SetStateAction<VehiculoSimulado[]>>;
  actualizarPedido: (idPedido: number, traspasoGLP: number) => void;
  actualizarAlmacen: (idAlmacen: number, traspasoGLP: number) => void;
  actualizarVehiculo: (
  idVehiculo: number,
  updater: (prev: VehiculoSimulado) => VehiculoSimulado
) => void;
}

const SimulacionContext = createContext<SimulacionContextProps | undefined>(undefined);

export const SimulacionProvider: React.FC<{
  initialPedidos: PedidoSimulado[];
  initialAlmacenes: AlmacenSimulado[];
  initialVehiculos: VehiculoSimulado[];
  children: React.ReactNode;
}> = ({ initialPedidos, initialAlmacenes, initialVehiculos, children }) => {
  const [pedidos, setPedidos] = useState<PedidoSimulado[]>(initialPedidos);
  const [almacenes, setAlmacenes] = useState<AlmacenSimulado[]>(initialAlmacenes);
  const [vehiculos, setVehiculos] = useState<VehiculoSimulado[]>(initialVehiculos);

  const actualizarPedido = (idPedido: number, traspasoGLP: number) => {
    setPedidos((prev) =>
      prev
        .map((pedido) =>
          pedido.idPedido === idPedido
            ? { ...pedido, glp: Math.max(0, (pedido.glp ?? 0) - traspasoGLP) }
            : pedido
        )
        .filter((pedido) => (pedido.glp ?? 0) > 0)
    );
  };

  const actualizarAlmacen = (idAlmacen: number, traspasoGLP: number) => {
    setAlmacenes((prev) =>
      prev.map((almacen) =>
        almacen.idAlmacen === idAlmacen
          ? {
              ...almacen,
              currentGLP: Math.min(
                (almacen.currentGLP ?? 0) - traspasoGLP,
                almacen.maxGLP ?? Infinity
              ),
            }
          : almacen
      )
    );
  };

  const actualizarVehiculo = (
    idVehiculo: number,
    updater: (prev: VehiculoSimulado) => VehiculoSimulado
    ) => {
    setVehiculos((prev) =>
        prev.map((vehiculo) =>
        vehiculo.idVehiculo === idVehiculo
            ? updater(vehiculo)
            : vehiculo
        )
    );
    };

    return (
    <SimulacionContext.Provider
      value={{
        pedidos,
        almacenes,
        vehiculos,
        setPedidos,
        setAlmacenes,
        setVehiculos,
        actualizarPedido,
        actualizarAlmacen,
        actualizarVehiculo,
      }}
    >
      {children}
    </SimulacionContext.Provider>
  );
};

export const useSimulacion = () => {
  const context = useContext(SimulacionContext);
  if (!context) {
    throw new Error('useSimulacion debe usarse dentro de SimulacionProvider');
  }
  return context;
};