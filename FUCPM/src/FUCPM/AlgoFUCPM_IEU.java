package FUCPM;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
/**
 * Copyright (C), 2015-2021, HITSZ
 * FileName: AlgoFUCPM
 * Description: FUCPM algorithm without IEU strategy.
 * Pruning strategy: Recurrent SWU.
 * Data structure: Sequence Infomation List and Instance List.
 */
public class AlgoFUCPM_IEU {

    /** the time the algorithm started */
    long startTimestamp = 0;
    /** the time the algorithm terminated */
    long endTimestamp = 0;
    /** the number of patterns generated */
    int patternCount = 0;

    /** writer to write the output file **/
    BufferedWriter writer = null;

    /** buffer for storing the current pattern that is mined when performing mining
     * the idea is to always reuse the same buffer to reduce memory usage. **/
    final int BUFFERS_SIZE = 2000;
    private int[] patternBuffer = null;

    /** if true, debugging information will be shown in the console */
    //1:SWU, 2:recurrent SWU, 3:SIL, 4:InstanceChain of 1-seq, 5:utility of 1-seq, 6:projected InstanceChain, 7:prefix utility, 8:writeout function
    final int DEBUG = 0;

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

    /**Default constructor**/
    public AlgoFUCPM_IEU() {
    }

    /**
     * Run the FUCPM algorithm
     * @param input the input file path
     * @param output the output file path
     * @param utilityratio minimum utility threshold ratio
     * @throws IOException exception if error while writing the file
     */
    public void runAlgorithm(String input, String output, double utilityratio) throws IOException {
        // reset MemoryLogger
        MemoryLogger.getInstance().reset();

        // input path
        this.input = input;

        // initialize the buffer for storing the current itemset
        patternBuffer = new int[BUFFERS_SIZE];

        // record the start time of the algorithm
        startTimestamp = System.currentTimeMillis();

        // create a writer object to write results to file
        writer = new BufferedWriter(new FileWriter(output));

        // for storing the current sequence number
        int NumberOfSequence = 0;

        // for storing the utility of all sequences
        int totalUtility = 0;

        BufferedReader myInput = null;
        String thisLine;

        // the database of SIL of each sequence
        List<SeqInfoList> dataset = new ArrayList<>();
        // for storing the instanceChain (composed of instanceLists) of each 1-sequence
        Map<Integer,ArrayList<InstanceList>> mapItemIC = new HashMap<>();

        //for storing the global utility and SWU of each 1-sequence
        Map<Integer,Integer> mapItemUtility = new HashMap<>();
        Map<Integer,Integer> mapItemSWU = new HashMap<>();

        //record the utility of each q-seq
        ArrayList<Integer> qSeqUtility = new ArrayList();

        //record the distinct items contained by each q-seq
        HashMap<Integer, HashSet<Integer>> qSeqContainItem = new HashMap<>();
        //record the sum utility of an item in a q-seq. <key:item, value:<key:sid,value:sum utility>>
        HashMap<Integer,HashMap<Integer,Integer>> mapItemSumUtility = new HashMap<>();

        /***** First scan, calculate the initial SWU of each item, and the sum utility of each item in each q-seq *****/
        try{
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
            //q-seq id
            int Sid = 0;
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }
                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");
                // get the sequence utility (the last token on the line)
                String sequenceUtilityString = tokens[tokens.length-1];
                int positionColons = sequenceUtilityString.indexOf(':');
                int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons+1));
                //record the utility of this q-seq
                qSeqUtility.add(sequenceUtility);
                //handle each token in this q-seq
                for(int i=0; i< tokens.length - 4; i++) {  //-2与SUtility间隔为2个空格，所以tokens.length-4
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
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        Integer item = Integer.parseInt(itemString);
                        String utilityString = currentToken.substring(positionLeftBracketString+1, positionRightBracketString);
                        Integer itemUtility = Integer.parseInt(utilityString);
                        //add the item to qSeqContainItem
                        if(qSeqContainItem.get(Sid) == null){
                            qSeqContainItem.put(Sid, new HashSet<>());
                        }
                        qSeqContainItem.get(Sid).add(item);

                        //record the sum utility of the item in this q-seq
                        if(mapItemSumUtility.get(item) ==null){ //if it is the first occurrence of item in the database
                                HashMap<Integer,Integer> innermap = new HashMap<>();
                                innermap.put(Sid,itemUtility);
                                mapItemSumUtility.put(item,innermap);
                        }else{
                            HashMap<Integer,Integer> innermap = mapItemSumUtility.get(item);
                            if(innermap.get(Sid) == null){ //if it is the first occurrence of item in this q-seq
                                innermap.put(Sid,itemUtility);
                            }else{
                                innermap.put(Sid,innermap.get(Sid) + itemUtility);
                            }
                        }
                    }
                }

                //update global SWU of each item
                for(Integer item : qSeqContainItem.get(Sid)){
                    if(mapItemSWU.get(item)==null){
                        mapItemSWU.put(item,sequenceUtility);
                    }else{
                        mapItemSWU.put(item,sequenceUtility + mapItemSWU.get(item));
                    }
                }
                Sid++;
                //update total utility
                totalUtility+=sequenceUtility;
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
        }//First scan finished.

        //set minimum utility threshold
        minUtility = utilityratio * totalUtility;
        System.out.println("utilityratio："+utilityratio+" Threshold："+minUtility);

        //the set of unpromising items
        HashSet<Integer> unpromisingItems = new HashSet<>();

        /***** Calculate Recurrent SWU *****/
        //Recurrent SWU: calculate the SWU of each item recurrently.
        //The utility of unpromising items is set to zero, so the utility of each q-seq may reduce. Thus, SWU of each item may also reduce.
        while(true){
            boolean flag = false; //record whether new unpromising item produces. true: a new unpromising item produces.
            for(Entry<Integer, Integer> entry : mapItemSWU.entrySet()){
                int item = entry.getKey();
                int itemSwu = entry.getValue();
                if(itemSwu < minUtility && unpromisingItems.contains(item)==false){
                    flag = true;
                    unpromisingItems.add(item);
                    //update utility of the q-seq which contains this unpromising item
                    for(Entry<Integer,Integer> element : mapItemSumUtility.get(item).entrySet()){
                        //id of the q-seq which contains this unpromising item
                        int sid = element.getKey();
                        //sum utility of the unpromising item in sid
                        int sumUtility = element.getValue();
                        //update utility of this q-seq
                        qSeqUtility.set(sid, qSeqUtility.get(sid) - sumUtility);
                        //update SWU of the items contained in this q-seq
                        for(int distinctItem: qSeqContainItem.get(sid)){
                            mapItemSWU.put(distinctItem, mapItemSWU.get(distinctItem) - sumUtility);
                        }
                    }
                }
            }
            if(flag == false) break;
        }

        System.out.println("Num of unpromising items:" + unpromisingItems.size() + " Num of all distinct items:" + mapItemSWU.size());

        if(DEBUG==2) {
            System.out.println("unpromising items:");
            for(int item : unpromisingItems){
                System.out.println(item+" ");
            }
            System.out.println("Recurrent SWU:");
            for (Entry<Integer, Integer> entry : mapItemSWU.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            System.out.println("q-seq utility:");
            for (int i=0; i<qSeqUtility.size(); i++) {
                System.out.println(i + ": " + qSeqUtility.get(i));
            }
        }

        /***** Second Scan, Construct the SeqInfoList, and InstanceList of promising 1-sequences (i.e. promising items) ******/
        try {
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));

            // We will read each sequence in buffers.
            // The first buffer will store the items of a sequence and the -1 between them)
            int[] itemBuffer = new int[BUFFERS_SIZE];
            // The second buffer will store the utility of items in a sequence and the -1 between them)
            int[] utilityBuffer = new int[BUFFERS_SIZE];
            // The following variable will contain the length of the data stored in the two previous buffer
            int itemBufferLength;

            // for each line (q-seq) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                // for storing the instanceList of each 1-sequence in the current q-seq
                Map<Integer, InstanceList> mapItemIL = new HashMap<>();

                //for storing utility of each 1-sequence in the current q-seq
                Map<Integer,Integer> mapItemU = new HashMap<>();

                // We reset the following buffer length to zero because we are reading a new sequence.
                itemBufferLength = 0;

                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");

                // get the sequence utility
                int sequenceUtility = qSeqUtility.get(NumberOfSequence);

                // the number of itemsets
                int nbItemsets = 1;

                // For each token on the line except the last three tokens (the -1 -2 and SUtility).
                for(int i=0; i< tokens.length - 4; i++) {  //-2与SUtility间隔为2个空格，所以tokens.length-4
                    String currentToken = tokens[i];
                    // if empty, continue to next token
                    if(currentToken.length() == 0) {
                        continue;
                    }
                    // if the current token is -1 ,the ending sign of an itemset
                    if(currentToken.equals("-1")) {
                        // We store the -1 in the respective buffers
                        itemBuffer[itemBufferLength] = -1;
                        utilityBuffer[itemBufferLength] = -1;
                        // We increase the length of the data stored in the buffers
                        itemBufferLength++;

                        // we update the number of itemsets in that sequence that are not empty
                        nbItemsets++;
                    }else {
                        //We need to finish the following three tasks if the current token is an item

                        /* Task 1: record the utility for constructing the SIL later */
                        // extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        Integer item = Integer.parseInt(itemString);
                        // extract the utility from the string:
                        String utilityString = currentToken.substring(positionLeftBracketString + 1, positionRightBracketString);
                        Integer itemUtility = Integer.parseInt(utilityString);
                        // if the item is unpromising, we set its utility to 0
                        if (unpromisingItems.contains(item) == true) {
                            itemUtility = 0;
                        }
                        // We store the item and its utility in the buffers for temporarily storing the sequence
                        itemBuffer[itemBufferLength] = item;
                        utilityBuffer[itemBufferLength] = itemUtility;
                        itemBufferLength++;

                        // If the 1-seq is promising
                        if (itemUtility != 0) {
                            /* Task 2: Construct InstanceList of promising 1-seq */
                            // if the promising item appears in the current q-seq for the first time
                            if (mapItemIL.get(item) == null) {
                                InstanceList tempUL = new InstanceList();
                                tempUL.set_sid(NumberOfSequence);
                                tempUL.add(nbItemsets - 1, itemUtility);
                                mapItemIL.put(item, tempUL);
                            }else {
                                InstanceList tempUL = mapItemIL.get(item);
                                tempUL.add(nbItemsets - 1, itemUtility);
                                mapItemIL.put(item, tempUL);
                            } //mapItemIL: for storing the instanceList of each 1-sequence in the current q-seq

                            /* Task 3: Calculate utility of promising 1-seq */
                            // if the promising item appears in the current q-seq for the first time
                            if (mapItemU.get(item) == null) {
                                mapItemU.put(item, itemUtility);
                            } else {
                                if (itemUtility > mapItemU.get(item)) {
                                    mapItemU.put(item, itemUtility);
                                }
                            }
                        }
                    }
                }

                //Update global variables mapItemUtility and mapItemIC according to mapItemU and mapItemIL
                //update mapItemUtility
                for(Entry<Integer,Integer> entry: mapItemU.entrySet()){
                    int item = entry.getKey();
                    if(mapItemUtility.get(item) == null){
                        mapItemUtility.put(item,entry.getValue());
                    }else{
                        mapItemUtility.put(item,entry.getValue() + mapItemUtility.get(item));
                    }
                }
                //update mapItemIC
                for(Entry<Integer, InstanceList> entry: mapItemIL.entrySet()){
                    int item = entry.getKey();
                    ArrayList<InstanceList> tempChain = new ArrayList<InstanceList>();
                    if(mapItemIC.get(item) != null)
                        tempChain = mapItemIC.get(item);
                    tempChain.add(entry.getValue());
                    mapItemIC.put(item,tempChain);
                }

                // create the SIL for current sequence
                SeqInfoList seqinfoList = new SeqInfoList(nbItemsets);

                // This variable will represent the position in the q-sequence
                int posBuffer = 0;
                // for each itemset
                for(int itemset = 0; itemset < nbItemsets; itemset++) {
                    while(posBuffer < itemBufferLength ) {
                        // Get the item at the current position in the sequence
                        int item = itemBuffer[posBuffer];

                        // if it is an itemset separator, we move to next position in the sequence
                        if (item == -1) {
                            posBuffer++;
                            break;
                        }
                        // else if it is an item
                        else {
                            // get the utility of the item
                            int utility = utilityBuffer[posBuffer];
                            // We update the rest utility by subtracting the utility of the current item
                            sequenceUtility -= utility;
                            // add the item to SIL of current q-seq if the item is promising
                            if (utility != 0) {
                                seqinfoList.registerItem(itemset, item, utility, sequenceUtility);
                            }
                            posBuffer++;
                        }
                    }
                } // SIL of the current q-seq has been built.

                // We add the SIL to the sequence database.
                dataset.add(seqinfoList);
                // if in debug mode, we print the SIL that we have just built
                if(DEBUG==3) {
                    System.out.println(seqinfoList.toString());
                    System.out.println();
                }

                // we update the number of sequences
                NumberOfSequence++;
            } // finish scaning a q-sequence each time through the loop

            // if in debug mode, we print the InstanceChain of each 1-seq
            if(DEBUG==4) {
                for (Entry<Integer,ArrayList<InstanceList>> entry: mapItemIC.entrySet()){
                    System.out.println("item:"+entry.getKey());
                    for(int i=0;i<entry.getValue().size();i++){
                        System.out.println(i+"-th InstanceList:");
                        for(int j=0;j<entry.getValue().get(i).insList.size();j++){
                            System.out.print(j+"-th element: ");
                            System.out.print("sid:"+entry.getValue().get(i).get_sid());
                            System.out.print("  tid:"+entry.getValue().get(i).insList.get(j).tid);
                            System.out.print("  acu:"+entry.getValue().get(i).insList.get(j).acu);
                        }
                        System.out.println("End of an InstanceList");
                    }
                    System.out.println("******");
                }
            }

            // if in debug mode, we print the utility of each 1-sequence
            if(DEBUG==5) {
                System.out.println("******");
                System.out.println("Utility:");
                for (Entry<Integer,Integer> entry: mapItemUtility.entrySet()){
                    System.out.println(entry.getKey()+" : "+entry.getValue());
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
        }//Second scan finished.

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        // Mine the database recursively using the FUCPM procedure
        for(Entry<Integer, Integer>entry : mapItemUtility.entrySet()){
            int item = entry.getKey();

            //RSWU pruning strategy
            if(mapItemSWU.get(item) < minUtility) continue;

            patternBuffer[0]= item;
            patternBuffer[1] = -1;
            patternBuffer[2] = -2;

            NumOfCandidate++;
            //check whether the 1-seq is high-utility
            if(entry.getValue() >= minUtility){
                //writeOut(patternBuffer,1,mapItemUtility.get(item));
                patternCount++;
            }
            // recursively mine the database
            FUCPM(patternBuffer, 1, dataset, mapItemIC.get(item),1);
        }

        long runtime = System.currentTimeMillis() - startTimestamp;
        StringBuilder buffer = new StringBuilder();
        buffer.append("=============  FUCPM_IEU ALGORITHM v1.0 - STATS ==========\n");
        buffer.append(" Minimum utility threshold ~ " + minUtility + " \n");
        buffer.append(" Total time ~ " + runtime + " ms\n");
        buffer.append(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB\n");
        buffer.append(" Number of unpromising items ~ " + unpromisingItems.size() + " \n");
        buffer.append(" Number of distinct items ~ " + mapItemSWU.size() + " \n");
        buffer.append(" Number of candidates ~ " + NumOfCandidate + " \n");
        buffer.append(" Number of high-utility sequential pattern ~ " + patternCount);
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
    public class ItemConcatenation {
        // utility
        int utility;
        // projected database InstanceChain
        ArrayList<InstanceList> IChain;
        // Candidate sequence after concatenating the item
        public int[] candidate;
        // length of candidate after concatenating the item
        int candidateLength;

        // Constructor
        public ItemConcatenation(int utility, ArrayList<InstanceList> UChain, int[] candidate, int candidateLength){
            this.utility = utility;
            this.IChain = UChain;
            this.candidateLength = candidateLength;
            this.candidate = new int[BUFFERS_SIZE];
            System.arraycopy(candidate, 0, this.candidate, 0, candidateLength);
        }
    }

    /**
     * construct InstanceChain of candidates
     * @param Candidate candidate sequence
     * @param CandidateLength length of Candidate
     * @param database  SIL of all q-seq
     * @param instanceChain InstanceChain of the prefix of Candidate
     * @param kind 0:i-Concatenate，1:s-Concatenate
     */
    private ItemConcatenation ConstructInstanceChain(int[] Candidate, int CandidateLength, List<SeqInfoList> database, ArrayList<InstanceList> instanceChain, int kind){
        //store InstanceChain of Candidate
        ArrayList<InstanceList> ic = new ArrayList<>();
        // item: the last item of Candidate, i.e. the extension item.
        int item = Candidate[CandidateLength - 1];
        //the global utility of Candidate
        int Utility = 0;

        //for each InstanceList of Candidate's Prefix. Each InstanceList corresponds to one q-seq.
        for (InstanceList instanceList:instanceChain){
            // record the utility of Candidate in current q-seq
            int LocalUtility = 0;
            //the InstanceList of Candidate in current q-seq
            InstanceList il = new InstanceList();

            //store sid of current q-seq
            int sid = instanceList.get_sid();
            il.set_sid(sid);
            //get SIL of the current q-seq
            SeqInfoList seqinfoList = database.get(sid);

            //i-concatenation
            if(kind == 0){
                //construct InstanceList of Candidate in current q-seq
                //for each element in instanceList
                for (int j = 0; j < instanceList.insList.size(); j++){
                    int itemsetID = instanceList.insList.get(j).tid;
                    int itemIndex = seqinfoList.ItemsetContainItem(itemsetID,item);
                    // if the extension item appears in this itemset, we can do i-concatenate to form Candidate and get a new instance.
                    if (itemIndex != -1){
                        int PrefixUtility = instanceList.insList.get(j).acu;
                        il.add(itemsetID,PrefixUtility + seqinfoList.seqInfo[itemsetID].get(itemIndex).getUtility());
                    }
                }
                // if the current q-seq does not contain Candidate, we continue to handle the next instanceList.
                if (il.insList.size() == 0)
                    continue;
            }else{ //s-concatenation
                //number of itemsets in current q-seq
                int numOfItemset = seqinfoList.seqInfo.length;
                //construct InstanceList of Candidate in the current q-seq
                //for each element in instanceList
                for (int j = 0; j < instanceList.insList.size(); j++){
                    int itemsetID = instanceList.insList.get(j).tid;
                    //if it is the last itemset of current q-seq, we finish the s-concatenation.
                    if( itemsetID == numOfItemset - 1) break;
                    int itemIndex = seqinfoList.ItemsetContainItem(itemsetID + 1,item);
                    // if the extension item appears in the next itemset, we can do s-concatenate to form Candidate and get a new instance.
                    if (itemIndex != -1){
                        int PrefixUtility = instanceList.insList.get(j).acu;
                        il.add(itemsetID + 1,PrefixUtility+seqinfoList.seqInfo[itemsetID + 1].get(itemIndex).getUtility());
                    }
                }
                // if the current q-seq does not contain Candidate, we continue to handle the next instanceList.
                if (il.insList.size() == 0)
                    continue;
            }

            // calculate utility of Candidate in the current q-seq
            for (int i = 0; i < il.insList.size(); i++){
                InstanceList.InstanceElement ie = il.insList.get(i);
                if (ie.acu > LocalUtility)
                    LocalUtility = ie.acu;
            }

            //update the global Utility of Candidate
            Utility += LocalUtility;

            //add InstanceList to InstanceChain
            ic.add(il);
        }

        // if in debug mode, we print the InstanceChain that we have just built
        if(DEBUG==6){
            System.out.println("**********************");
            System.out.print("Candidate: ");
            for (int i=0;i<CandidateLength;i++){
                System.out.print(Candidate[i]+" ");
            }
            System.out.println();
            System.out.println(" global Utility:"+Utility);
            for (int i=0;i<ic.size();i++){
                System.out.println(i+"-th InstanceList:");
                for(int j=0;j<ic.get(i).insList.size();j++){
                    System.out.print("Element"+j+": ");
                    System.out.print("sid:"+ic.get(i).get_sid());
                    System.out.print("  tid:"+ic.get(i).insList.get(j).tid);
                    System.out.print("  acu:"+ic.get(i).insList.get(j).acu);
                }
                System.out.println("End of a InstanceList");
            }
            System.out.println("#####################");
        }

        Candidate[CandidateLength] = -1;
        Candidate[CandidateLength+1] = -2;

        //return the ItemConcatnation of Candidate
        return new ItemConcatenation(Utility, ic, Candidate, CandidateLength);
    }

    /**
     * recursive pattern growth function
     * @param prefix prefix sequence
     * @param prefixLength length of prefix
     * @param database  SIL of all q-seq
     * @param instanceChain InstanceChain of prefix
     * @param itemCount number of items in prefix
     */
    private void FUCPM(int[] prefix, int prefixLength, List<SeqInfoList> database, ArrayList<InstanceList> instanceChain, int itemCount) throws IOException {

        if(DEBUG==7){
                // Print the current prefix
                for(int i = 0; i < prefixLength; i++){
                    System.out.print(prefix[i] + " ");
                }
                System.out.println("TmpMinUtility:" + minUtility);
        }

        //for storing global IEU of i-extension items and s-extension items.
        //they are also ilist and slist.
        Map<Integer,Integer> mapiItemIEU = new HashMap<>();
        Map<Integer,Integer> mapsItemIEU = new HashMap<>();

        /***** Construct ilist and slist *****/
        //scan prefix-projected DB once to find items to be concatenated
        for (InstanceList instanceList : instanceChain) {
            SeqInfoList seqinfoList = database.get(instanceList.get_sid());

            //record the last item of prefix
            int item = prefix[prefixLength-1];

            // store the local IEU of the i-extension items in current q-seq
            Map<Integer,Integer> mapiItemLocalIEU = new HashMap<Integer, Integer>();
            // store the local IEU of the s-extension items in current q-seq
            Map<Integer,Integer> mapsItemLocalIEU = new HashMap<Integer, Integer>();

            /***** Construct ilist *****/
            // put i-extension items into ilist and update the global variable mapiItemIEU
            // for each element in instanceList
            for (int j = 0; j < instanceList.insList.size(); j++){
                int itemsetID = instanceList.insList.get(j).tid;
                //find i-extension items in current itemset
                for(int i = 0;i < seqinfoList.seqInfo[itemsetID].size(); i++){
                    //only the items whose lexicographical order is larger than that of item can be added to ilist
                    if(seqinfoList.seqInfo[itemsetID].get(i).getItem() <= item) continue;
                    int ConItem = seqinfoList.seqInfo[itemsetID].get(i).getItem();
                        //calculate IEU of ConItem
                        int prefixUtility = instanceList.insList.get(j).acu;
                        int currentIEU = prefixUtility + seqinfoList.seqInfo[itemsetID].get(i).getUtility() + seqinfoList.seqInfo[itemsetID].get(i).getRestutility();
                        //if ConItem appears in current q-seq for the first time
                        if(mapiItemLocalIEU.get(ConItem) == null) {
                            mapiItemLocalIEU.put(ConItem, currentIEU);
                            if (mapiItemIEU.get(ConItem) == null) { //if ConItem appears in database for the first time
                                mapiItemIEU.put(ConItem, currentIEU);
                            } else {
                                int tmpIEU = mapiItemIEU.get(ConItem);
                                mapiItemIEU.put(ConItem, currentIEU + tmpIEU);
                            }
                        }else{ //if ConItem has already appeared in current q-seq.
                            //choose the greater IEU
                            if(currentIEU > mapiItemLocalIEU.get(ConItem)){
                                int tmpGlobalIEU = mapiItemIEU.get(ConItem);
                                mapiItemIEU.put(ConItem, tmpGlobalIEU - mapiItemLocalIEU.get(ConItem) + currentIEU);
                                mapiItemLocalIEU.put(ConItem, currentIEU);
                            }
                        }

                }
            }

            /***** Construct slist *****/
            //put s-extension items into slist and get the items to be s-concatenated
            // for each element in instanceList
            for (int j = 0; j < instanceList.insList.size(); j++) {
                int itemsetID = instanceList.insList.get(j).tid;
                //if it is the last itemset of current q-seq, we finish finding s-extension items.
                if(itemsetID == seqinfoList.seqInfo.length - 1) break;
                //find s-extension items in the next itemset
                for(int i = 0; i < seqinfoList.seqInfo[itemsetID + 1].size(); i++){
                    int ConItem = seqinfoList.seqInfo[itemsetID + 1].get(i).getItem();
                        //calculate IEU of ConItem
                        int prefixUtility = instanceList.insList.get(j).acu;
                        int currentIEU = prefixUtility + seqinfoList.seqInfo[itemsetID + 1].get(i).getUtility() + seqinfoList.seqInfo[itemsetID + 1].get(i).getRestutility();
                        //if ConItem appears in current q-seq for the first time
                        if(mapsItemLocalIEU.get(ConItem) == null) {
                            mapsItemLocalIEU.put(ConItem, currentIEU);
                            if (mapsItemIEU.get(ConItem) == null) { //if ConItem appears in database for the first time
                                mapsItemIEU.put(ConItem, currentIEU);
                            } else {
                                int tmpIEU = mapsItemIEU.get(ConItem);
                                mapsItemIEU.put(ConItem, currentIEU + tmpIEU);
                            }
                            //mapsItemLocalIEU.put(ConItem, 1);
                        }else{ //if ConItem has already appeared in current q-seq.
                            //choose the greater IEU
                            if(currentIEU > mapsItemLocalIEU.get(ConItem)){
                                int tmpGlobalIEU = mapsItemIEU.get(ConItem);
                                mapsItemIEU.put(ConItem, tmpGlobalIEU - mapsItemLocalIEU.get(ConItem) + currentIEU);
                                mapsItemLocalIEU.put(ConItem, currentIEU);
                            }
                        }
                }
            }
        }//Finish constructing ilist and slist.

        // for temporarily storing information of candidates after extension
        ItemConcatenation ItemCom;

        /***** I-Extension *****/
        // perform I-Extension to grow the pattern larger.
        for (Entry<Integer,Integer> entry : mapiItemIEU.entrySet()){
            int item = entry.getKey();
            int ieu = entry.getValue();

            // without IEU pruning strategy
            /*if (ieu < minUtility){
                continue;
            }*/

            //construct the candidate after extension
            prefix[prefixLength] = item;

            //construct InstanceChain of the candidate
            if (itemCount + 1 <= maxPatternLength){
                ItemCom = ConstructInstanceChain(prefix,prefixLength + 1, database, instanceChain,0);
                NumOfCandidate++;
                //check whether the candidate is high-utility
                if(ItemCom.utility >= minUtility){
                    //writeOut(ItemCom.candidate,ItemCom.candidateLength,ItemCom.utility);
                    patternCount++;
                }

                //mine the database recursively using the FUCPM procedure
                FUCPM(ItemCom.candidate, ItemCom.candidateLength, database, ItemCom.IChain, itemCount+1);
            }
        }

        /***** S-Extension *****/
        // perform S-Extension to grow the pattern larger.
        for (Entry<Integer,Integer> entry : mapsItemIEU.entrySet()){
            int item = entry.getKey();
            int ieu = entry.getValue();

            // without IEU pruning strategy
            /*if (ieu < minUtility){
                continue;
            }*/

            //construct the candidate after extension
            prefix[prefixLength] = -1;
            prefix[prefixLength+1] = item;

            //construct InstanceChain of the candidate
            if (itemCount + 1 <= maxPatternLength){
                ItemCom = ConstructInstanceChain(prefix,prefixLength + 2, database, instanceChain,1);
                NumOfCandidate++;
                //check whether the candidate is high-utility
                if(ItemCom.utility >= minUtility){
                    //writeOut(ItemCom.candidate,ItemCom.candidateLength,ItemCom.utility);
                    patternCount++;
                }
                //mine the database recursively using the FUCPM procedure
                FUCPM(ItemCom.candidate, ItemCom.candidateLength, database, ItemCom.IChain, itemCount+1);
            }
        }

        //check the memory usage
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
        patternCount++;

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
        if(DEBUG==8) {
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
            // for each line (q-seq) until the end of file
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
        System.out.println("=============  FUCPM_IEU ALGORITHM v1.0 - STATS  ==========");
        System.out.println(" Minimum utility threshold ~ " + minUtility );
        System.out.println(" Total time ~ " + (endTimestamp - startTimestamp) + " ms");
        System.out.println(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Number Of Candidate ~ " + NumOfCandidate);
        System.out.println(" High-utility sequential pattern count ~ " + patternCount);
        System.out.println("========================================================");
    }
}
