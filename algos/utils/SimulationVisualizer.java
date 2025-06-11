package utils;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import java.awt.*;
import entities.PlannerVehicle;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import algorithm.Node;

public class SimulationVisualizer {
    private static JFrame visFrame = null;
    private static VehicleVisualizerPanel visPanel = null;

    private static class VehicleVisualizerPanel extends JPanel {
        private final int gridLength;
        private final int gridWidth;
        private List<PlannerVehicle> vehicles;
        private List<PlannerBlockage> blockages;
        private List<Node> deliveryNodes;
        private List<Node> refillNodes;
        private List<PlannerWarehouse> warehouses;
        private String currentTimeString = "";

        public VehicleVisualizerPanel(int gridLength, int gridWidth) {
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            setPreferredSize(new Dimension(900, 700));
            setBackground(Color.WHITE);
        }

        public void updateState(List<PlannerVehicle> vehicles, List<PlannerBlockage> blockages, 
                              List<Node> deliveryNodes, List<Node> refillNodes, String currentTimeString) {
            this.vehicles = vehicles;
            this.blockages = blockages;
            this.deliveryNodes = deliveryNodes;
            this.refillNodes = refillNodes;
            this.currentTimeString = currentTimeString;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margin = 40;
            double scaleX = (getWidth() - 2 * margin) / (double) gridLength;
            double scaleY = (getHeight() - 2 * margin) / (double) gridWidth;

            // Draw current time
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString("Time: " + currentTimeString, 20, 30);

            // Draw grid
            g2d.setColor(new Color(220, 220, 220));
            for (int x = 0; x <= gridLength; x++) {
                int sx = margin + (int) (x * scaleX);
                g2d.drawLine(sx, margin, sx, getHeight() - margin);
            }
            for (int y = 0; y <= gridWidth; y++) {
                int sy = margin + (int) (y * scaleY);
                g2d.drawLine(margin, sy, getWidth() - margin, sy);
            }

            // Draw blockages
            if (blockages != null) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(3));
                for (PlannerBlockage blockage : blockages) {
                    List<Position> verts = blockage.vertices;
                    for (int i = 0; i < verts.size() - 1; i++) {
                        Position v1 = verts.get(i);
                        Position v2 = verts.get(i + 1);
                        int x1 = margin + (int) (v1.x * scaleX);
                        int y1 = margin + (int) (v1.y * scaleY);
                        int x2 = margin + (int) (v2.x * scaleX);
                        int y2 = margin + (int) (v2.y * scaleY);
                        g2d.drawLine(x1, y1, x2, y2);
                        g2d.fillOval(x1 - 4, y1 - 4, 8, 8);
                        g2d.fillOval(x2 - 4, y2 - 4, 8, 8);
                    }
                }
            }

            // Draw warehouses
            if (warehouses != null) {
                for (PlannerWarehouse warehouse : warehouses) {
                    Position pos = warehouse.position;
                    int x = margin + (int) (pos.x * scaleX) - 12;
                    int y = margin + (int) (pos.y * scaleY) - 12;
                    
                    // Draw warehouse as a house shape
                    g2d.setColor(new Color(150, 150, 150)); // Gray for warehouse
                    int[] xPoints = {x, x + 24, x + 24, x};
                    int[] yPoints = {y + 12, y + 12, y + 24, y + 24};
                    g2d.fillPolygon(xPoints, yPoints, 4);
                    
                    // Draw roof
                    int[] roofXPoints = {x - 4, x + 12, x + 28};
                    int[] roofYPoints = {y + 12, y, y + 12};
                    g2d.fillPolygon(roofXPoints, roofYPoints, 3);
                    
                    // Draw warehouse ID
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("W" + warehouse.id, x + 8, y + 20);
                    
                    // Draw capacity indicator
                    double capacityPercentage = (double) warehouse.currentGLP / warehouse.maxGLP;
                    int barWidth = 20;
                    int barHeight = 4;
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.fillRect(x + 2, y + 26, barWidth, barHeight);
                    g2d.setColor(new Color(0, 200, 0));
                    g2d.fillRect(x + 2, y + 26, (int)(barWidth * capacityPercentage), barHeight);
                }
            }

            // Draw delivery nodes
            if (deliveryNodes != null) {
                g2d.setColor(new Color(255, 100, 100)); // Red for delivery
                for (Node node : deliveryNodes) {
                    Position pos = node.getPosition();
                    int x = margin + (int) (pos.x * scaleX) - 8;
                    int y = margin + (int) (pos.y * scaleY) - 8;
                    g2d.fillOval(x, y, 16, 16);
                }
            }

            // Draw refill nodes
            if (refillNodes != null) {
                g2d.setColor(new Color(100, 100, 255)); // Blue for refill
                for (Node node : refillNodes) {
                    Position pos = node.getPosition();
                    int x = margin + (int) (pos.x * scaleX) - 8;
                    int y = margin + (int) (pos.y * scaleY) - 8;
                    g2d.fillRect(x, y, 16, 16);
                }
            }

            // Draw vehicle paths and positions
            if (vehicles != null) {
                for (PlannerVehicle v : vehicles) {
                    // Draw path only if vehicle is not STUCK
                    if (v.currentPath != null && v.currentPath.size() > 1 && v.state != PlannerVehicle.VehicleState.STUCK) {
                        g2d.setColor(new Color(0, 180, 0, 180));
                        g2d.setStroke(new BasicStroke(2));
                        for (int i = 0; i < v.currentPath.size() - 1; i++) {
                            Position p1 = v.currentPath.get(i);
                            Position p2 = v.currentPath.get(i + 1);
                            int x1 = margin + (int) (p1.x * scaleX);
                            int y1 = margin + (int) (p1.y * scaleY);
                            int x2 = margin + (int) (p2.x * scaleX);
                            int y2 = margin + (int) (p2.y * scaleY);
                            g2d.drawLine(x1, y1, x2, y2);
                        }
                    }
                    // Draw vehicle position
                    Color vehicleColor;
                    switch (v.state) {
                        case STUCK:
                            vehicleColor = new Color(255, 0, 0); // Red for failed vehicles
                            break;
                        case MAINTENANCE:
                            vehicleColor = new Color(255, 165, 0); // Orange for maintenance
                            break;
                        default:
                            vehicleColor = new Color(255, 200, 0); // Yellow for normal state
                    }
                    g2d.setColor(vehicleColor);
                    int vx = margin + (int) (v.position.x * scaleX) - 10;
                    int vy = margin + (int) (v.position.y * scaleY) - 10;
                    g2d.fillOval(vx, vy, 20, 20);
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("V" + v.id, vx, vy - 5);
                }
            }
        }
    }

    public static void draw(List<PlannerVehicle> vehicles, List<PlannerBlockage> blockages, 
                          List<Node> deliveryNodes, List<Node> refillNodes, Time currentTime, int minutesToSimulate) {
        int gridLength = SimulationProperties.gridLength;
        int gridWidth = SimulationProperties.gridWidth;
        SwingUtilities.invokeLater(() -> {
            if (visFrame == null) {
                visFrame = new JFrame("Vehicle & Node Visualization");
                visPanel = new VehicleVisualizerPanel(gridLength, gridWidth);
                visFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                visFrame.add(visPanel);
                visFrame.pack();
                visFrame.setLocationRelativeTo(null);
                visFrame.setVisible(true); 
            }
            List<PlannerBlockage> activeBlockages = blockages.stream()
                .filter(blockage -> blockage.isActive(currentTime, currentTime.addMinutes(minutesToSimulate)))
                .collect(Collectors.toList());
            visPanel.warehouses = CSVDataParser.parseWarehouses("main/warehouses.csv");
            visPanel.updateState(vehicles, activeBlockages, deliveryNodes, refillNodes, currentTime.toString());
        });
    }
} 