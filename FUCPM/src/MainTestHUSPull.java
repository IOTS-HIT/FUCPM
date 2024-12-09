import HUSPull.HUSPull;

import java.io.IOException;

/**
 * Test the HUSPull algorithm.
 */
public class MainTestHUSPull {
    public static void main(String[] args) throws IOException {

        // minimum utility threshold ratio
        double minUtilityRatio = 0.00024;

        String dataset = "BIBLE";
    	String input = "input/"+dataset+".txt";
    	String output = "output/HUSPull_"+dataset+"_"+minUtilityRatio+".txt";

        // output the parameters

        System.out.println("test dataset: " + input);
        System.out.println("minUtilityRatio: " + String.format("%.5f", minUtilityRatio));
        
        // run the algorithm
        HUSPull huspMiner = new HUSPull(input, minUtilityRatio, output);
        huspMiner.runAlgo();
        
        // print statistics
        huspMiner.printStatistics();
        //System.out.println(huspMiner.toString());
    }
}
