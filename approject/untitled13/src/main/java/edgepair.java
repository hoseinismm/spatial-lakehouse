import java.util.*;
import java.util.regex.*;

public class edgepair {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder input = new StringBuilder();

        System.out.println("Paste your edges data below (end input with an empty line):");
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            input.append(line).append("\n");
        }

        // Parse input
        List<Edge> edges = parseInput(input.toString());

        // Find paired edges
        List<PairedEdge> pairedEdges = findPairedEdges(edges);

        // Print paired edges with numbering
        for (int i = 0; i < pairedEdges.size(); i++) {
            System.out.println("Paired Edge " + (i + 1) + ": " + pairedEdges.get(i));
        }
    }

    public static List<Edge> parseInput(String input) {
        List<Edge> edges = new ArrayList<>();
        Pattern pattern = Pattern.compile("Edge \\d+: Node (\\d+) → Node (\\d+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String start = "Node " + matcher.group(1);
            String end = "Node " + matcher.group(2);
            edges.add(new Edge(start, end));
        }

        return edges;
    }

    public static List<PairedEdge> findPairedEdges(List<Edge> edges) {
        List<PairedEdge> pairedEdges = new ArrayList<>();

        // Create a map from each node to the list of outgoing edges
        Map<String, List<Edge>> outgoingEdges = new HashMap<>();
        for (Edge edge : edges) {
            outgoingEdges.putIfAbsent(edge.start, new ArrayList<>());
            outgoingEdges.get(edge.start).add(edge);
        }

        // Find paired edges
        for (Edge edge1 : edges) {
            List<Edge> nextEdges = outgoingEdges.get(edge1.end);
            if (nextEdges != null) {
                for (Edge edge2 : nextEdges) {
                    pairedEdges.add(new PairedEdge(edge1, edge2));
                }
            }
        }

        return pairedEdges;
    }

    static class Edge {
        String start;
        String end;

        Edge(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start + " → " + end;
        }
    }

    static class PairedEdge {
        Edge first;
        Edge second;

        PairedEdge(Edge first, Edge second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return first + " , " + second;
        }
    }
}
