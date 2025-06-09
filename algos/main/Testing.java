package main;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import algorithm.Algorithm;
import algorithm.Environment;
import algorithm.Node;
import algorithm.Solution;
import utils.DataParser;
import utils.PathBuilder;
import utils.SimulationProperties;
import utils.Time;
import entities.PlannerVehicle;
import entities.PlannerOrder;
import entities.PlannerBlockage;
import entities.PlannerWarehouse;
import entities.PlannerFailure;
import entities.PlannerMaintenance;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Testing {
    private static List<PlannerBlockage> getActiveBlockages(List<PlannerBlockage> blockages, Time time) {
        return blockages.stream()
            .filter(blockage -> blockage.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerOrder> getActiveOrders(List<PlannerOrder> orders, Time time) {
        return orders.stream()
            .filter(order -> order.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerMaintenance> getActiveMaintenances(List<PlannerMaintenance> maintenances, Time time) {
        return maintenances.stream()
            .filter(maintenance -> maintenance.isActive(time))
            .collect(Collectors.toList());
    }

    private static List<PlannerVehicle> getActiveVehicles(List<PlannerVehicle> vehicles, Time time) {
        return vehicles.stream()
            .filter(vehicle -> vehicle.isActive(time))
            .collect(Collectors.toList());
    }

    // --- Visualization Panel for Vehicles and Nodes ---
    private static JFrame visFrame = null;
    private static VehicleVisualizerPanel visPanel = null;

    private static class VehicleVisualizerPanel extends JPanel {
        private final int gridLength;
        private final int gridWidth;
        private List<PlannerVehicle> vehicles;
        private List<PlannerBlockage> blockages;
        private List<Node> deliveryNodes;
        private List<Node> refillNodes;

        public VehicleVisualizerPanel(int gridLength, int gridWidth) {
            this.gridLength = gridLength;
            this.gridWidth = gridWidth;
            setPreferredSize(new java.awt.Dimension(900, 700));
            setBackground(java.awt.Color.WHITE);
        }

        public void updateState(List<PlannerVehicle> vehicles, List<PlannerBlockage> blockages, List<Node> deliveryNodes, List<Node> refillNodes) {
            this.vehicles = vehicles;
            this.blockages = blockages;
            this.deliveryNodes = deliveryNodes;
            this.refillNodes = refillNodes;
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int margin = 40;
            double scaleX = (getWidth() - 2 * margin) / (double) gridLength;
            double scaleY = (getHeight() - 2 * margin) / (double) gridWidth;

            // Draw grid
            g2d.setColor(new java.awt.Color(220, 220, 220));
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
                g2d.setColor(java.awt.Color.BLACK);
                g2d.setStroke(new java.awt.BasicStroke(3));
                for (PlannerBlockage blockage : blockages) {
                    List<utils.Position> verts = blockage.vertices;
                    for (int i = 0; i < verts.size() - 1; i++) {
                        utils.Position v1 = verts.get(i);
                        utils.Position v2 = verts.get(i + 1);
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

            // Draw delivery nodes
            if (deliveryNodes != null) {
                g2d.setColor(new java.awt.Color(255, 100, 100)); // Red for delivery
                for (Node node : deliveryNodes) {
                    utils.Position pos = node.getPosition();
                    int x = margin + (int) (pos.x * scaleX) - 8;
                    int y = margin + (int) (pos.y * scaleY) - 8;
                    g2d.fillOval(x, y, 16, 16);
                }
            }
            // Draw refill nodes
            if (refillNodes != null) {
                g2d.setColor(new java.awt.Color(100, 100, 255)); // Blue for refill
                for (Node node : refillNodes) {
                    utils.Position pos = node.getPosition();
                    int x = margin + (int) (pos.x * scaleX) - 8;
                    int y = margin + (int) (pos.y * scaleY) - 8;
                    g2d.fillRect(x, y, 16, 16);
                }
            }

            // Draw vehicle paths and positions
            if (vehicles != null) {
                for (PlannerVehicle v : vehicles) {
                    // Draw path
                    if (v.currentPath != null && v.currentPath.size() > 1) {
                        g2d.setColor(new java.awt.Color(0, 180, 0, 180));
                        g2d.setStroke(new java.awt.BasicStroke(2));
                        for (int i = 0; i < v.currentPath.size() - 1; i++) {
                            utils.Position p1 = v.currentPath.get(i);
                            utils.Position p2 = v.currentPath.get(i + 1);
                            int x1 = margin + (int) (p1.x * scaleX);
                            int y1 = margin + (int) (p1.y * scaleY);
                            int x2 = margin + (int) (p2.x * scaleX);
                            int y2 = margin + (int) (p2.y * scaleY);
                            g2d.drawLine(x1, y1, x2, y2);
                        }
                    }
                    // Draw vehicle position
                    g2d.setColor(new java.awt.Color(255, 200, 0));
                    int vx = margin + (int) (v.position.x * scaleX) - 10;
                    int vy = margin + (int) (v.position.y * scaleY) - 10;
                    g2d.fillOval(vx, vy, 20, 20);
                    g2d.setColor(java.awt.Color.BLACK);
                    g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
                    g2d.drawString("V" + v.id, vx, vy - 5);
                }
            }
        }
    }

    // --- Main draw() method ---
    public static void draw(List<PlannerVehicle> vehicles, List<PlannerBlockage> blockages, List<Node> deliveryNodes, List<Node> refillNodes) {
        int gridLength = utils.SimulationProperties.gridLength;
        int gridWidth = utils.SimulationProperties.gridWidth;
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (visFrame == null) {
                visFrame = new JFrame("Vehicle & Node Visualization");
                visPanel = new VehicleVisualizerPanel(gridLength, gridWidth);
                visFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                visFrame.add(visPanel);
                visFrame.pack();
                visFrame.setLocationRelativeTo(null);
                visFrame.setVisible(true);
            }
            visPanel.updateState(vehicles, blockages, deliveryNodes, refillNodes);
        });
    }

    public static void main(String[] args) {
        int minutesToSimulate = 60;
        Time currTime = new Time(2025, 1, 1, 0, 0);

        List<PlannerVehicle> vehicles = DataParser.parseVehicles("main/vehicles.csv");
        List<PlannerOrder> orders = DataParser.parseOrders("main/orders.csv");
        List<PlannerBlockage> blockages = DataParser.parseBlockages("main/blockages.csv");
        List<PlannerWarehouse> warehouses = DataParser.parseWarehouses("main/warehouses.csv");
        List<PlannerFailure> failures = DataParser.parseFailures("main/failures.csv");
        List<PlannerMaintenance> maintenances = DataParser.parseMaintenances("main/maintenances.csv");

        for(int i=0; i<10; i++) {
            List<PlannerBlockage> activeBlockages = getActiveBlockages(blockages, currTime);
            List<PlannerOrder> activeOrders = getActiveOrders(orders, currTime);
            List<PlannerMaintenance> activeMaintenances = getActiveMaintenances(maintenances, currTime);
            List<PlannerVehicle> activeVehicles = getActiveVehicles(vehicles, currTime);

            Environment environment = new Environment(activeVehicles, activeOrders, warehouses, activeBlockages, failures, activeMaintenances, currTime, minutesToSimulate);
            System.out.println("Planning interval " + i + " started at " + currTime + " with " + activeVehicles.size() + " vehicles and " + activeOrders.size() + " orders");
            Solution sol = Algorithm.run(environment, minutesToSimulate);
            System.out.println(sol.getReport());

            for (PlannerVehicle vehicle : activeVehicles) {
                vehicle.nextNodeIndex = 1;
                if (vehicle.state == PlannerVehicle.VehicleState.FINISHED) {
                    vehicle.state = PlannerVehicle.VehicleState.IDLE;
                }
            }

            for (int iteration = 0; iteration < minutesToSimulate; iteration++) {
                System.out.println("--- Time: " + currTime + " ---");

                for (PlannerVehicle plannerVehicle : activeVehicles) {
                    if (plannerVehicle.state == PlannerVehicle.VehicleState.FINISHED) {
                        continue;
                    }

                    // System.out.println("Vehicle " + plannerVehicle.id + " started at position: " + plannerVehicle.position);

                    // Create path when there's no path or the path is empty
                    if (plannerVehicle.currentPath == null || plannerVehicle.currentPath.isEmpty()) {
                        // System.out.println("Before currentPath correction the path is: " + plannerVehicle.currentPath);

                        // Has arrived at location
                        Node arrivedNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex);
                        System.out.println("Vehicle " + plannerVehicle.id + " has arrived at location of node " + arrivedNode);

                        // HERE GOES ON_REACHING_NODE_LOCATION
                        // System.out.println("Vehicle " + plannerVehicle.id + " is processing node " + arrivedNode);
                        plannerVehicle.processNode(arrivedNode, plannerVehicle, activeOrders, warehouses, currTime);

                        plannerVehicle.nextNodeIndex++;

                        if (plannerVehicle.nextNodeIndex >= sol.routes.get(plannerVehicle.id).size()) {
                            // Has reached last node
                            System.out.println("HAS REACHED LAST NODE");
                            plannerVehicle.state = PlannerVehicle.VehicleState.FINISHED;
                            continue;
                        }

                        Node currNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex - 1);
                        Node nextNode = sol.routes.get(plannerVehicle.id).get(plannerVehicle.nextNodeIndex);

                        plannerVehicle.currentPath = PathBuilder.buildPath(currNode.getPosition(), nextNode.getPosition(), activeBlockages);
                        // System.out.println("After currentPath correction the path is: " + plannerVehicle.currentPath);
                    } else {

                        if (plannerVehicle.waitTransition > 0) {
                            System.out.println("Vehicle " + plannerVehicle.id + " is waiting for " + plannerVehicle.waitTransition + " minutes");
                            plannerVehicle.waitTransition--;
                        } else {
                            // System.out.println("Vehicle " + plannerVehicle.id + " is advancing path");
                            plannerVehicle.advancePath(SimulationProperties.speed / 60.0);
                        }
                    }
                    // System.out.println("Vehicle " + plannerVehicle.id + " ended at position: " + plannerVehicle.position);
                }

                // Collect only delivery nodes currently being served (next node for each vehicle if it's an OrderDeliverNode)
                List<Node> deliveryNodes = new ArrayList<>();
                List<Node> refillNodes = new ArrayList<>();
                for (PlannerVehicle v : activeVehicles) {
                    List<Node> route = sol.routes.get(v.id);
                    if (route != null && v.nextNodeIndex < route.size()) {
                        Node nextNode = route.get(v.nextNodeIndex);
                        if (nextNode instanceof algorithm.OrderDeliverNode) {
                            deliveryNodes.add(nextNode);
                        } else if (nextNode instanceof algorithm.ProductRefillNode) {
                            refillNodes.add(nextNode);
                        }
                    }
                }
                draw(activeVehicles, activeBlockages, deliveryNodes, refillNodes);
                try {
                    Thread.sleep(250); // 750 ms delay between steps
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                currTime = currTime.addMinutes(1);
            }
        }

    }
}
