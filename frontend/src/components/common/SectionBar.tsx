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

  return (
    <Flex
      width={isCollapsed ? '50px' : '30%'}
      transition="width 0.3s ease"
      direction="column"
      h="full"
    >
      <Tabs 
        variant='enclosed' 
        orientation='vertical' 
        h='full' 
      >
        <TabList 
          aria-orientation='vertical'
          sx={{ gap: '3px' }}
        >
          <Tab
          onClick={onToggleCollapse}
            bg={notSelectedColor}
            borderLeftRadius='10px'
            borderTopRightRadius='0'
            _hover={{
              bg: selectedColor,
            }}
            _selected={{}}
          >
            {isCollapsed ? '<' : '>'}
          </Tab>
          {sections.map((section) => (
            <Tab
            height='100%'
            key={section.title}
            color='#3E4990'
            onClick={() => onSectionChange(section.title)}
            bg={currentSection === section.title ? selectedColor : notSelectedColor}
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
    </Flex>
  )
}
