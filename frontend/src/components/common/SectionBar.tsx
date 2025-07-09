import { Flex, Tab, TabList, TabPanel, TabPanels, Tabs, useColorModeValue } from '@chakra-ui/react'

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
  const currentIndex = sections.findIndex((section) => section.title === currentSection);

  return (
    <Flex
      position="absolute"
      right="0"
      top="0"
      height="100%"
      maxWidth="30%"
      zIndex={2}
      width={isCollapsed ? '40px' : '30%'}
      transition="width 0.3s ease"
      direction="column"
      h="full"
      overflow="hidden"
    >
      <Tabs 
        variant='enclosed' 
        orientation='vertical' 
        h='full' 
        index={currentIndex}
      >
        <TabList 
          aria-orientation='vertical'
          sx={{ gap: '3px' }}
        >
          {sections.map((section) => (
            <Tab
            height='100%'
            key={section.title}
            color='#3E4990'
            onClick={() => {
              if (section.title === currentSection) {
                // Si ya está seleccionado, alterna el colapso
                onToggleCollapse();
              } else {
                // Si es un nuevo tab, cambia de sección y asegúrate de expandir si está colapsado
                onSectionChange(section.title);
                if (isCollapsed) {
                  onToggleCollapse();
                }
              }
            }}
            bg={currentSection === section.title && !isCollapsed ? selectedColor : notSelectedColor}
            fontWeight={currentSection === section.title && !isCollapsed ? 'bold' : 'normal'}
            borderTopRightRadius='10px'
            borderBottomRightRadius='10px'
            borderTopLeftRadius='0'
            _hover={{
              bg: selectedColor,
            }}
            sx={{
              writingMode: 'vertical-rl',
              transform: 'rotate(180deg)',
              textAlign: 'center',
              transition: 'all 0.2s',
              padding: '7px'
            }}
            >
              {section.title}
            </Tab>
          ))}
        </TabList>
        <TabPanels 
          bg={selectedColor}
          overflowY={'auto'}
        >
          {sections.map((section) => (
            <TabPanel
              key={section.title}
            >
              {section.content}
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </Flex>
  )
}
