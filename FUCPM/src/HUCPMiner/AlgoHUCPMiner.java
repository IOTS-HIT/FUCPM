/**
 * Copyright (C), 2015-2021, HITSZ
 * FileName: AlgoHUCPMiner
 * Description: The implementation of HUCPMiner algorithm proposed in "AN EFFICIENT ALGORITHM FOR MINING HIGH UTILITY
 * CONTIGUOUS PATTERNS FROM SOFTWARE EXECUTING TRACES".
 * Pruning strategy: SWU + RUUB
 * Data structure: UL-list
 */
package HUCPMiner;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AlgoHUCPMiner {

    /** the time the algorithm started */
    double startTimestamp = 0;
    /** the time the algorithm terminated */
    double endTimestamp = 0;
    /** the number of patterns generated */
    int patternCount = 0;

    /** writer to write the output file **/
    BufferedWriter writer = null;

    /** buffer for storing the current pattern that is mined when performing mining
     * the idea is to always reuse the same buffer to reduce memory usage. **/
    final int BUFFERS_SIZE = 2000;
    private int[] patternBuffer = null;

    /** if true, debugging information will be shown in the console */
    final int DEBUG = 0; //1:SWU, 2:ULL of 1-seq, 3:Utility and RUUB of 1-seq, 4:projected ULL, 5:writeout function

    /** if true, save result to file in a format that is easier to read by humans **/
    final boolean SAVE_RESULT_EASIER_TO_READ_FORMAT = false;

    /** the minUtility threshold **/
    double minUtility = 0;

    /** max pattern length **/
    int maxPatternLength = 1000;

    /** the input file path **/
    String input;

    // the number of Candidate
    int NumOfCandidate = 0;

    boolean iswriteout = false;
    /**
     * Default constructor
     */
    public AlgoHUCPMiner() {
    }

    /**
     * Run the HUCPMiner algorithm
     * @param input the input file path
     * @param output the output file path
     * @throws IOException exception if error while writing the file
     */
    public void runAlgorithm(String input, String output, double utilityratio) throws IOException {
        // reset maximum
        MemoryLogger.getInstance().reset();

        // input path
        this.input = input;

        // initialize the buffer for storing the current pattern
        patternBuffer = new int[BUFFERS_SIZE];

        // record the start time of the algorithm
        startTimestamp = System.currentTimeMillis();

        // create a writer object to write results to file
        writer = new BufferedWriter(new FileWriter(output));

        // for storing the current sequence number
        int NumberOfSequence = 0;

        // for storing the utility of all sequence
        int totalUtility = 0;

        BufferedReader myInput = null;
        String thisLine;

        /***** CONSTRUCT ULList of 1-sequences, calculate utility and RUUB of them *****/
        // ULL of 1-seq
        Map<Integer,ULList > mapitemULL = new HashMap<>();
        //for storing the global utility, RUUB and SWU of each 1-sequence
        Map<Integer,Integer> mapItemUtility = new HashMap<Integer,Integer>();
        Map<Integer,Integer> mapItemRUUB = new HashMap<Integer,Integer>();
        Map<Integer,Integer> mapItemSWU = new HashMap<Integer,Integer>();

        /***** First Scan, calculate SWU of 1-seqs *****/
        try{
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }
                //local SWU of 1-seq in current q-seq
                Map<Integer, Integer> mapItemS = new HashMap<Integer, Integer>();
                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");
                // get the sequence utility (the last token on the line)
                String sequenceUtilityString = tokens[tokens.length-1];
                int positionColons = sequenceUtilityString.indexOf(':');
                int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons + 1));
                for(int i = 0; i < tokens.length - 4; i++) {  //-2与SUtility间隔2个空格，tokens.length-4
                    String currentToken = tokens[i];
                    // if empty, continue to next token
                    if (currentToken.length() == 0) {
                        continue;
                    }
                    if (currentToken.equals("-1")) {
                        continue;
                    } else {
                        // We will extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        Integer item = Integer.parseInt(itemString);
                        if(mapItemS.get(item) == null){
                            mapItemS.put(item,sequenceUtility);
                        }
                    }
                }
                //update global SWU
                for(Entry<Integer,Integer> entry: mapItemS.entrySet()){
                    int item = entry.getKey();
                    if(mapItemSWU.get(item) == null){
                        mapItemSWU.put(item,entry.getValue());
                    }else{
                        mapItemSWU.put(item,entry.getValue() + mapItemSWU.get(item));
                    }
                }
                //更新总效用
                totalUtility += sequenceUtility;
            }
        }catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                // close the input file
                myInput.close();
            }
            if(DEBUG==1) {
                for (Entry<Integer, Integer> entry : mapItemSWU.entrySet()) {
                    System.out.println("SWU:");
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
            }
        }//Finish first scan.

        //set minimum utility threshold
        minUtility = utilityratio * totalUtility;
        System.out.println("utilityratio：" + utilityratio + " Threshold："+minUtility);

        //calculate the number of unpromising items
        int NumOfUnpromisingItem = 0;
        for(Entry<Integer, Integer> entry : mapItemSWU.entrySet()){
            if(entry.getValue() < minUtility) NumOfUnpromisingItem++;
        }

        /***** Second scan, construct ULList of promising 1-seqs *****/
        try {
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));

            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                //for storing utility and RUUB of each 1-sequence in current transaction
                Map<Integer,Integer> mapItemU = new HashMap<Integer,Integer>();
                Map<Integer,Integer> mapItemR = new HashMap<Integer,Integer>();

                //if previousItem == -3, then we are processing the first item of current q-seq.
                int previousItem = -3;
                //the utility and rest utility of the previous item
                int previousItemUtility = 0;
                int previousItemRu = 0;

                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");

                // get the sequence utility (the last token on the line)
                String sequenceUtilityString = tokens[tokens.length - 1];
                int positionColons = sequenceUtilityString.indexOf(':');
                int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons+1));

                // This variable will count the number of itemsets

                int nbItemsets = 0;

                //record the rest utility
                int restUtility = sequenceUtility;

                // For each token on the line except the last three tokens (the -1 -2 and SUtility).
                for(int i = 0; i < tokens.length - 4; i++) {
                    String currentToken = tokens[i];
                    // if empty, continue to next token
                    if(currentToken.length() == 0) {
                        continue;
                    }
                    // if the current token is -1 ,the ending sign of an itemset
                    if(currentToken.equals("-1")) {
                        // we update the number of itemsets in that sequence that are not empty
                        nbItemsets++;
                    }else {
                        //We need to finish the following two tasks if the current token is an item
                        // 1. construct ULL of 1-seq; 2. calculate utility and RUUB of 1-seq

                        //task 1: construct ULL of 1-seq
                        // We will extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        Integer item = Integer.parseInt(itemString);

                        // We also extract the utility from the string:
                        String utilityString = currentToken.substring(positionLeftBracketString + 1, positionRightBracketString);
                        Integer itemUtility = Integer.parseInt(utilityString);
                        //update rest utility
                        restUtility -= itemUtility;

                        //add an instance of previousItem to its ULL
                        //if previousItem != -3, then we are processing the items after the first item
                        if (previousItem != -3) {
                            //if previousItem is promising, then add an instance to ULL of previousItem
                            if (mapItemSWU.get(previousItem) >= minUtility) {
                                if (mapitemULL.get(previousItem) == null) {
                                    mapitemULL.put(previousItem, new ULList());
                                }
                                //if item is promising, set the nextitem field as item
                                if (mapItemSWU.get(item) >= minUtility) {
                                    mapitemULL.get(previousItem).add(NumberOfSequence, nbItemsets - 1, previousItemUtility, previousItemRu, item);
                                } else {
                                    //if item is unpromising, set the nextitem field as -4. (-4 represents null)
                                    mapitemULL.get(previousItem).add(NumberOfSequence, nbItemsets - 1, previousItemUtility, previousItemRu, -4);
                                }
                            }
                        }
                        //update previousItem by current item
                        previousItem = item;
                        previousItemUtility = itemUtility;
                        previousItemRu = restUtility;

                        //Task 2: Calculate utility and RUUB of 1-seq
                        if (mapItemSWU.get(item) >= minUtility) {
                            int tempRUUB = 0;
                            // if rest utility == 0 then RUUB=0
                            if (tokens[i + 2].equals("-2")) {
                                tempRUUB = 0;
                            } else {
                                tempRUUB = itemUtility + restUtility;
                            }
                            //if the item appears in the current transaction for the first time
                            if (mapItemU.get(item) == null) {
                                mapItemU.put(item, itemUtility);
                                mapItemR.put(item, tempRUUB);
                            } else {
                                if (itemUtility > mapItemU.get(item)) {
                                    mapItemU.put(item, itemUtility);
                                }
                                if (tempRUUB > mapItemR.get(item)) {
                                    mapItemR.put(item, tempRUUB);
                                }
                            }
                        }
                    }
                } //Finish processing all tokens

                //process the last item of the q-seq
                if (mapItemSWU.get(previousItem) >= minUtility){
                    if (mapitemULL.get(previousItem) == null) {
                        mapitemULL.put(previousItem, new ULList());
                    }
                    mapitemULL.get(previousItem).add(NumberOfSequence, nbItemsets, previousItemUtility, previousItemRu, -4);
                }

                //Update global variables mapItemUtility, mapItemRUUB according to mapItemU and mapItemR
                //update mapItemUtility
                for(Entry<Integer,Integer> entry: mapItemU.entrySet()){
                    int item = entry.getKey();
                    if(mapItemUtility.get(item) == null){
                        mapItemUtility.put(item,entry.getValue());
                    }else{
                        mapItemUtility.put(item,entry.getValue() + mapItemUtility.get(item));
                    }
                }
                //update mapItemRUUB
                for(Entry<Integer,Integer> entry: mapItemR.entrySet()){
                    int item = entry.getKey();
                    if(mapItemRUUB.get(item)==null){
                        mapItemRUUB.put(item,entry.getValue());
                    }else{
                        mapItemRUUB.put(item,entry.getValue() + mapItemRUUB.get(item));
                    }
                }

                // update the number of transactions
                NumberOfSequence++;
            } // finish scaning a sequence

            // if in debug mode, we print the ULL that we have just built
            if(DEBUG == 2) {
                for (Entry<Integer,ULList> entry: mapitemULL.entrySet()){
                    System.out.println("item:"+entry.getKey());
                    HashMap<Integer, HashMap<Integer, ULList.ULLElement>> templist = entry.getValue().ullist; //<sid,<index,ullelement>>
                    for(Entry<Integer,HashMap<Integer, ULList.ULLElement>> entry1 : templist.entrySet()){
                        for(Entry<Integer, ULList.ULLElement>entry2 : entry1.getValue().entrySet()){
                            System.out.print("sid:" + entry1.getKey());
                            System.out.print("  index:" + entry2.getKey());
                            System.out.print("  iutil:" + entry2.getValue().iutil);
                            System.out.print("  ru:" + entry2.getValue().rutil);
                            System.out.println("  next:" + entry2.getValue().nextitem);
                        }
                    }
                    System.out.println("End of a ULL");
                }
            }

            // if in debug mode, we print the Utility and RUUB of each 1-sequence that we have just built
            if(DEBUG == 3) {
                System.out.println("RUUB:");
                for (Entry<Integer,Integer> entry: mapItemRUUB.entrySet()){
                    System.out.println(entry.getKey() + " : "+entry.getValue());
                }
                System.out.println("******");
                System.out.println("Utility:");
                for (Entry<Integer,Integer> entry: mapItemUtility.entrySet()){
                    System.out.println(entry.getKey() + " : "+entry.getValue());
                }
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                // close the input file
                myInput.close();
            }
        }//Finish loading data

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        // Mine the database recursively
        for (Entry<Integer,Integer> entry: mapItemSWU.entrySet()){

            //SWU pruning strategy
            if(entry.getValue() < minUtility) continue;

            int item = entry.getKey();
            patternBuffer[0] = item;
            patternBuffer[1] = -1;
            patternBuffer[2] = -2;

            //Update the count of the candidate
            NumOfCandidate++;
            //check whether 1-seq is high-utility
            if(mapItemUtility.get(item) >= minUtility){
                if(iswriteout) writeOut(patternBuffer,1,mapItemUtility.get(item));
                patternCount++;
            }

            //RUUB pruning strategy
            if (mapItemRUUB.get(item) >= minUtility) {
                HUCPMiner(patternBuffer, 1,1, mapItemSWU, mapitemULL, mapitemULL.get(item));
            }
        }

        double runtime = System.currentTimeMillis() - startTimestamp;
        StringBuilder buffer = new StringBuilder();
        buffer.append("============= HUCPMiner ALGORITHM v1.0 - STATS ==========\n");
        buffer.append(" Minimum utility threshold ~ " + minUtility + " \n");
        buffer.append(" Total time ~ " + runtime/1000 + " s\n");
        buffer.append(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB\n");
        buffer.append(" Number of unpromising items ~ " + NumOfUnpromisingItem + " \n");
        buffer.append(" Number of distinct items ~ " + mapItemSWU.size() + " \n");
        buffer.append(" Number of candidates ~ " + NumOfCandidate + " \n");
        buffer.append(" Number of High-utility sequential pattern ~ " + patternCount);
        writer.write(buffer.toString());
        writer.newLine();

        // check the memory usage again and close the file.
        MemoryLogger.getInstance().checkMemory();
        // close output file
        writer.close();
        // record end time
        endTimestamp = System.currentTimeMillis();
    }

    //	This inner class is used to store the information of candidates after concatenating the item.
    public class ItemConcatnation {
        // utility
        int utility;
        // RUUB
        int RUUB;
        // projected database ULList
        ULList ulList;
        // Candidate after concatenating the item
        public int[] candidate;
        // length of Candidate after concatenating the item
        int candidateLength;

        // Constructor
        public ItemConcatnation(int utility, int RUUB, ULList ulList, int[] candidate, int candidateLength){
            this.utility = utility;
            this.RUUB = RUUB;
            this.ulList = ulList;
            this.candidateLength = candidateLength;
            this.candidate = new int[BUFFERS_SIZE];
            System.arraycopy(candidate, 0, this.candidate, 0, candidateLength);
        }
    }

    /**
     * construct ULList of Candidate
     * @param exitemULL the ULList of the extension item. (Prefix + extension item = Candidate)
     * @param prefixULL the ULList of the prefix of Candidate
     */
    private ItemConcatnation ConstructUllist(int[] Candidate, int CandidateLength, ULList exitemULL, ULList prefixULL){
        //ULL of Candidate
        ULList CandidateULL = new ULList();
        // store utility and RUUB of Candidate
        int Utility = 0;
        int RUUB = 0;
        //local utility and local RUUB of Candidate in each q-seq
        Map<Integer,Integer > mapCanUtility = new HashMap<>();
        Map<Integer,Integer > mapCanRUUB = new HashMap<>();

        //ULList of Prefix and extension item
        HashMap<Integer, HashMap<Integer, ULList.ULLElement>> prefixList = prefixULL.ullist; //<sid,<index,ullelement>>
        HashMap<Integer, HashMap<Integer, ULList.ULLElement>> exitemList = exitemULL.ullist;
        //for each element of prefix's ULL
        for(Entry<Integer,HashMap<Integer, ULList.ULLElement>> prefixEntry1 : prefixList.entrySet()){
            for(Entry<Integer, ULList.ULLElement>prefixEntry2 : prefixEntry1.getValue().entrySet()){
                int sid = prefixEntry1.getKey();
                int index = prefixEntry2.getKey();
                int prefixIutil = prefixEntry2.getValue().iutil;
                //if sid of Prefix == sid of extension item, and index of Prefix == index of exitem - 1, then create a new element and add to the ULL of Candidate
                if(exitemList.get(sid) != null){
                    HashMap<Integer, ULList.ULLElement> exitemListWithSid = exitemList.get(sid);
                    if(exitemListWithSid.get(index + 1) != null){
                        int exitemIutil = exitemListWithSid.get(index + 1).iutil;
                        int exitemRutil = exitemListWithSid.get(index + 1).rutil;
                        int exitemNextitem = exitemListWithSid.get(index + 1).nextitem;
                        CandidateULL.add(sid,index + 1,prefixIutil + exitemIutil, exitemRutil, exitemNextitem);
                    }
                }
            }
        }

        //calculate local utility and local RUUB of Candidate
        for(Entry<Integer,HashMap<Integer, ULList.ULLElement>> canEntry1 : CandidateULL.ullist.entrySet()) { //prefixULL.ullist : <sid,<index,ullelement>>
            int sid = canEntry1.getKey();
            for (Entry<Integer, ULList.ULLElement> canEntry2 : canEntry1.getValue().entrySet()) {
                int currentRUUB = canEntry2.getValue().iutil + canEntry2.getValue().rutil;
                if(canEntry2.getValue().rutil == 0) currentRUUB = 0;
                //if Candidate appears in current q-seq for the first time, record its utility and RUUB directly
                if(mapCanUtility.get(sid) == null){
                    mapCanUtility.put(sid, canEntry2.getValue().iutil);
                    mapCanRUUB.put(sid, currentRUUB);
                }else{ //if Candidate has appeared already, choose the larger utility and RUUB
                    if(mapCanRUUB.get(sid) < currentRUUB){
                        mapCanRUUB.put(sid, currentRUUB);
                    }
                    if(mapCanUtility.get(sid) < canEntry2.getValue().iutil){
                        mapCanUtility.put(sid, canEntry2.getValue().iutil);
                    }
                }
            }
        }

        //update global RUUB and utility of Candidate
        for (Entry<Integer,Integer> entry : mapCanUtility.entrySet()) {
            Utility += entry.getValue();
        }
        for (Entry<Integer,Integer> entry : mapCanRUUB.entrySet()) {
            RUUB += entry.getValue();
        }

        // if in debug mode, we print the ULL that we have just built
        if(DEBUG == 4){
            System.out.println("**********************");
            System.out.print("Candidate: ");
            for (int i = 0; i < CandidateLength; i++){
                System.out.print(Candidate[i] + " ");
            }
            System.out.println();
            System.out.println("global RUUB:" + RUUB + " global utility:" + Utility);

            for(Entry<Integer,HashMap<Integer, ULList.ULLElement>> entry1 : CandidateULL.ullist.entrySet()){ //prefixULL.ullist : <sid,<index,ullelement>>
                for(Entry<Integer, ULList.ULLElement>entry2 : entry1.getValue().entrySet()){
                        System.out.print("sid:" + entry1.getKey());
                        System.out.print("  index:" + entry2.getKey());
                        System.out.print("  iutil:" + entry2.getValue().iutil);
                        System.out.print("  ru:" + entry2.getValue().rutil);
                        System.out.println("  next:" + entry2.getValue().nextitem);
                }
                System.out.println("End of a ULL");
            }
        }

        Candidate[CandidateLength] = -1;
        Candidate[CandidateLength + 1] = -2;

        //return the ItemConcatnation of Candidate
        return new ItemConcatnation(Utility, RUUB, CandidateULL, Candidate, CandidateLength);
    }


    /**
     * recursive pattern growth function
     * @param prefix prefix sequence
     * @param prefixLength length of prefix
     * @param itemCount number of items in prefix
     * @param mapItemSWU SWU of all items
     * @param mapItemULL ULList of all items
     * @param prefixULL ULList of prefix
     */
    private void HUCPMiner(int[] prefix, int prefixLength, int itemCount, Map<Integer,Integer> mapItemSWU, Map<Integer,ULList> mapItemULL, ULList prefixULL) throws IOException {
        //HUCPMiner can only handle q-sequence whose each itemset contains one item. So only s-extension can be done in HUCPMiner.
        Map<Integer, Integer> slist = new HashMap<>();

        /***** Construct slist *****/
        for(Entry<Integer,HashMap<Integer, ULList.ULLElement>> entry1 : prefixULL.ullist.entrySet()) { //prefixULL.ullist : <sid,<index,ullelement>>
            for (Entry<Integer, ULList.ULLElement> entry2 : entry1.getValue().entrySet()) {
                int nextitem = entry2.getValue().nextitem;
                if(nextitem != -4){
                    slist.put(nextitem, mapItemSWU.get(nextitem));
                }
            }
        }

        //for temporarily storing information of candidates
        ItemConcatnation ItemCom;

        /***** S-CONCATENATIONS *****/
        // perform S-Concatenations to grow the pattern larger.
        for (Entry<Integer,Integer> entry : slist.entrySet()){

            //SWU pruning strategy
            if(entry.getValue() < minUtility) continue;
            int item = entry.getKey();

            //Update the number of the candidate
            NumOfCandidate++;

            //new candidate sequence
            prefix[prefixLength] = -1;
            prefix[prefixLength + 1] = item;

            //call the function to construct ULL of the candidate
            if (itemCount + 1 <= maxPatternLength){
                ItemCom = ConstructUllist(prefix,prefixLength + 2, mapItemULL.get(item), prefixULL);

                //check whether candidate is high-utility
                if(ItemCom.utility >= minUtility){
                    if(iswriteout) writeOut(ItemCom.candidate,ItemCom.candidateLength,ItemCom.utility);
                    patternCount++;
                }
                // RUUB pruning strategy
                if (ItemCom.RUUB >= minUtility)
                    //Mine the database recursively
                    HUCPMiner(ItemCom.candidate, ItemCom.candidateLength, itemCount + 1, mapItemSWU, mapItemULL, ItemCom.ulList);
            }
        }

        // We check the memory usage
        MemoryLogger.getInstance().checkMemory();
    }

    /**
     * Set the maximum pattern length
     * @param maxPatternLength the maximum pattern length
     */
    public void setMaxPatternLength(int maxPatternLength) {
        this.maxPatternLength = maxPatternLength;
    }

    /**
     * Method to write a high utility itemset to the output file.
     //* @param the prefix to be written o the output file
     * @param utility the utility of the prefix concatenated with the item
     * @param prefixLength the prefix length
     */
    private void writeOut(int[] prefix, int prefixLength,  int utility) throws IOException {
        // increase the number of high utility itemsets found
        //patternCount++;

        StringBuilder buffer = new StringBuilder();

        // If the user wants to save in SPMF format
        if(SAVE_RESULT_EASIER_TO_READ_FORMAT == false) {
            // append each item of the pattern
            for (int i = 0; i < prefixLength; i++) {
                buffer.append(prefix[i]);
                buffer.append(' ');
            }

            // append the end of itemset symbol (-1) and end of sequence symbol (-2)
            buffer.append("-1 #UTIL: ");
            // append the utility of the pattern
            buffer.append(utility);
        }
        else {
            // Otherwise, if the user wants to save in a format that is easier to read for debugging.
            // Append each item of the pattern
            buffer.append('<');
            buffer.append('(');
            for (int i = 0; i < prefixLength; i++) {
                if(prefix[i] == -1) {
                    buffer.append(")(");
                }else {
                    buffer.append(prefix[i]);
                }
            }
            buffer.append(")>:");
            buffer.append(utility);
        }

        // write the pattern to the output file
        writer.write(buffer.toString());
        writer.newLine();

        // if in debugging mode, then also print the pattern to the console
        if(DEBUG==5) {
            System.out.println(" SAVING : " + buffer.toString());
            System.out.println();

            // check if the calculated utility is correct by reading the file
            // for debugging purpose
            checkIfUtilityOfPatternIsCorrect(prefix, prefixLength, utility);
        }
    }

    /**
     * This method check if the utility of a pattern has been correctly calculated for
     * debugging purposes. It is not designed to be efficient since it is just used for
     * debugging.
     * @param prefix a pattern stored in a buffer
     * @param prefixLength the pattern length
     * @param utility the utility of the pattern
     * @throws IOException if error while writting to file
     */
    private void checkIfUtilityOfPatternIsCorrect(int[] prefix, int prefixLength, int utility) throws IOException {
        int calculatedUtility = 0;

        BufferedReader myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
        // we will read the database
        try {
            // prepare the object for reading the file

            String thisLine;
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");

                int tokensLength = tokens.length -3;

                int[] sequence = new int[tokensLength];
                int[] sequenceUtility = new int[tokensLength];

                // Copy the current sequence in the sequence buffer.
                // For each token on the line except the last three tokens
                // (the -1 -2 and sequence utility).
                for(int i=0; i< tokensLength; i++) {
                    String currentToken = tokens[i];

                    // if empty, continue to next token
                    if(currentToken.length() == 0) {
                        continue;
                    }

                    // read the current item
                    int item;
                    int itemUtility;

                    // if the current token is -1
                    if(currentToken.equals("-1")) {
                        item = -1;
                        itemUtility = 0;
                    }else {
                        // if  the current token is an item
                        //  We will extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        item = Integer.parseInt(itemString);

                        // We also extract the utility from the string:
                        String utilityString = currentToken.substring(positionLeftBracketString+1, positionRightBracketString);
                        itemUtility = Integer.parseInt(utilityString);
                    }
                    sequence[i] = item;
                    sequenceUtility[i] = itemUtility;
                }

                // For each position of the sequence
                int util = tryToMatch(sequence,sequenceUtility, prefix, prefixLength, 0, 0, 0);
                calculatedUtility += util;
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                // close the input file
                myInput.close();
            }
        }

        if(calculatedUtility != utility) {
            System.out.print(" ERROR, WRONG UTILITY FOR PATTERN : ");
            for(int i=0; i<prefixLength; i++) {
                System.out.print(prefix[i]);
            }
            System.out.println(" utility is: " + utility + " but should be: " + calculatedUtility);
            System.in.read();
        }
    }

    /**
     * This is some code for verifying that the utility of a pattern is correctly calculated
     * for debugging only. It is not efficient. But it is a mean to verify that
     * the result is correct.
     * @param sequence a sequence (the items and -1)
     * @param sequenceUtility a sequence (the utility values and -1)
     * @param prefix the current pattern stored in a buffer
     * @param prefixLength the current pattern length
     * @param prefixPos the position in the current pattern that we will try to match with the sequence
     * @param seqPos the position in the sequence that we will try to match with the pattenr
     * @param utility the calculated utility until now
     * @return the utility of the pattern
     */
    private int tryToMatch(int[] sequence, int[] sequenceUtility, int[] prefix,	int prefixLength,
                           int prefixPos, int seqPos, int utility) {

        // Note: I do not put much comment in this method because it is just
        // used for debugging.

        List<Integer> otherUtilityValues = new ArrayList<Integer>();

        // try to match the current itemset of prefix
        int posP = prefixPos;
        int posS = seqPos;

        int previousPrefixPos = prefixPos;
        int itemsetUtility = 0;
        while(posP < prefixLength & posS < sequence.length) {
            if(prefix[posP] == -1 && sequence[posS] == -1) {
                posS++;

                // try to skip the itemset in prefix
                int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                otherUtilityValues.add(otherUtility);

                posP++;
                utility += itemsetUtility;
                itemsetUtility = 0;
                previousPrefixPos = posP;
            }else if(prefix[posP] == -1) {
                // move to next itemset of sequence
                while(posS < sequence.length && sequence[posS] != -1){
                    posS++;
                }

                // try to skip the itemset in prefix
                int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                otherUtilityValues.add(otherUtility);

                utility += itemsetUtility;
                itemsetUtility = 0;
                previousPrefixPos = posP;

            }else if(sequence[posS] == -1) {
                posP = previousPrefixPos;
                itemsetUtility = 0;
                posS++;
            }else if(prefix[posP] == sequence[posS]) {
                posP++;
                itemsetUtility += sequenceUtility[posS];
                posS++;
                if(posP == prefixLength) {

                    // try to skip the itemset in prefix
                    // move to next itemset of sequence
                    while(posS < sequence.length && sequence[posS] != -1){
                        posS++;
                    }
                    int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                    otherUtilityValues.add(otherUtility);


                    utility += itemsetUtility;
                }
            }else if(prefix[posP] != sequence[posS]) {
                posS++;
            }
        }

        int max = 0;
        if(posP == prefixLength) {
            max = utility;
        }
        for(int utilValue : otherUtilityValues) {
            if(utilValue > utility) {
                max = utilValue;
            }
        }
        return max;
    }

    /**
     * Print statistics about the latest execution to System.out.
     */
    public void printStatistics() {
        System.out.println("=============  HUCPMiner ALGORITHM v1.0 - STATS  ==========");
        System.out.println(" Total time ~ " + (endTimestamp - startTimestamp)/1000 + " s");
        System.out.println(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Number Of Candidate : " + NumOfCandidate);
        System.out.println(" High-utility sequential pattern count : " + patternCount);
        System.out.println("========================================================");
    }
}