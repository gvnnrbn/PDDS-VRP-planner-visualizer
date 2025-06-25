import { Box, Heading, Text, VStack, useColorModeValue, Image } from "@chakra-ui/react";
import homeImg from '../assets/home.jpg';

export default function Home() {
  const text = useColorModeValue('purple.900', 'white');

  return (
    <Box minH="80vh" display="flex" alignItems="center" justifyContent="center">
      <VStack
        spacing={8}
        p={8}
        borderRadius="xl"
        boxShadow="2xl"
        bg="white"
        maxW="lg"
      >
        <Image
          src={homeImg}
          alt="Bienvenida"
          boxSize="240px"
          objectFit="contain"
          borderRadius="lg"
          boxShadow="lg"
          bg="white"
        />
        <Heading as="h1" size="xl" color={text} textAlign="center">
          ¡Bienvenido a PLG!
        </Heading>
        <Text fontSize="lg" color={text} textAlign="center">
          Optimiza y visualiza la planificación de rutas, pedidos, incidencias, vehículos y almacenes de manera eficiente y profesional. Utiliza el menú lateral para navegar por las diferentes secciones del sistema.
        </Text>
      </VStack>
    </Box>
  );
}