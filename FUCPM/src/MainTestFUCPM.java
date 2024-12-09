import FUCPM.AlgoFUCPM;

import java.io.IOException;

/**
 * Test the FUCPM algorithm.
 */
public class MainTestFUCPM {

    public static void main(String [] arg) throws IOException{

        //datasets
        String[] datasets = {"kosarak10k"/*,"Leviathan","BIBLE","MSNBC","kosarak10k","BMS","FIFA","Scalability_80K"*/};

        //threshold
        double [][]minutil = {
                {0.0001},
        };

        int index = 0;
        // run the algorithm
        for(String s: datasets) {
            // the input database
            String input = "input/" + s + ".txt";
            for (double i : minutil[index]) {
                //AlgoFUCPM algo = new AlgoFUCPM();
                AlgoFUCPM algo = new AlgoFUCPM();
                // set the maximum pattern length (optional)
                algo.setMaxPatternLength(1000);
                // the path for saving the patterns found
                String output = "output/FUCPM_"+s+"_" + i + ".txt";
                algo.runAlgorithm(input, output, i);
                // print statistics
                algo.printStatistics();
            }
            index++;
        }
    }
}
