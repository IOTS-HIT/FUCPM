/**
 * Copyright (C), 2015-2021, HITSZ
 * FileName: ULlist
 * Description: UL-list structure proposed in HUCPMiner paper.
 */
package HUCPMiner;


import java.util.HashMap;

public class ULList {
    //element in ULList
    public class ULLElement {

        //utility
        public int iutil;

        //rest utility
        public int rutil;

        //next item
        public int nextitem;

        public ULLElement(){}

        public ULLElement( int iutil, int rutil, int nextitem) {
            this.iutil=iutil;
            this.rutil=rutil;
            this.nextitem = nextitem;
        }
    }

    //key1：sid; key2：index (i.e. itemset id)
    HashMap<Integer,HashMap<Integer,ULLElement>> ullist;

    public ULList() {
        ullist = new HashMap<>();
    }

    public void add(int sid, int index, int iutil, int rutil,int nextitem) {
        ULLElement ullElement = new ULLElement(iutil,rutil,nextitem);
        if(ullist.get(sid) == null){
            HashMap<Integer,ULLElement> tempMap = new HashMap<>();
            tempMap.put(index,ullElement);
            ullist.put(sid,tempMap);
        }else{
            HashMap<Integer,ULLElement> tempMap = ullist.get(sid);
            tempMap.put(index,ullElement);
        }
    }
}