import { Flex, Spinner, useColorModeValue } from '@chakra-ui/react';
import React from 'react';

interface LoadingOverlayProps {
  isVisible: boolean;
}

const LoadingOverlay: React.FC<LoadingOverlayProps> = ({ isVisible }) => {
  if (!isVisible) return null;

  const bgColor = useColorModeValue('rgba(255,255,255,0.85)', 'rgba(0,0,0,0.85)');

  return (
    <Flex
      position="fixed"
      top={0}
      left={0}
      w="100vw"
      h="100vh"
      bg={bgColor}
      align="center"
      justify="center"
      zIndex={2000}
    >
      <Spinner size="xl" thickness="4px" speed="0.65s" color="purple.500" />
    </Flex>
  );
};

export default LoadingOverlay;