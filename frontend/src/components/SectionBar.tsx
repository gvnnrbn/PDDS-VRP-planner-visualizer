import { Box, Button, Flex, useColorModeValue } from '@chakra-ui/react'

interface Section {
  title: string
  content: React.ReactNode
}

interface SectionBarProps {
  sections: Section[]
  onSectionChange: (section: string) => void
  currentSection: string
  isCollapsed: boolean
  onToggleCollapse: () => void
}

export const SectionBar = ({ 
  sections, 
  onSectionChange, 
  currentSection,
  isCollapsed,
  onToggleCollapse 
}: SectionBarProps) => {
  const bgColor = useColorModeValue('#f3f4f6', '#1a1a1a')
  const borderColor = useColorModeValue('gray.200', 'gray.700')

  return (
    <Flex
      width={isCollapsed ? '50px' : '25%'}
      transition="width 0.3s ease"
      bg={bgColor}
      borderLeft="1px"
      borderColor={borderColor}
      direction="column"
      h="full"
    >
      <Button
        size="sm"
        w="full"
        h="40px"
        onClick={onToggleCollapse}
        bg="transparent"
        _hover={{ bg: useColorModeValue('gray.100', 'gray.800') }}
        display="flex"
        alignItems="center"
        justifyContent="center"
        fontSize="lg"
        fontWeight="bold"
      >
        {isCollapsed ? '←' : '→'}
      </Button>

      {!isCollapsed && (
        <Box p={2} flex={1} overflowY="auto">
          <Flex direction="column" gap={2} h="full">
            {/* Sections */}
            <Box
              overflowX="auto"
              sx={{
                '&::-webkit-scrollbar': {
                  display: 'none',
                },
                scrollbarWidth: 'none',
                msOverflowStyle: 'none',
              }}
            >
              <Flex
                p={1}
                gap={1}
                minW="max-content"
              >
                {sections.map((section) => (
                  <Button
                    key={section.title}
                    variant="outline"
                    onClick={() => onSectionChange(section.title)}
                    colorScheme={currentSection === section.title ? 'blue' : undefined}
                    whiteSpace="nowrap"
                    minW="120px"
                  >
                    {section.title}
                  </Button>
                ))}
              </Flex>
            </Box>

            {/* Content */}
            <Box p={4} bg={useColorModeValue('white', 'gray.800')} borderRadius="md" mt={2} flex={1}>
              {sections.find(s => s.title === currentSection)?.content}
            </Box>
          </Flex>
        </Box>
      )}
    </Flex>
  )
}
