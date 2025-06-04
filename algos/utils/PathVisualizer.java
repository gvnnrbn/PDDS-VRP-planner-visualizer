package utils;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import entities.PlannerBlockage;

public class PathVisualizer {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int POINT_SIZE = 8;
    private static final Color PATH_COLOR = new Color(41, 121, 255);
    private static final Color BLOCKAGE_COLOR = new Color(255, 89, 94);
    private static final Color START_COLOR = new Color(38, 222, 129);
    private static final Color END_COLOR = new Color(255, 159, 67);
    private static final Color GRID_COLOR = new Color(200, 200, 200);
    private static final BasicStroke PATH_STROKE = new BasicStroke(2f);
    private static final BasicStroke BLOCKAGE_STROKE = new BasicStroke(2f);

    private static JFrame frame;
    private static PathPanel panel;
    private static ArrayList<VisualizationState> history = new ArrayList<>();
    private static int currentIndex = -1;
    private static JButton prevButton;
    private static JButton nextButton;
    private static JLabel descLabel;

    private static class VisualizationState {
        Position start;
        Position end;
        List<Position> path;
        List<PlannerBlockage> blockages;
        int gridLength;
        int gridWidth;
        String description;

        VisualizationState(Position start, Position end, List<Position> path, 
                List<PlannerBlockage> blockages, int gridLength, int gridWidth, String description) {
            this.start = start;
            this.end = end;
            this.path = new ArrayList<>(path);
            this.blockages = new ArrayList<>(blockages);
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            this.description = description;
        }
    }

    private static void updateVisualization(int index) {
        if (index >= 0 && index < history.size()) {
            VisualizationState state = history.get(index);
            panel.updateVisualization(state.start, state.end, state.path, 
                state.blockages, state.gridLength, state.gridWidth);
            descLabel.setText(state.description);
            prevButton.setEnabled(index > 0);
            nextButton.setEnabled(index < history.size() - 1);
            frame.repaint();
        }
    }

    public static void visualizePath(Position start, Position end, List<Position> path, 
            List<PlannerBlockage> blockages, int gridLength, int gridWidth) {
        visualizePath(start, end, path, blockages, gridLength, gridWidth, "Path " + (history.size() + 1));
    }

    public static void visualizePath(Position start, Position end, List<Position> path, 
            List<PlannerBlockage> blockages, int gridLength, int gridWidth, String description) {
        // Also keep the text visualization for debugging
        visualizePathText(start, end, path, blockages, gridLength, gridWidth);
        
        // Add to history
        history.add(new VisualizationState(start, end, path, blockages, gridLength, gridWidth, description));
        currentIndex = history.size() - 1;
        
        // Create and show the graphical visualization
        SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                frame = new JFrame("Path Visualization");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        frame = null;
                        panel = null;
                        history.clear();
                        currentIndex = -1;
                        prevButton = null;
                        nextButton = null;
                        descLabel = null;
                    }
                });
                
                // Create main panel with BorderLayout
                JPanel mainPanel = new JPanel(new BorderLayout());
                
                // Create navigation panel
                JPanel navPanel = new JPanel(new FlowLayout());
                prevButton = new JButton("← Previous");
                nextButton = new JButton("Next →");
                descLabel = new JLabel(description);
                descLabel.setFont(new Font("Arial", Font.BOLD, 14));
                descLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                
                prevButton.addActionListener(e -> {
                    if (currentIndex > 0) {
                        currentIndex--;
                        updateVisualization(currentIndex);
                    }
                });
                
                nextButton.addActionListener(e -> {
                    if (currentIndex < history.size() - 1) {
                        currentIndex++;
                        updateVisualization(currentIndex);
                    }
                });
                
                // Set initial button states
                prevButton.setEnabled(currentIndex > 0);
                nextButton.setEnabled(currentIndex < history.size() - 1);
                
                navPanel.add(prevButton);
                navPanel.add(descLabel);
                navPanel.add(nextButton);
                
                // Create keyboard shortcuts
                InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap actionMap = mainPanel.getActionMap();
                
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
                
                actionMap.put("previous", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (prevButton.isEnabled()) {
                            prevButton.doClick();
                        }
                    }
                });
                
                actionMap.put("next", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (nextButton.isEnabled()) {
                            nextButton.doClick();
                        }
                    }
                });
                
                panel = new PathPanel(start, end, path, blockages, gridLength, gridWidth);
                
                mainPanel.add(navPanel, BorderLayout.NORTH);
                mainPanel.add(panel, BorderLayout.CENTER);
                
                frame.add(mainPanel);
                frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else {
                updateVisualization(currentIndex);
            }
        });
    }

    private static class PathPanel extends JPanel {
        private Position start;
        private Position end;
        private List<Position> path;
        private List<PlannerBlockage> blockages;
        private int gridLength;
        private int gridWidth;
        private double scaleX;
        private double scaleY;
        private int margin = 50;

        public PathPanel(Position start, Position end, List<Position> path, 
                List<PlannerBlockage> blockages, int gridLength, int gridWidth) {
            this.start = start;
            this.end = end;
            this.path = path;
            this.blockages = blockages;
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            
            setBackground(Color.WHITE);
        }

        public void updateVisualization(Position start, Position end, List<Position> path,
                List<PlannerBlockage> blockages, int gridLength, int gridWidth) {
            this.start = start;
            this.end = end;
            this.path = path;
            this.blockages = blockages;
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate scaling factors
            scaleX = (getWidth() - 2 * margin) / (double) gridLength;
            scaleY = (getHeight() - 2 * margin) / (double) gridWidth;

            // Draw grid
            drawGrid(g2);

            // Draw blockages
            g2.setColor(BLOCKAGE_COLOR);
            g2.setStroke(BLOCKAGE_STROKE);
            for (PlannerBlockage blockage : blockages) {
                List<Position> vertices = blockage.vertices;
                if (vertices.size() < 2) continue;
                
                for (int i = 0; i < vertices.size() - 1; i++) {
                    Position from = vertices.get(i);
                    Position to = vertices.get(i + 1);
                    drawLine(g2, from, to);
                }
            }

            // Draw path
            if (path != null && path.size() >= 2) {
                g2.setColor(PATH_COLOR);
                g2.setStroke(PATH_STROKE);
                for (int i = 0; i < path.size() - 1; i++) {
                    Position from = path.get(i);
                    Position to = path.get(i + 1);
                    drawLine(g2, from, to);
                    drawPoint(g2, from, PATH_COLOR);
                }
                drawPoint(g2, path.get(path.size() - 1), PATH_COLOR);
            }

            // Draw start and end points
            drawPoint(g2, start, START_COLOR);
            drawPoint(g2, end, END_COLOR);

            // Draw legend
            drawLegend(g2);
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f));

            // Draw vertical lines and coordinates
            for (int x = 0; x <= gridLength; x++) {
                int screenX = toScreenX(x);
                g2.drawLine(screenX, margin, screenX, getHeight() - margin);
                // Draw coordinate every 5 units
                if (x % 5 == 0) {
                    g2.drawString(String.valueOf(x), screenX - 10, getHeight() - margin/2);
                }
            }

            // Draw horizontal lines and coordinates
            for (int y = 0; y <= gridWidth; y++) {
                int screenY = toScreenY(y);
                g2.drawLine(margin, screenY, getWidth() - margin, screenY);
                // Draw coordinate every 5 units
                if (y % 5 == 0) {
                    g2.drawString(String.valueOf(y), margin/2, screenY + 5);
                }
            }
        }

        private void drawPoint(Graphics2D g2, Position pos, Color color) {
            int x = toScreenX(pos.x) - POINT_SIZE/2;
            int y = toScreenY(pos.y) - POINT_SIZE/2;
            Color prevColor = g2.getColor();
            g2.setColor(color);
            g2.fillOval(x, y, POINT_SIZE, POINT_SIZE);
            g2.setColor(prevColor);
        }

        private void drawLine(Graphics2D g2, Position from, Position to) {
            g2.drawLine(
                toScreenX(from.x), 
                toScreenY(from.y),
                toScreenX(to.x), 
                toScreenY(to.y)
            );
        }

        private void drawLegend(Graphics2D g2) {
            int legendX = margin;
            int legendY = margin/2;
            int spacing = 120;
            
            // Path
            g2.setColor(PATH_COLOR);
            g2.fillOval(legendX, legendY - 5, 10, 10);
            g2.drawString("Path", legendX + 15, legendY + 5);
            
            // Blockages
            g2.setColor(BLOCKAGE_COLOR);
            g2.fillRect(legendX + spacing, legendY - 5, 10, 10);
            g2.drawString("Blockages", legendX + spacing + 15, legendY + 5);
            
            // Start
            g2.setColor(START_COLOR);
            g2.fillOval(legendX + spacing * 2, legendY - 5, 10, 10);
            g2.drawString("Start", legendX + spacing * 2 + 15, legendY + 5);
            
            // End
            g2.setColor(END_COLOR);
            g2.fillOval(legendX + spacing * 3, legendY - 5, 10, 10);
            g2.drawString("End", legendX + spacing * 3 + 15, legendY + 5);
        }

        private int toScreenX(double x) {
            return (int) (x * scaleX + margin);
        }

        private int toScreenY(double y) {
            return (int) (y * scaleY + margin);
        }
    }

    // Keep the original text visualization for debugging
    private static void visualizePathText(Position start, Position end, List<Position> path, 
            List<PlannerBlockage> blockages, int gridLength, int gridWidth) {
        char[][] grid = new char[gridWidth][gridLength];
        for (int y = 0; y < gridWidth; y++) {
            for (int x = 0; x < gridLength; x++) {
                grid[y][x] = '·';
            }
        }

        // Draw blockages
        for (PlannerBlockage blockage : blockages) {
            List<Position> vertices = blockage.vertices;
            if (vertices.size() < 2) continue;
            
            for (int i = 0; i < vertices.size() - 1; i++) {
                Position from = vertices.get(i);
                Position to = vertices.get(i + 1);
                drawLineText(grid, from, to, '█');
            }
        }

        // Draw path
        if (path != null && path.size() >= 2) {
            for (int i = 0; i < path.size() - 1; i++) {
                Position from = path.get(i);
                Position to = path.get(i + 1);
                drawLineText(grid, from, to, '•');
            }
        }

        // Mark start and end positions
        markPositionText(grid, start, 'S');
        markPositionText(grid, end, 'E');

        // Print the grid
        System.out.println("\nPath Visualization (Text):");
        System.out.println("S: Start, E: End, •: Path, █: Blockage, ·: Empty");
        System.out.println();
        
        System.out.print("   ");
        for (int x = 0; x < gridLength; x++) {
            System.out.printf("%2d", x);
        }
        System.out.println();

        for (int y = 0; y < gridWidth; y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < gridLength; x++) {
                System.out.print(" " + grid[y][x]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void markPositionText(char[][] grid, Position pos, char marker) {
        int x = (int) Math.round(pos.x);
        int y = (int) Math.round(pos.y);
        if (x >= 0 && x < grid[0].length && y >= 0 && y < grid.length) {
            grid[y][x] = marker;
        }
    }

    private static void drawLineText(char[][] grid, Position from, Position to, char marker) {
        int x1 = (int) Math.round(from.x);
        int y1 = (int) Math.round(from.y);
        int x2 = (int) Math.round(to.x);
        int y2 = (int) Math.round(to.y);

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x1 >= 0 && x1 < grid[0].length && y1 >= 0 && y1 < grid.length) {
                if (grid[y1][x1] != 'S' && grid[y1][x1] != 'E') {
                    grid[y1][x1] = marker;
                }
            }

            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
} 