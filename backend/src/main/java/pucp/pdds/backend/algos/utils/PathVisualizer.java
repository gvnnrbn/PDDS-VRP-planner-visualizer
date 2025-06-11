package pucp.pdds.backend.algos.utils;

import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import pucp.pdds.backend.algos.entities.PlannerBlockage;

public class PathVisualizer {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int POINT_SIZE = 8;
    private static final int BLOCKAGE_STROKE = 3;
    private static final Color PATH_COLOR = new Color(0, 150, 0);
    private static final Color START_COLOR = new Color(0, 200, 0);
    private static final Color END_COLOR = new Color(200, 0, 0);
    private static final Color BLOCKAGE_COLOR = new Color(0, 0, 0);
    private static final Color GRID_COLOR = new Color(200, 200, 200);
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    private static List<VisualizationState> visualizationStates = new ArrayList<>();
    private static int currentStateIndex = 0;
    private static JFrame frame;
    private static VisualizationPanel visualPanel;
    private static JButton prevButton;
    private static JButton nextButton;

    private static class VisualizationPanel extends JPanel {
        private final int gridLength;
        private final int gridWidth;

        public VisualizationPanel(int gridLength, int gridWidth) {
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setBackground(BACKGROUND_COLOR);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (visualizationStates.isEmpty()) return;
            
            VisualizationState state = visualizationStates.get(currentStateIndex);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate scaling factors
            double scaleX = (double) (getWidth() - 40) / gridLength;
            double scaleY = (double) (getHeight() - 40) / gridWidth;

            // Draw grid
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(1));
            for (int x = 0; x <= gridLength; x++) {
                int screenX = 20 + (int) (x * scaleX);
                g2d.drawLine(screenX, 20, screenX, getHeight() - 20);
            }
            for (int y = 0; y <= gridWidth; y++) {
                int screenY = 20 + (int) (y * scaleY);
                g2d.drawLine(20, screenY, getWidth() - 20, screenY);
            }

            // Draw path if it exists
            if (!state.path.isEmpty()) {
                g2d.setColor(PATH_COLOR);
                g2d.setStroke(new BasicStroke(2));
                for (int i = 0; i < state.path.size() - 1; i++) {
                    Position current = state.path.get(i);
                    Position next = state.path.get(i + 1);
                    int x1 = 20 + (int) (current.x * scaleX);
                    int y1 = 20 + (int) (current.y * scaleY);
                    int x2 = 20 + (int) (next.x * scaleX);
                    int y2 = 20 + (int) (next.y * scaleY);
                    g2d.drawLine(x1, y1, x2, y2);
                }

                // Draw points along the path
                for (Position pos : state.path) {
                    int x = 20 + (int) (pos.x * scaleX) - POINT_SIZE/2;
                    int y = 20 + (int) (pos.y * scaleY) - POINT_SIZE/2;
                    g2d.fillOval(x, y, POINT_SIZE, POINT_SIZE);
                }
            }

            // Draw start and end points
            g2d.setColor(START_COLOR);
            int startX = 20 + (int) (state.start.x * scaleX) - POINT_SIZE;
            int startY = 20 + (int) (state.start.y * scaleY) - POINT_SIZE;
            g2d.fillOval(startX, startY, POINT_SIZE * 2, POINT_SIZE * 2);

            g2d.setColor(END_COLOR);
            int endX = 20 + (int) (state.end.x * scaleX) - POINT_SIZE;
            int endY = 20 + (int) (state.end.y * scaleY) - POINT_SIZE;
            g2d.fillOval(endX, endY, POINT_SIZE * 2, POINT_SIZE * 2);

            // Draw blockages last and make them more prominent
            g2d.setColor(BLOCKAGE_COLOR);
            g2d.setStroke(new BasicStroke(BLOCKAGE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (PlannerBlockage blockage : state.blockages) {
                List<Position> vertices = blockage.vertices;
                for (int i = 0; i < vertices.size() - 1; i++) {
                    Position v1 = vertices.get(i);
                    Position v2 = vertices.get(i + 1);
                    int x1 = 20 + (int) (v1.x * scaleX);
                    int y1 = 20 + (int) (v1.y * scaleY);
                    int x2 = 20 + (int) (v2.x * scaleX);
                    int y2 = 20 + (int) (v2.y * scaleY);
                    g2d.drawLine(x1, y1, x2, y2);

                    // Draw vertices as small filled circles
                    g2d.fillOval(x1 - POINT_SIZE/2, y1 - POINT_SIZE/2, POINT_SIZE, POINT_SIZE);
                    g2d.fillOval(x2 - POINT_SIZE/2, y2 - POINT_SIZE/2, POINT_SIZE, POINT_SIZE);
                }
            }

            // Draw state counter
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String stateText = String.format("Path %d of %d", currentStateIndex + 1, visualizationStates.size());
            g2d.drawString(stateText, 10, getHeight() - 10);
        }
    }

    private static class VisualizationState {
        Position start;
        Position end;
        List<Position> path;
        List<PlannerBlockage> blockages;

        VisualizationState(Position start, Position end, List<Position> path, List<PlannerBlockage> blockages) {
            this.start = start;
            this.end = end;
            this.path = new ArrayList<>(path);
            this.blockages = blockages;
        }
    }

    public static void visualizePath(Position start, Position end, List<Position> path, List<PlannerBlockage> blockages, int gridLength, int gridWidth) {
        VisualizationState state = new VisualizationState(start, end, path, blockages);
        visualizationStates.add(state);
        currentStateIndex = visualizationStates.size() - 1;
        
        SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                // Create new window only if it doesn't exist
                frame = new JFrame("Path Visualization");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        // Clear static references when window is closed
                        frame = null;
                        visualPanel = null;
                        prevButton = null;
                        nextButton = null;
                        visualizationStates.clear();
                    }
                });
                
                visualPanel = new VisualizationPanel(gridLength, gridWidth);
                
                // Create navigation buttons
                prevButton = new JButton("Previous");
                nextButton = new JButton("Next");
                JButton clearButton = new JButton("Clear History");
                
                // Add button listeners
                prevButton.addActionListener(e -> {
                    if (currentStateIndex > 0) {
                        currentStateIndex--;
                        visualPanel.repaint();
                        updateButtonStates();
                    }
                });
                
                nextButton.addActionListener(e -> {
                    if (currentStateIndex < visualizationStates.size() - 1) {
                        currentStateIndex++;
                        visualPanel.repaint();
                        updateButtonStates();
                    }
                });
                
                clearButton.addActionListener(e -> {
                    VisualizationState currentState = visualizationStates.get(currentStateIndex);
                    visualizationStates.clear();
                    visualizationStates.add(currentState);
                    currentStateIndex = 0;
                    visualPanel.repaint();
                    updateButtonStates();
                });
                
                // Create button panel
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(prevButton);
                buttonPanel.add(nextButton);
                buttonPanel.add(clearButton);
                
                // Add components to frame
                frame.setLayout(new BorderLayout());
                frame.add(visualPanel, BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else {
                // Update existing window
                visualPanel.repaint();
                updateButtonStates();
            }
        });
    }
    
    private static void updateButtonStates() {
        if (prevButton != null && nextButton != null) {
            prevButton.setEnabled(currentStateIndex > 0);
            nextButton.setEnabled(currentStateIndex < visualizationStates.size() - 1);
        }
    }
} 