package HUSPull;

/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */
public class UPosition {
    private int index;
    private double utility;

    public UPosition(int index, double utility) {
        this.index = index;
        this.utility = utility;
    }
    
    public int index() {
        return index;
    }
    
    public double utility() {
        return utility;
    }
    
    @Override
    public String toString() {
        String ret = "";
        ret += "(index: " + index + "  utility: " + utility + ")";
        return ret;
    }
}
