import java.util.ArrayList;

public class Node {
    private ArrayList<Node> children;
    private String skill;
    private Tree.SkillLevel skillLevel;

    public Node(String skill, Tree.SkillLevel skillLevel) {
        this.skill = skill;
        this.skillLevel = skillLevel;
        children = new ArrayList<>();
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public ArrayList<Node> getChildren(){
        return children;
    }

    public String getSkill(){
        return skill;
    }

    public String toString(){
        return skill + " " + skillLevel;
    }
}
