"use client"

import { ChakraProvider, defaultSystem } from "@chakra-ui/react"
import type { JSX } from "react/jsx-runtime"

export function Provider(props) {
  return (
    <ChakraProvider value={defaultSystem}>
    </ChakraProvider>
  )
}
