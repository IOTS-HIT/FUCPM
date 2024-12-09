/**
 * Copyright (C), 2015-2020, HITSZ
 * FileName: SeqInfoList
 * Description: SeqInfoList (SIL) is used to store utility and rest utility of each item in q-sequences.
 */

package FUCPM;

import java.util.ArrayList;

public class SeqInfoList {

    //store utility and rest utility of an item in the q-sequence
    class itemInfo{
        int item;
        int utility;
        int restUtility;

        public itemInfo(int item, int utility,int restutility){
            this.item = item;
            this.utility = utility;
            this.restUtility = restutility;
        }
        public int getItem(){
            return item;
        }
        public int getUtility(){
            return utility;
        }
        public int getRestutility(){
            return restUtility;
        }
    }

    // Each element of seqInfo corresponds to an itemset of the q-sequence.
    ArrayList<itemInfo>[] seqInfo;

    public SeqInfoList(int NumOfItemset){
        seqInfo = new ArrayList[NumOfItemset];
        for(int i = 0; i < NumOfItemset; i++){
            seqInfo[i] = new ArrayList<>(1);
        }
    }

    //find the position of item in itemset
    public int ItemsetContainItem(int itemsetID, int item){
        for(int i = 0; i < seqInfo[itemsetID].size(); i++){
            if(seqInfo[itemsetID].get(i).item == item) return i;
            else if(seqInfo[itemsetID].get(i).item > item) break;
        }
        return -1;
    }

    //register an item
    public void registerItem(int itemsetID, int item, int utility, int restutility) {
        seqInfo[itemsetID].add(new itemInfo(item,utility,restutility));
    }

    //transform the SIL into string
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(" SIL \n");
        for(int i = 0; i < seqInfo.length; i++) {
            for(int j = 0;j < seqInfo[i].size(); j++){
                buffer.append( "item: " + seqInfo[i].get(j).item + " utility: " + seqInfo[i].get(j).utility +
                        " restutility: " + seqInfo[i].get(j).restUtility);
            }
            buffer.append("\n");
        }
        buffer.append("\n");
        return buffer.toString();
    }
}
