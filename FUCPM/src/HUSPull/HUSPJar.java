package HUSPull;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by labwin on 2017/11/4.
 */
public class HUSPJar {
    static double maxConsumptionMemory;
    static double initConsumptionMemory;
    static double difConsumptionMemory;
    static boolean isTestMem;

    public static void main(String[] args) throws IOException {
        String algo;
        String pathname;
        double threshold;
        double current;

        algo = args[0];
        pathname = args[1];
        threshold = Double.parseDouble(args[2]);

        // algorithm == "0"!!!
        if (algo.equals("HuspMiner")) {
            HUSPull huspMiner = new HUSPull(pathname, threshold,"");
            new Thread(new MemoryUpdateRunnable()).start();
            isTestMem = true;
            current = System.currentTimeMillis();
            huspMiner.runAlgo();
            isTestMem = false;
            ArrayList<String> ret = huspMiner.getResults();
            ret.add("" + ((System.currentTimeMillis() - current) / 1000));
            ret.add("" + maxConsumptionMemory);
            ret.add("" + difConsumptionMemory);
            for (String item: ret)
                System.out.println(item);
        }
    }

    static class MemoryUpdateRunnable implements Runnable {
        public void run() {
            maxConsumptionMemory = Double.MIN_VALUE;
            initConsumptionMemory = Double.MIN_VALUE;
            difConsumptionMemory = Double.MIN_VALUE;
            while (isTestMem) {
                double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d
                        / 1024d;
                if (initConsumptionMemory == Double.MIN_VALUE) {
                    initConsumptionMemory = currentMemory;
                }
                if (currentMemory - initConsumptionMemory > difConsumptionMemory) {
                    difConsumptionMemory = currentMemory - initConsumptionMemory;
                }
                if (currentMemory > maxConsumptionMemory) {
                    maxConsumptionMemory = currentMemory;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    System.out.println("error InterruptedException");
                }
            }
            
            // System.out.println("Memory= " + maxConsumptionMemory + " M");
            // System.out.println(maxConsumptionMemory);
        }
    }
}
