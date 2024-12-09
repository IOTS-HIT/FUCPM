package HUSPull;

/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */
public class UItem {
    private int itemName;
    private double utility;

    public UItem(int item, double utility) {
        this.itemName = item;
        this.utility = utility;
    }

    public double utility() {
        return utility;
    }

    public int itemName() {
        return itemName;
    }

    @Override
    public String toString() {
        return "UItem(" + itemName + ", " + utility + ')';
    }
}
