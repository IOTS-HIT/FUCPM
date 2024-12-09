import HUCPMiner.AlgoHUCPMiner;

import java.io.IOException;

/**
 * Test the HUCPMiner algorithm.
 */
public class MainTestHUCPMiner {

    public static void main(String [] arg) throws IOException{

        //datasets
        String[] datasets = {"BIBLE"/*,"Leviathan","BIBLE",","MSNBC","kosarak10k","BMS","FIFA","Scalability_80K"*/};

        //threshold
        double [][]minutil = {
                {0.00024},
        };

        int index = 0;
        // run the algorithm
        for(String s: datasets) {
            // the input database
            String input = "input/" + s + ".txt";
            for (double i : minutil[index]) {
                AlgoHUCPMiner algo = new AlgoHUCPMiner();
                // set the maximum pattern length (optional)
                algo.setMaxPatternLength(1000);
                // the path for saving the patterns found
                String output = "output/HUCPMiner_"+s+"_" + i + ".txt";
                algo.runAlgorithm(input, output, i);
                // print statistics
                algo.printStatistics();
            }
            index++;
        }
    }
}
