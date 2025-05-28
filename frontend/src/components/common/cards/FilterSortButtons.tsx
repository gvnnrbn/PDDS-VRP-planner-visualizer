import { Button, Flex } from "@chakra-ui/react";
import { faFilter, faSort } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";

interface FilterSortProps {
    entity: string,
}
export const FilterSortButtons = ({
    entity,
}:FilterSortProps) => {
    const handleFilter = () => {}
    const handleSort = () => {}
  return (
    <Flex gap={4} my={-2}>
        <Button size='md' gap={2} variant='primary' onClick={handleFilter}>
            Filtrar
            <FontAwesomeIcon icon={faFilter} />
        </Button>
        <Button size='md' gap={2} variant='primary' onClick={handleSort}>
            Ordenar
            <FontAwesomeIcon icon={faSort} />
        </Button>

    </Flex>
    
);
}