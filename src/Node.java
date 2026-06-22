import java.util.ArrayList;

public class Node {
    private ArrayList<Node> children;
    private String skill;
    private Tree.SkillLevel skillLevel;
    private Node parent;

    public Node(String skill, Tree.SkillLevel skillLevel) {
        this.skill = skill;
        this.skillLevel = skillLevel;
        children = new ArrayList<>();
    }

    public void addChild(Node child) {
        child.setParent(this);
        children.add(child);
    }

    public void addChild(int index, Node child) {
        child.setParent(this);
        children.add(index, child);
    }

    public boolean removeChild(Node child) {
        return children.remove(child);
    }

    public ArrayList<Node> getChildren(){
        return children;
    }

    public String getSkill(){
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Tree.SkillLevel getSkillLevel(){
        return skillLevel;
    }

    public void setSkillLevel(Tree.SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public int indexInParent() {
        if (parent == null) {
            return -1;
        }
        return parent.getChildren().indexOf(this);
    }

    public String toString(){
        return skill + " " + skillLevel;
    }
}
