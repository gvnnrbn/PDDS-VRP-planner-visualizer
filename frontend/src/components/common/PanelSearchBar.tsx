import { Button, Flex, Input } from "@chakra-ui/react"
import { faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons"
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome"

interface PanelSearchBarProps {
  onSubmit: () => void
}

export const PanelSearchBar = ({ 
  onSubmit
}: PanelSearchBarProps) => {
  
  return (
  <>
    <Flex borderRadius='20px' py={1} px={4} ml={-4} gap={1} align={'center'}>
        <Input border={0} size='sm' rounded={'full'} bg='white' mx={-1}>
        </Input>
        <Button rounded={'full'} variant='primary' mr={-4} ml={1} size='sm'
            onClick={() => console.log('Buscar')}
        >
            <FontAwesomeIcon icon={faMagnifyingGlass} size={'lg'}/>
        </Button>
    </Flex>
  </>

  )
}