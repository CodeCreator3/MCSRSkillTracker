import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
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
                    this.root = new Node(data[0], SkillLevel.values()[Integer.parseInt(data[2])]);
                } else {
                    Node parent = findNode(Integer.parseInt(data[1]));
                    if(parent != null){
                        parent.addChild(new Node(data[0], SkillLevel.values()[Integer.parseInt(data[2])]));
                    }
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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

    public enum SkillLevel {
        COAL,
        IRON,
        GOLD,
        EMERALD,
        DIAMOND,
        NETHERITE
    }
}
