import React from "react";
import { Box,Flex,Button,Select,Text,useColorModeValue } from "@chakra-ui/react";

type BottomControlsVariant = "date-pause" | "full";

interface BottomLeftControlsProps {
  variant?: BottomControlsVariant;
  date?: string;
  onStop?: () => void;
  speed?: string;
  onSpeedChange?: (value: string) => void;
}

const BottomLeftControls: React.FC<BottomLeftControlsProps> = ({
  variant = "full",
  date = "Día 1 | 02/04/2025 | 13:00",
  onStop,
  speed = "Velocidad",
  onSpeedChange,
}) => {
  const panelBg = useColorModeValue("white", "gray.800");
  const shadow = useColorModeValue("md", "dark-lg");

  const showSpeed = variant === "full";
  const showDate = variant === "full" || variant === "date-pause";
  const showPause = variant === "full" || variant === "date-pause";
  const boxShadow = useColorModeValue("md", "dark-lg");

  return (
    <Flex
        position="absolute"
        bottom="20px"
        left="20px"
        gap={4}
        zIndex={1000}
        >
        {showDate && (
            <Box
                bg={panelBg}
                borderRadius="md"
                px={4}
                py={2}
                boxShadow={boxShadow}
                minW="170px"
                >
                <Text fontSize="sm">{date}</Text>
            </Box>
        )}

        {showPause && (
            <Box
            bg={panelBg}
            borderRadius="md"
            px={4}
            py={2}
            boxShadow={boxShadow}
            display="flex"
            alignItems="center"
            justifyContent="center"
            >
            <Button size="sm" colorScheme="red" onClick={onStop} />
            </Box>
        )}

        {showSpeed && (
            <Box
            bg={panelBg}
            borderRadius="md"
            px={4}
            py={2}
            boxShadow={boxShadow}
            display="flex"
            alignItems="center"
            >
            <Select
                size="sm"
                value={speed}
                onChange={(e) => onSpeedChange?.(e.target.value)}
            >
                <option value="x0.5">x0.5</option>
                <option value="x1">x1</option>
                <option value="x2">x2</option>
                <option value="x4">x4</option>
            </Select>
            </Box>
        )}
        </Flex>
  );
};

export default BottomLeftControls;