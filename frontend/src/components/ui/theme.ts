import { extendTheme } from "@chakra-ui/react";
import { ButtonStyles as Button } from "./buttonStyles";

export const customTheme = extendTheme({
    components: {
        Button,
    },
});