import { extendTheme } from "@chakra-ui/react";
import { ButtonStyles as Button } from "./buttonStyles";

export const customTheme = extendTheme({
    components: {
        Button,
    },
    colors: {
        purple: {
          100: '#5459EA',
          200: '#483190',
          300: '#13187E',
        }
      },
});