package HUSPull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jiexiong Zhang, Wensheng Gan @HITsz, China
 * 
 * @comment This is an adapted version of the HUSPull algorithm. This algorithm can mine high-utility contiguous pattern form databases.
 * 
 * 1. Project(ULinkList/Matrix):
 * transformed from transaction, some of transactions may be empty (do not have UPositions) and
 * not in ProjectULinkListDB.
 * 
 * 2. UPosition:
 * 1). a sequence has multiple matches in a transaction, a UPosition represents a match,
 * which stores the utility and last item position of each match.
 * 
 * 2). position can be used as a prefix to calculate swu and concatenation with addItem.
 */
public class HUSPull {
	// % of threshold * databaseUtility 
    protected double threshold; 
    // test dataset
    protected String pathname;

    double startTime;
    double finishTime;

    protected double databaseUtility;
    // = databaseUtility * threshold.
    protected double minUtility;  

    protected long huspNum;
    protected long candidateNum;

    protected boolean isDebug;
    protected ArrayList<String> patterns;
    protected boolean isWriteToFile;
    protected String output;

    protected double currentTime;

    protected boolean[] isRemove;

    
    /**
     * Set the WSU and LastID
     * 
     * @author wsgan
     *
     */
    protected class LastId {
        public double swu;
        public ULinkList uLinkList;

        public LastId(double swu, ULinkList uLinkList) {
            this.swu = swu;
            this.uLinkList = uLinkList;
        }
    }

    /**
     * HUSP-Miner
     * 
     * @param pathname
     * @param threshold
     * @param output
     */
    public HUSPull(String pathname, double threshold, String output) {
        this.pathname = pathname;
        this.threshold = threshold;

        huspNum = 0;
        candidateNum = 0;

        isDebug = false;
        isWriteToFile = false;
        this.output = output;
    }

    /**
     * Run HUSP-Miner
     * 
     */
    public void runAlgo() throws IOException{
        startTime = System.currentTimeMillis();
        if (isWriteToFile) patterns = new ArrayList<String>();
        if (isDebug) currentTime = System.currentTimeMillis();
        
        // reset maximum memory
		MemoryLogger.getInstance().reset();
        DataBase dataBase = ConstructDBAndULL.getDataBase(pathname);
        databaseUtility = dataBase.utility();
        minUtility = dataBase.utility() * threshold;

        System.out.println("minUtility:"+minUtility);
        if (isDebug) System.out.println(System.currentTimeMillis() - currentTime);
        ULinkList[] uLinkListDB = ConstructDBAndULL.getULinkListDB(dataBase, minUtility);
        if (isDebug) System.out.println(System.currentTimeMillis() - currentTime);

        isRemove = new boolean[dataBase.getMaxItemName() + 1];
        for (int i = 0; i < isRemove.length; ++i) isRemove[i] = false;

        MemoryLogger.getInstance().checkMemory();
        
        // call the mining function
        firstUSpan(uLinkListDB, new ArrayList<>(dataBase.getMaxTXNLen() + 1));

        MemoryLogger.getInstance().checkMemory();
        finishTime = System.currentTimeMillis();

        //System.out.println("Max memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        if (isWriteToFile) writeToFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        writer.write("=============  HUSP-ULL ALGORITHM - STATS ============" + " \n");
        writer.write("Minimum utility threshold ~ " + minUtility + " \n");
        writer.write("Total time ~ "+ (finishTime - startTime)/1000 + " s" + " \n");
        writer.write("Max Memory ~ "+ MemoryLogger.getInstance().getMaxMemory() + " MB" + " \n");
        writer.write("Number of candidates ~ "+ candidateNum + " \n");
        writer.write("Number of HUCSPs ~ "+ huspNum + " \n");
        writer.close();
    }

    /**
     * Write to file
     * 
     */
    protected void writeToFile() throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        for(String s : patterns){
            writer.write(s);
            writer.newLine();
        }
        writer.close();
    }

    /**
     * First USpan
     * 
     * @param uLinkListDB
     * @param prefix
     */
    protected void firstUSpan(ULinkList[] uLinkListDB, ArrayList<Integer> prefix) {
        HashMap<Integer, Double> mapItemSwu = getMapItemSwu(uLinkListDB);

        for (Map.Entry<Integer, Double> entry : mapItemSwu.entrySet())
            if (entry.getValue() < minUtility) isRemove[entry.getKey()] = true;
        // remove the invalid item
        firstRemoveItem(uLinkListDB);

        // call the function of firstConcatenation
        firstConcatenation(uLinkListDB, prefix, mapItemSwu);
        
        // check the memory usage
     	MemoryLogger.getInstance().checkMemory();
    }

    /**
     * Get MAPItem of SWU
     * 
     * @param uLinkListDB
     * @return
     */
    protected HashMap<Integer, Double> getMapItemSwu(ULinkList[] uLinkListDB) {
        HashMap<Integer, Double> mapItemSwu = new HashMap<Integer, Double>();
        for (ULinkList uLinkList : uLinkListDB) {
            for (int i = 0; i < uLinkList.headerLength(); ++i) {
                int item = uLinkList.header(i);
                Double twu = mapItemSwu.getOrDefault(item, 0.0);
                mapItemSwu.put(item, uLinkList.getTransactionUtility() + twu);
            }
        }
        
        return mapItemSwu;
    }

    /**
     * First concatenation
     * 
     * @param uLinkListDB
     * @param prefix
     * @param mapItemSwu
     */
    protected void firstConcatenation(ULinkList[] uLinkListDB, ArrayList<Integer> prefix,
                                      HashMap<Integer, Double> mapItemSwu) {
        for (Map.Entry<Integer, Double> entry : mapItemSwu.entrySet()) {
            if (entry.getValue() >= minUtility) {
                /*System.out.print("prefix:");
                for(int i:prefix) System.out.print(i+" ");
                System.out.println();
                System.out.println("additem:"+entry.getKey());*/
                candidateNum += 1;
                int addItem = entry.getKey();
                double sumUtility = 0;
                double upperBound = 0;
                ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<ProjectULinkList>();
                
                for (ULinkList uLinkList : uLinkListDB) {
                    Integer firstPosOfItem = uLinkList.firstPosOfItemByName(addItem);
                    if (firstPosOfItem != null) {  
                    	// addItem should be in the transaction
                        double utilityInTXN = 0;
                        double ubInTXN = 0;
                        ArrayList<UPosition> newUPositions = new ArrayList<UPosition>();
                        
                        for (int index = firstPosOfItem; index != -1; index = uLinkList.nextPos(index)) {
                            double curUtility = uLinkList.utility(index);
                            utilityInTXN = Math.max(utilityInTXN, curUtility);
                            ubInTXN = Math.max(ubInTXN, getUpperBound(uLinkList, index, curUtility));
                            newUPositions.add(new UPosition(index, curUtility));
                        }
                        
                        // update the sumUtility and upper-bound
                        if (newUPositions.size() > 0) {
                            newProjectULinkListDB.add(
                                    new ProjectULinkList(uLinkList, newUPositions, utilityInTXN));
                            sumUtility += utilityInTXN;
                            upperBound += ubInTXN;
                        }
                    }
                }
                
                upperBound += getUpperBoundAdd(sumUtility);
                // upperBound >= minUtility
                if (upperBound >= minUtility) {
                    prefix.add(addItem);

                    //TODO: 这里加条件
                    // sumUtility >= minUtility
                    if (sumUtility >= minUtility)
                        getPattern(prefix, newProjectULinkListDB, sumUtility);
                    
                    // call the function
                    runHUSPspan(newProjectULinkListDB, prefix);
                    prefix.remove(prefix.size() - 1);
                }
            }
        }
    }

    /**
     * Run USpan algorithm
     * 
     * @param projectULinkListDB
     * @param prefix
     */
    protected void runHUSPspan(ArrayList<ProjectULinkList> projectULinkListDB, ArrayList<Integer> prefix) {
        HashMap<Integer, LastId> mapItemExtensionUtility = getMapItemExtensionUtility(projectULinkListDB);


        // remove the item has low SWU
        for (Map.Entry<Integer, LastId> entry : mapItemExtensionUtility.entrySet()) {
            int item = entry.getKey();
            double swu = entry.getValue().swu;
            if (swu < minUtility) {
                isRemove[item] = true;
            }
        }
        removeItem(projectULinkListDB);

        // call the iConcatenation function
        HashMap<Integer, LastId> mapItemIConcatenationSwu = getMapItemIConcatenationSwu(projectULinkListDB);
        iConcatenation(projectULinkListDB, prefix, mapItemIConcatenationSwu);
        
        // check the memory usage
     	MemoryLogger.getInstance().checkMemory();
     		
        // call the sConcatenation function
        HashMap<Integer, LastId> mapItemSConcatenationSwu = getMapItemSConcatenationSwu(projectULinkListDB);
        sConcatenation(projectULinkListDB, prefix, mapItemSConcatenationSwu);
        
        // check the memory usage
		MemoryLogger.getInstance().checkMemory();
		
        for (Map.Entry<Integer, LastId> entry : mapItemExtensionUtility.entrySet()) {
            int item = entry.getKey();
            double swu = entry.getValue().swu;
            if (swu < minUtility) isRemove[item] = false;
        }
        
        removeItem(projectULinkListDB);
    }

    /**
     * items appear after prefix in the same itemset in difference sequences;
     * SWU = the sum these sequence utilities for each item as their upper bounds under prefix
     * should not add sequence utility of same sequence more than once
     *
     * @param projectedDB: database
     * @return upper-bound
     */
    protected HashMap<Integer, LastId> getMapItemIConcatenationSwu(ArrayList<ProjectULinkList> projectedDB) {
        HashMap<Integer, LastId> mapItemIConcatenationSwu = new HashMap<Integer, LastId>();
        for (ProjectULinkList projectULinkList : projectedDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            double localSwu = getItemUpperBound(projectULinkList);
            
            for (UPosition uPosition : uPositions) {
                for (int index = uPosition.index() + 1; index < uLinkList.length(); ++index) {
                    int item = uLinkList.itemName(index);
                    
                    if (item != -1 && !isRemove[item]) {  
                    	// only find items in the same itemset, else break
                        LastId lastId = mapItemIConcatenationSwu.get(item);
                        
                        if (lastId == null) {
                            mapItemIConcatenationSwu.put(item, new LastId(localSwu, uLinkList));
                        } else {
                            // should not add sequence utility of same sequence more than once
                            // since many UPosition may have same item, [a b] [a b]
                            if (lastId.uLinkList != uLinkList) {
                                lastId.swu += localSwu;
                                lastId.uLinkList = uLinkList;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return mapItemIConcatenationSwu;
    }

    /**
     * items appear from the next itemset after prefix in difference sequences;
     * SWU = sum these sequence utilities for each item as their upper bounds under prefix
     * should not add sequence utility of same sequence more than once
     *
     * @param projectedDB: database
     * @return upper-bound
     */
    protected HashMap<Integer, LastId> getMapItemSConcatenationSwu(ArrayList<ProjectULinkList> projectedDB) {
        HashMap<Integer, LastId> mapItemSConcatenationSwu = new HashMap<Integer, LastId>();
        for (ProjectULinkList projectULinkList : projectedDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            double localSwu = getItemUpperBound(projectULinkList);
            // ....
            //TODO: 这里需要考虑前缀的最后一个项的所有位置
            for(int j=0; j < uPositions.size(); j++) {
                //uPositions.get(j).index()：前缀的第i个instance的最后一个项在uLinkList的位置
                //下一个项集的首个项的位置
                int addItemPos = uLinkList.nextItemsetPos(uPositions.get(j).index());
                //下下个项集的首个项的位置
                int endingPos = -1;
                if (addItemPos != -1) {
                    endingPos = uLinkList.nextItemsetPos(addItemPos);
                }
                // two methods to calc swu of s-concatenation
                // first one is to traverse the last positions of items in header,
                // which will not repeat adding swu of item in the same transaction.
                //TODO: 注释这里的if
                /**if (addItemPos != -1 && uLinkList.length() - addItemPos + 1 > uLinkList.headerLength()) {
                    for (int i = 0; i < uLinkList.headerLength(); ++i) {
                        if (uLinkList.lastPosOfItemByInd(i) >= addItemPos) {
                            LastId lastId = mapItemSConcatenationSwu.get(uLinkList.header(i));
                            if (lastId == null) {
                                mapItemSConcatenationSwu.put(uLinkList.header(i), new LastId(localSwu, uLinkList));
                            } else {
                                // update the SWU of lastID
                                lastId.swu += localSwu;
                            }
                        }
                    }
                } else {*/
                    // the second one is to traverse from the position of next itemset of addItem to
                    // the end of transaction, which may repeat adding swu of item in the same transaction.
                    //TODO: 修改这里，只从下一个项集找扩展项
                    for (int index = addItemPos; (index < endingPos && index != -1 && endingPos != -1)||
                            (index != -1 && endingPos == -1 && index < uLinkList.length()); ++index) {
                        int item = uLinkList.itemName(index);
                        if (item != -1 && !isRemove[item]) {
                            LastId lastId = mapItemSConcatenationSwu.get(item);
                            if (lastId == null) {
                                mapItemSConcatenationSwu.put(item, new LastId(localSwu, uLinkList));
                            } else {
                                // should not add sequence utility of same sequence more than once
                                if (lastId.uLinkList != uLinkList) {
                                    lastId.swu += localSwu;
                                    lastId.uLinkList = uLinkList;
                                }
                            }
                        }
                    //}
                }

            }
        }
        return mapItemSConcatenationSwu;
    }

    /**
     * I-concatenation
     * <p>
     * current item should be larger than last item in prefix
     * avoiding repetition: e.g. <[a a]>, <[a b]> and <[b a]>
     * candidate sequences are evaluated by (prefix utility + remaining utility) (PU)
     *
     * @param projectedDB:              database
     * @param prefix:                   prefix sequence
     * @param mapItemIConcatenationSwu: upper-bound of addItem
     */
    protected void iConcatenation(ArrayList<ProjectULinkList> projectedDB, ArrayList<Integer> prefix,
                                  HashMap<Integer, LastId> mapItemIConcatenationSwu) {
        for (Map.Entry<Integer, LastId> entry : mapItemIConcatenationSwu.entrySet()) {
                candidateNum += 1;
                int addItem = entry.getKey();
                /*System.out.print("prefix:");
                for(int i:prefix) System.out.print(i+" ");
                System.out.println();
                System.out.println("additem:"+entry.getKey());*/
                double sumUtility = 0;
                double upperBound = 0;
                ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<ProjectULinkList>();

                for (ProjectULinkList projectULinkList : projectedDB) {
                    ULinkList uLinkList = projectULinkList.getULinkList();
                    ArrayList<UPosition> uPositions = projectULinkList.getUPositions();

                    Integer firstPosOfItem = uLinkList.firstPosOfItemByName(addItem);
                    
                    /*
                     * 1. addItem should in the header of transaction
                     * 2. addItem should be larger than UPosition (last item in prefix), avoiding
                     * repetition.  e.g. <[a a]>, <[a b]> and <[b a]>
                     */
                    if (firstPosOfItem != null && uLinkList.itemName(uPositions.get(0).index()) < addItem) {
                        double utilityInTXN = 0;
                        double ubInTXN = 0;
                        ArrayList<UPosition> newUPositions = new ArrayList<UPosition>();

                        /*
                         * i: index of UPosition (prefix), UPosition contains position and prefix
                         * utility.
                         * addItemInd: position of addItem in transaction, can get item utility.
                         * 
                         * addItem should in the same itemset with UPosition (last item of prefix),
                         * which indicates that prefixItemsetIndex == addItemItemsetIndex
                         */
                        for (int i = 0, addItemInd = firstPosOfItem; i < uPositions.size() && addItemInd != -1; ) {
                            UPosition uPosition = uPositions.get(i);
                            int uPositionItemsetIndex = uLinkList.whichItemset(uPosition.index());
                            int addItemItemsetIndex = uLinkList.whichItemset(addItemInd);

                            if (uPositionItemsetIndex == addItemItemsetIndex) {
                                double curUtility = uLinkList.utility(addItemInd) + uPosition.utility();
                                utilityInTXN = Math.max(utilityInTXN, curUtility);
                                ubInTXN = Math.max(ubInTXN, getUpperBound(uLinkList, addItemInd, curUtility));
                                newUPositions.add(new UPosition(addItemInd, curUtility));

                                addItemInd = uLinkList.nextPos(addItemInd);
                                i++;
                            } else if (uPositionItemsetIndex > addItemItemsetIndex) {
                                addItemInd = uLinkList.nextPos(addItemInd);
                            } else if (uPositionItemsetIndex < addItemItemsetIndex) {
                                i++;
                            }
                        }
                        
                        // if exist new positions, update the sumUtility and upper-bound
                        if (newUPositions.size() > 0) {
                            newProjectULinkListDB.add(new ProjectULinkList(uLinkList, newUPositions, utilityInTXN));
                            sumUtility += utilityInTXN;
                            upperBound += ubInTXN;
                        }
                    }
                }
                //upperBound += getUpperBoundAdd(sumUtility);
                // upperBound >= minUtility
                if (upperBound >= minUtility) {
                    prefix.add(addItem);
                    
                    // sumUtility >= minUtility
                    if (sumUtility >= minUtility)
                        getPattern(prefix, newProjectULinkListDB, sumUtility);
                    
                    // call the function
                    runHUSPspan(newProjectULinkListDB, prefix);
                    prefix.remove(prefix.size() - 1);
                }
        }
    }

    /**
     * S-concatenation
     * <p>
     * each addItem (candidate item) has multiple index in the sequence
     * each index can be s-concatenation with multiple UPositions before this index
     * but these UPositions s-concatenation with the same index are regarded as one sequence
     * so for each index, choose the UPosition with maximal utility
     * <p>
     * candidate sequences are evaluated by (prefix utility + remaining utility) (PU)
     *
     * @param projectedDB:              database
     * @param prefix:                   prefix sequence
     * @param mapItemSConcatenationSwu: upper-bound of addItem
     */
    protected void sConcatenation(ArrayList<ProjectULinkList> projectedDB, ArrayList<Integer> prefix,
                                  HashMap<Integer, LastId> mapItemSConcatenationSwu) {
        for (Map.Entry<Integer, LastId> entry : mapItemSConcatenationSwu.entrySet()) {
            if (entry.getValue().swu >= minUtility) {
                candidateNum += 1;
                int addItem = entry.getKey();

                double sumUtility = 0;
                double upperBound = 0;
                ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<ProjectULinkList>();

                for (ProjectULinkList projectULinkList : projectedDB) {
                    ULinkList uLinkList = projectULinkList.getULinkList();
                    ArrayList<UPosition> uPositions = projectULinkList.getUPositions();//前缀的所有instance的最后一个项在ULinkList的位置

                    Integer firstPosOfItem = uLinkList.firstPosOfItemByName(addItem); //扩展项有多个instance，这里获得的是首个instance的位置
                    if (firstPosOfItem != null) {  // addItem should be in the transaction
                        double utilityInTXN = 0;
                        double ubInTXN = 0;
                        ArrayList<UPosition> newUPositions = new ArrayList<UPosition>();

                        /*
                         * each addItem has multiple index (will become new UPosition) in the
                         * sequence, each index (will become new UPosition) can be s-concatenation
                         * with multiple UPositions (contain position of last item in prefix)
                         * before this index, but multiple UPositions s-concatenation with the same
                         * index are regarded as one new UPosition, so for each index, choose the
                         * maximal utility of UPositions before this index as prefix utility for
                         * this index.
                         */
                        double maxPositionUtility = 0;  // choose the maximal utility of UPositions
                        int uPositionNextItemsetPos = -1;

/**
                        for (int  addItemInd = firstPosOfItem; addItemInd != -1; addItemInd = uLinkList.nextPos(addItemInd)) {
                            //System.out.println("进入循环，addItemInd:" + addItemInd);
                            for (int i = 0; i < uPositions.size(); i++) { //对于前缀的每个instance的最后一个项在ULinkList的位置
                                uPositionNextItemsetPos = uLinkList.nextItemsetPos(uPositions.get(i).index()); //下一个项集的首个项的位置
                                int endingPos = -1;
                                if(uPositionNextItemsetPos != -1){
                                    endingPos = uLinkList.nextItemsetPos(uPositionNextItemsetPos);
                                }

                                // 1. next itemset should be in transaction
                                // 2. addItem should be after or equal to the next itemset of UPosition
                                //TODO: 不是所有index都是合法的，扩展项必须在前缀最后一个项的下一个项集
                                if ((uPositionNextItemsetPos != -1 && endingPos != -1 && addItemInd >= uPositionNextItemsetPos && addItemInd < endingPos) ||
                                        (uPositionNextItemsetPos != -1 && endingPos == -1 && addItemInd >= uPositionNextItemsetPos)) {
                                    //System.out.println("符合条件的addItemInd:"+addItemInd);
                                    //TODO:这里是找前缀所有instance的最后一个item的最大的效用值？ 问题应该出在这里
                                    if (maxPositionUtility < uPositions.get(i).utility())
                                        maxPositionUtility = uPositions.get(i).utility();
                                }
                            }
                            //System.out.println("maxPositionUtility:"+maxPositionUtility);
                            // maxPositionUtility is initialized outside the loop,
                            // will be the same or larger than before
                            if (maxPositionUtility != 0) {
                                //TODO: addItemInd有问题
                                double curUtility = uLinkList.utility(addItemInd) + maxPositionUtility;
                                //System.out.println("additem Utility:"+uLinkList.utility(addItemInd));
                                //System.out.println("addItemInd:"+addItemInd);
                                //System.out.println("curUtility:"+curUtility);
                                newUPositions.add(new UPosition(addItemInd, curUtility));
                                utilityInTXN = Math.max(utilityInTXN, curUtility);
                                ubInTXN = Math.max(ubInTXN, getUpperBound(uLinkList, addItemInd, curUtility));
                            }
                        }
                */


                            for (int i = 0; i < uPositions.size(); i++) { //对于前缀的每个instance的最后一个项在ULinkList的位置
                                double prefixUtility = uPositions.get(i).utility();

                                uPositionNextItemsetPos = uLinkList.nextItemsetPos(uPositions.get(i).index()); //下一个项集的首个项的位置
                                int endingPos = -1;
                                if(uPositionNextItemsetPos != -1){
                                    endingPos = uLinkList.nextItemsetPos(uPositionNextItemsetPos);
                                }

                                //看看下一个项集是否包含additem
                                for (int  addItemInd = firstPosOfItem; addItemInd != -1; addItemInd = uLinkList.nextPos(addItemInd)) {
                                    if(addItemInd < uPositionNextItemsetPos) continue;
                                    if ((uPositionNextItemsetPos != -1 && endingPos != -1 && addItemInd >= uPositionNextItemsetPos && addItemInd < endingPos) ||
                                            (uPositionNextItemsetPos != -1 && endingPos == -1 && addItemInd >= uPositionNextItemsetPos)) {
                                        double curUtility = uLinkList.utility(addItemInd) + prefixUtility;
                                        newUPositions.add(new UPosition(addItemInd, curUtility));
                                        utilityInTXN = Math.max(utilityInTXN, curUtility);
                                        ubInTXN = Math.max(ubInTXN, getUpperBound(uLinkList, addItemInd, curUtility));
                                    }
                                }
                        }

                     // if exist new positions, update the sumUtility and upper-bound
                        if (newUPositions.size() > 0) {
                            newProjectULinkListDB.add(new ProjectULinkList(uLinkList, newUPositions, utilityInTXN));
                            sumUtility += utilityInTXN;
                            upperBound += ubInTXN;
                        }
                    }
                }
                //upperBound += getUpperBoundAdd(sumUtility);
                // upperBound >= minUtility
                if (upperBound >= minUtility) {
                    prefix.add(-1);
                    prefix.add(addItem);
                    
                    // sumUtility >= minUtility
                    if (sumUtility >= minUtility) {
                        getPattern(prefix, newProjectULinkListDB, sumUtility);
                    }
                    //System.out.println("prefix after extension:");
                    /*for(int i:prefix){
                        System.out.print(i+" ");
                    }
                    System.out.println();*/
                    // call the function
                    runHUSPspan(newProjectULinkListDB, prefix);
                    prefix.remove(prefix.size() - 1);
                    prefix.remove(prefix.size() - 1);
                }
            }
        }
    }

    /**
     * 
     * Example for check of S-Concatenation
     * <[(3:25)], [(1:32) (2:18) (4:10) (5:8)], [(2:12) (3:40) (5:1)]> 146
     * Pattern: 3 -1 2
     * UPositions: (3:25), (3:40)
     * For
     * addItemInd = firstPosOfItemByName = (2:18)
     *   UPosition = (3:25)
     *   uPositionNextItemsetPos = [(1:32) (2:18) (4:10) (5:8)]
     *   maxPositionUtility = 25
     *   UPosition = (3:40)
     *   uPositionNextItemsetPos = -1 -> break
     * newUPosition = 25 + 18
     * addItemInd = (2:12)
     *   UPosition = (3:40)
     *   uPositionNextItemsetPos = -1 -> break
     * newUPosition = 25 + 12
     * End
     */

    /**
     * Get upper-bound
     * 
     * @param sumUtility
     * @return
     */
    protected double getUpperBoundAdd(double sumUtility) {
        return 0;
    }

    /**
     * PEU
     * 
     * @param uLinkList
     * @param index
     * @param curUtility
     * @return
     */
    protected double getUpperBound(ULinkList uLinkList, int index, double curUtility) {
        return curUtility + uLinkList.remainUtility(index);
    }

    /**
     * PEU
     * 
     * @param projectULinkList
     * @return
     */
    protected double getItemUpperBound(ProjectULinkList projectULinkList) {
        ULinkList uLinkList = projectULinkList.getULinkList();
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        double upperBound = 0;
        for (UPosition uPosition : uPositions) {
            upperBound = Math.max(upperBound, uPosition.utility() + uLinkList.remainUtility(uPosition.index()));
        }
        
        // return upper-bound
        return upperBound;
    }

    /**
     * Get pattern
     * 
     * @param prefix
     * @param projectULinkListDB
     * @param sumUtility
     */
    protected void getPattern(ArrayList<Integer> prefix, ArrayList<ProjectULinkList> projectULinkListDB, double sumUtility) {
        huspNum += 1;
        if (isWriteToFile) {
            StringBuilder temp = new StringBuilder();
            for (Integer item : prefix) temp.append(item).append(" ");
            temp.append("-1 #UTIL: ").append(sumUtility);
            patterns.add(temp.toString());
        }
    }

    /**
     * reset remaining utility for not removed item.
     * 
     * @param uLinkListDB ULinkLists of database
     */
    protected void firstRemoveItem(ULinkList[] uLinkListDB) {
        for (ULinkList uLinkList : uLinkListDB) {
            double remainingUtility = 0;
            for (int i = uLinkList.length() - 1; i >= 0; --i) {
                int item = uLinkList.itemName(i);
                
                if (item != -1 && !isRemove[item]) {
                    uLinkList.setRemainUtility(i, remainingUtility);
                    remainingUtility += uLinkList.utility(i);
                }
            }
        }
    }

    /**
     * items appear after prefix (including I-Concatenation and S-Concatenation item) in difference
     * sequences;
     * sum these MEU for each item as their upper bounds under prefix
     * PEU = max{position.utility + position.remaining utility}
     * should not add sequence utility of same sequence more than once
     * used for removing items to reduce remaining utility
     */
    
    /**
     * Get MapItemExtension utility
     * 
     * 
     * @param projectULinkListDB
     * @return
     */
    protected HashMap<Integer, LastId> getMapItemExtensionUtility(
            ArrayList<ProjectULinkList> projectULinkListDB) {
        HashMap<Integer, LastId> mapItemExtensionUtility = new HashMap<Integer, LastId>();
        for (ProjectULinkList projectULinkList : projectULinkListDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            double swuInT = getItemUpperBound(projectULinkList);

            UPosition uPosition = uPositions.get(0);
            for (int i = uPosition.index() + 1; i < uLinkList.length(); ++i) {
                int item = uLinkList.itemName(i);
                if (item != -1 && !isRemove[item]) {
                    LastId lastId = mapItemExtensionUtility.get(item);
                    if (lastId == null) {
                        mapItemExtensionUtility.put(item, new LastId(swuInT, uLinkList));
                    } else {
                        if (lastId.uLinkList != uLinkList) {
                            lastId.swu += swuInT;
                            lastId.uLinkList = uLinkList;
                        }
                    }
                }
            }
        }
        
        return mapItemExtensionUtility;
    }


    /**
     * Funtion of removeItem, using the position of remaining utility
     * used for mapItemSwu(swu = position.utility + position.remaining utility)
     * 
     * @param projectULinkListDB
     */
    protected void removeItem(ArrayList<ProjectULinkList> projectULinkListDB) {
        for (ProjectULinkList projectULinkList : projectULinkListDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            int positionIndex = uPositions.get(0).index();
            double remainingUtility = 0;
            
            for (int i = uLinkList.length() - 1; i >= positionIndex; --i) {
                int item = uLinkList.itemName(i);
                
                if (item != -1 && !isRemove[item]) {
                    uLinkList.setRemainUtility(i, remainingUtility);
                    remainingUtility += uLinkList.utility(i);
                } else {  // ??? can be delete 
                	// no, someone >= minUtility should reset remaining utility
                    uLinkList.setRemainUtility(i, remainingUtility);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "HuspMiner{" +
                "threshold= " + threshold +
                ", DB= '" + pathname.split("/")[pathname.split("/").length - 1] +
                ", DBUtility= " + databaseUtility +
                ", minUtility= " + minUtility +
                ", huspNum= " + huspNum +
                ", candidateNum= " + candidateNum +
                '}';
    }


    public ArrayList<String> getResults() {
        ArrayList<String> ret = new ArrayList<String>();
        ret.add("HuspMiner");
        ret.add("" + databaseUtility);
        ret.add("" + threshold);
        ret.add("" + huspNum);
        ret.add("" + candidateNum);
        return ret;
    }
    
	/**
	 * Print statistics about the algorithm execution
	 */
	public void printStatistics() throws IOException {
        System.out.println("=============  HUSP-ULL ALGORITHM - STATS ============");
        System.out.println("Minimum utility threshold ~ " + minUtility);
        System.out.println("Total time ~ "+ (finishTime - startTime)/1000 + " s");
        System.out.println("Max memory ~ "+ MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println("Number of candidates ~ "+ candidateNum);
        System.out.println("Number of HUCSPs ~ "+ huspNum);
        System.out.println("========================================================");
	}
	

}
