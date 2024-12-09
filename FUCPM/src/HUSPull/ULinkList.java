package HUSPull;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */

public class ULinkList {
    /** contain items and their utilities in original transaction.
     * can be replaced by Transaction class which contains transactionUtility and sid??? */
    private UItem[] transaction;
    
    /** the index of transaction in database. */
    private int sid;

    private double transactionUtility;
    private double[] remainUtility;

    /** next occurrence of this item in transaction. */
    
    // itemName names of distinguish items sorted by itemName name
    private int[] header;  
    private int[] nextPos;
    
    //firstPosOfItemByName;  // key: item, value: position
    private int[] firstPosOfItemInHeader;

    private int[] lastPosOfItemInHeader;  // last position of header item. for SConcatenation swu
    private int[] whichItemset;  // the index of this itemset in all itemsets
    private int[] nextItemsetPos;  // the position of the first item in the next itemset

    public ULinkList(UItem[] transaction, int sid) {
        this.transaction = transaction;
        this.sid = sid;
        uLinkListFormat();
        
        // test the ULL
        //toString();
    }

    public int sid() {return sid;}

    public int length() {
        return transaction.length;
    }

    public int itemName(int ind) {
        return transaction[ind].itemName();
    }

    public double utility(int ind) {
        return transaction[ind].utility();
    }

    public double remainUtility(int ind) {
        return remainUtility[ind];
    }

    public void setRemainUtility(int ind, double remainUtility) {
         this.remainUtility[ind] = remainUtility;
    }

    public int nextPos(int ind) {
        return nextPos[ind];
    }

    public int header(int ind) {
        return header[ind];
    }

    public int headerLength() {
        return header.length;
    }

    public int nextItemsetPos(int ind) {
        return nextItemsetPos[ind];
    }

    public int whichItemset(int ind) {
        return whichItemset[ind];
    }

    public double getTransactionUtility() {
        return transactionUtility;
    }

    public Integer firstPosOfItemByName(int item) {
        //return firstPosOfItemByName.get(item);
        int ind = Arrays.binarySearch(header, item);
        if (ind < 0)
            return null;
        
        return firstPosOfItemInHeader[ind];
    }

    public int lastPosOfItemByInd(int ind) {
        return lastPosOfItemInHeader[ind];
    }

    /**
     * calculate ULinkList from general transaction
     */
    private void uLinkListFormat() {
        // calculate header, itemsetNum
        int itemsetNum = 0;
        ArrayList<Integer> allItemNames = new ArrayList<Integer>();
        for (int i = 0; i < transaction.length; ++i) {
            // -1 means the end of an itemset
            // the number of -1 is the same as the number of itemsets
            if (transaction[i].itemName() != -1) allItemNames.add(transaction[i].itemName());
            else itemsetNum += 1;
        }

        // get the set of itemName (without repeat) and sort them by name
        Collections.sort(allItemNames);
        ArrayList<Integer> rdAllItemNames = new ArrayList<Integer>();
        for (int i = 0; i < allItemNames.size(); ++i) {
            if (rdAllItemNames.size() == 0 ||
                    !allItemNames.get(i).equals(rdAllItemNames.get(rdAllItemNames.size() - 1))) { // use equals, not ==
                rdAllItemNames.add(allItemNames.get(i));
            }
        }
        header = new int[rdAllItemNames.size()];
        for (int i = 0; i < header.length; ++i) header[i] = rdAllItemNames.get(i);

        /*
         * Calculate UtilityLinkedList
         * itemsetInd: the index of this itemset in all itemsets
         * itemNameInd: the index of this itemName in all distinguish itemName (header)
         * nextItemsetPosition: the position of the first item in the next itemset
         * preUtility: the sum of utilities of items after this item
         */
        remainUtility = new double[transaction.length];
        nextPos = new int[transaction.length];

        // first occurrences of items in transaction
        firstPosOfItemInHeader = new int[header.length];
        for (int i = 0; i < firstPosOfItemInHeader.length; ++i) firstPosOfItemInHeader[i] = -1;

        // last occurrences of items in transaction
        lastPosOfItemInHeader = new int[header.length];
        for (int i = 0; i < lastPosOfItemInHeader.length; ++i) lastPosOfItemInHeader[i] = -1;

        whichItemset = new int[transaction.length];
        nextItemsetPos = new int[transaction.length];

        double preUtility = 0;
        for (int i = transaction.length - 1, headerIndex = header.length - 1,
             itemsetIndex = itemsetNum, nextItemsetPosition = -1; i >= 0; --i) {
            if (transaction[i].itemName() != -1) {
            	
                // remaining Utility
                remainUtility[i] = preUtility;
                preUtility += transaction[i].utility();

                // itemset index
                whichItemset[i] = itemsetIndex;

                // position of the first item in the next itemset
                nextItemsetPos[i] = nextItemsetPosition;

                //System.out.println(headerIndex+"  item =" + transaction[i].itemName() + "   preUtility= "+ preUtility);
               // System.out.println(header.length+"  " + header[headerIndex]+" : " + transaction[i].itemName());             		
        
                
                // next position and first position
                while (headerIndex != 0 && header[headerIndex] != transaction[i].itemName()) {
                    //System.out.println(headerIndex);
                    headerIndex -= 1;
                    //System.out.println(headerIndex);
                }
                nextPos[i] = firstPosOfItemInHeader[headerIndex];
                firstPosOfItemInHeader[headerIndex] = i;

                // last position
                if (lastPosOfItemInHeader[headerIndex] == -1) lastPosOfItemInHeader[headerIndex] = i;
            } else if (transaction[i].itemName() == -1) {  // -1 is the end of an itemset
                itemsetIndex -= 1;
                headerIndex = header.length - 1;

                /* if i + 1 >  transaction.length - 1, next element is empty (out of transaction).
                 * if i + 1 <= transaction.length - 1, next element is the first item in the next
                 * itemset. */
                if (i + 1 <= transaction.length - 1) nextItemsetPosition = i + 1;
            }
        }
        transactionUtility = preUtility;

//        firstPosOfItemByName = new HashMap<>(header.length);
//        for (int i = 0; i < header.length; ++i) {
//            firstPosOfItemByName.put(header[i], firstPosOfItemInHeader[i]);
//        }
    }

    /**
     * To string
     */
    public String toString() {
    	/*System.out.println("ULinkList{" +
                "transaction=" + Arrays.toString(transaction) +
                ", remainUtility=" + Arrays.toString(remainUtility) +
                ", nextPos=" + Arrays.toString(nextPos) +
                ", header=" + Arrays.toString(header) +
                ", firstPosOfItemInHeader=" + Arrays.toString(firstPosOfItemInHeader) +
                ", lastPosOfItemByInd=" + Arrays.toString(lastPosOfItemInHeader) +
                ", whichItemset=" + Arrays.toString(whichItemset) +
                ", nextItemsetPos=" + Arrays.toString(nextItemsetPos) +
                ", transactionUtility=" + transactionUtility +
                '}');*/
    	
        return "ULinkList{" +
                "transaction=" + Arrays.toString(transaction) +
                ", remainUtility=" + Arrays.toString(remainUtility) +
                ", nextPos=" + Arrays.toString(nextPos) +
                ", header=" + Arrays.toString(header) +
                //", firstPosOfItemByName=" + firstPosOfItemInHeader +
                ", lastPosOfItemByInd=" + Arrays.toString(lastPosOfItemInHeader) +
                ", whichItemset=" + Arrays.toString(whichItemset) +
                ", nextItemsetPos=" + Arrays.toString(nextItemsetPos) +
                ", transactionUtility=" + transactionUtility +
                '}';
        
        
    }
}
