import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class Tree {

    Node root;

    public Tree() {
        loadTree();
        printBreadFirst();
    }

    private void loadTree(){
        
        try {
            FileReader reader = new FileReader("data.txt");
            Scanner scanner = new Scanner(reader);
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                String[] data = line.split(",");
                if(Integer.parseInt(data[1]) == -1){
                    this.root = new Node(data[0], SkillLevel.values()[Integer.parseInt(data[2])], data.length > 3 ? data[3] : null);
                } else {
                    Node parent = findNode(Integer.parseInt(data[1]));
                    if(parent != null){
                        parent.addChild(new Node(data[0], SkillLevel.values()[Integer.parseInt(data[2])], data.length > 3 ? data[3] : null));
                    }
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Node getRoot() {
        return root;
    }

    public Node getNodeByPath(int[] path) {
        Node current = root;
        for (int index : path) {
            if (current == null) {
                return null;
            }
            if (index < 0 || index >= current.getChildren().size()) {
                return null;
            }
            current = current.getChildren().get(index);
        }
        return current;
    }

    public void saveTree() throws IOException {
        if (root == null) {
            return;
        }
        Map<Node, Integer> ids = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        int index = 0;
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            ids.put(current, index++);
            queue.addAll(current.getChildren());
        }

        queue.clear();
        queue.add(root);
        ArrayList<String> lines = new ArrayList<>();
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int parentId = current.getParent() == null ? -1 : ids.getOrDefault(current.getParent(), -1);
            lines.add(current.getSkill() + "," + parentId + "," + current.getSkillLevel().ordinal() + (current.getURL().isPresent() ? "," : "") + current.getURL().orElse(""));
            queue.addAll(current.getChildren());
        }

        Path file = Path.of("data.txt");
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private Node findNode(int id) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        int i = 0;
        while(!queue.isEmpty()){
            Node current = queue.poll();
            if(id == i) return current;
            queue.addAll(current.getChildren());
            i++;
        }
        return null;
    }

    private void printBreadFirst(){
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while(!queue.isEmpty()){
            Node current = queue.poll();
            System.out.println(current);
            queue.addAll(current.getChildren());
        }
    }

    public enum SkillLevel{
        
        COAL(new Color(61, 61, 61)),
        IRON(new Color(255, 204, 153)),
        GOLD(new Color(255, 196, 0)),
        EMERALD(new Color(0, 165, 33)),
        DIAMOND(new Color(61, 236, 236)),
        NETHERITE(new Color(129, 0, 176));
        private Color color;
        private SkillLevel(Color color){
            this.color = color;
        };
        public Color getColor(){
            return color;
        }

        public SkillLevel next() {
            SkillLevel[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public SkillLevel prev() {
            SkillLevel[] values = values();
            return values[(this.ordinal() - 1 + values.length) % values.length];
        }
    }
}
