import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class AStar {
    private static class Node {
        Position pos;
        Node parent;
        int gScore;
        int fScore;

        Node(Position pos) {
            this.pos = pos;
            this.gScore = Integer.MAX_VALUE;
            this.fScore = Integer.MAX_VALUE;
        }
    }

    private static String nodeKey(Position pos) {
        return pos.x + "," + pos.y;
    }

    private static int getManhattanDistance(Position a, Position b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private static List<PathFragment> reconstructPath(Node end) {
        List<PathFragment> path = new ArrayList<>();
        Node current = end;
        
        while (current.parent != null) {
            PathFragment fragment = new PathFragment();
            fragment.start = current.parent.pos;
            fragment.end = current.pos;
            path.add(0, fragment);
            current = current.parent;
        }
        
        return path;
    }

    public static List<PathFragment> findPath(Position start, Position end, int gridLength, int gridWidth, List<Blockage> blockages) {
        PriorityQueue<Node> openSet = new PriorityQueue<>((a, b) -> a.fScore - b.fScore);
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> nodes = new HashMap<>();
        
        Node startNode = new Node(start);
        startNode.gScore = 0;
        startNode.fScore = getManhattanDistance(start, end);
        
        openSet.add(startNode);
        nodes.put(nodeKey(start), startNode);
        
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            if (current.pos.x == end.x && current.pos.y == end.y) {
                return reconstructPath(current);
            }
            
            closedSet.add(nodeKey(current.pos));
            
            int[][] dirs = {{0,1}, {1,0}, {0,-1}, {-1,0}};
            for (int[] dir : dirs) {
                Position nextPos = new Position();
                nextPos.x = current.pos.x + dir[0];
                nextPos.y = current.pos.y + dir[1];
                
                if (nextPos.x < 0 || nextPos.x >= gridLength || 
                    nextPos.y < 0 || nextPos.y >= gridWidth) {
                    continue;
                }
                
                boolean isBlocked = false;
                for (Blockage blockage : blockages) {
                    if (blockage.isBlocked(current.pos, nextPos)) {
                        isBlocked = true;
                        break;
                    }
                }
                if (isBlocked) continue;
                
                String nextKey = nodeKey(nextPos);
                if (closedSet.contains(nextKey)) continue;
                
                int tentativeGScore = current.gScore + 1;
                
                Node neighbor = nodes.get(nextKey);
                if (neighbor == null) {
                    neighbor = new Node(nextPos);
                    nodes.put(nextKey, neighbor);
                }
                
                if (!openSet.contains(neighbor) || tentativeGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = neighbor.gScore + getManhattanDistance(nextPos, end);
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        return new ArrayList<>(); // No path found
    }
}
