import { useRef, useState } from "react";
import { Stage, Layer, Line } from "react-konva";

const CELL_SIZE = 20;
const GRID_WIDTH = 70;
const GRID_HEIGHT = 50;

export const MapGrid = () => {
    const stageRef = useRef<any>(null);
    const [scale, setScale] = useState(1);
    const [position, setPosition] = useState({ x: 0, y: 0 });

    // Handle zooming
    const handleWheel = (e: any) => {
        e.evt.preventDefault();
        const scaleBy = 1.05;
        const stage = stageRef.current;

        const oldScale = stage.scaleX();
        const pointer = stage.getPointerPosition();

        const mousePointTo = {
            x: (pointer.x - stage.x()) / oldScale,
            y: (pointer.y - stage.y()) / oldScale,
        };

        const direction = e.evt.deltaY > 0 ? -1 : 1;
        const newScale = direction > 0 ? oldScale * scaleBy : oldScale / scaleBy;

        stage.scale({ x: newScale, y: newScale });

        const newPos = {
            x: pointer.x - mousePointTo.x * newScale,
            y: pointer.y - mousePointTo.y * newScale,
        };

        stage.position(newPos);
        stage.batchDraw();
        setScale(newScale);
        setPosition(newPos);
    };

    // Generate grid lines
    const gridLines = () => {
        const lines = [];

        for (let i = 0; i <= GRID_WIDTH; i++) {
        lines.push(
            <Line
            key={`v-${i}`}
            points={[i * CELL_SIZE, 0, i * CELL_SIZE, GRID_HEIGHT * CELL_SIZE]}
            stroke="#ddd"
            strokeWidth={1}
            />
        );
        }

        for (let j = 0; j <= GRID_HEIGHT; j++) {
        lines.push(
            <Line
            key={`h-${j}`}
            points={[0, j * CELL_SIZE, GRID_WIDTH * CELL_SIZE, j * CELL_SIZE]}
            stroke="#ddd"
            strokeWidth={1}
            />
        );
        }

        return lines;
    };

    return (
        <Stage
            width={window.innerWidth}
            height={window.innerHeight}
            draggable
            ref={stageRef}
            onWheel={handleWheel}
            scaleX={scale}
            scaleY={scale}
            x={position.x}
            y={position.y}
            style={{ 
                position: "absolute",
                top: 0,
                left: 0,
                background: "#f8f8f8",
                overflow: "hidden",
                cursor: "grab" }}
            >
            <Layer>{gridLines()}</Layer>
        </Stage>
    );
};