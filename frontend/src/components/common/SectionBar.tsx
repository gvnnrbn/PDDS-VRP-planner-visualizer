import { Box, Button, Flex, Tab, TabList, TabPanel, TabPanels, Tabs, useColorModeValue } from '@chakra-ui/react'

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
  const selectedColor = useColorModeValue('#E5E5E5', '#1a1a1a')
  const notSelectedColor = useColorModeValue('#BDBDBD', '#BDBDBD')
  const bg = useColorModeValue('#white', 'gray.300')

  return (
    <Flex
      width={isCollapsed ? '50px' : '30%'}
      transition="width 0.3s ease"
      // bg={selectedColor}
      direction="column"
      h="full"
    >
      <Tabs 
        isFitted 
        variant='enclosed' 
        orientation='vertical' 
        // colorScheme='purple' 
        bg={'red'}
        
        h='full' 
      >
        <TabList 
          aria-orientation='vertical'
          // borderRight='1px solid'
          // notSelectedColor='#3E4990'
          paddingY={2}
          sx={{ gap: '3px' }}
        >
          {sections.map((section) => (
            <Tab
            key={section.title}
            height="120px"
            minW="unset"
            // borderRadius="lg"
            bg={currentSection === section.title ? selectedColor : notSelectedColor}
            borderTopRightRadius='10px'
            borderBottomRightRadius='10px'
            borderTopLeftRadius='0'
            _selected={{
                bg: {selectedColor},
                color: 'black',
                // borderRight: '2px solid',
                // zIndex: 1,
            }}
            _hover={{
              bg: '#3E4990',
              color: 'white',
            }}
            sx={{
              writingMode: 'vertical-rl',
              transform: 'rotate(180deg)',
              textAlign: 'center',
              // borderRadius: 'md',
              transition: 'all 0.2s',
              padding: '7px'
            }}
            >
              {section.title}
            </Tab>
          ))}
        </TabList>
        <TabPanels bg={selectedColor}>
          {sections.map((section) => (
            <TabPanel
              key={section.title}
            >
              {section.content}
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
      {/* <Button
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

            <Box p={4} bg={useColorModeValue('white', 'gray.800')} borderRadius="md" mt={2} flex={1}>
              {sections.find(s => s.title === currentSection)?.content}
            </Box>
          </Flex>
        </Box>
      )} */}
    </Flex>
  )
}
