/**
 * Copyright (C), 2015-2020, HITSZ
 * FileName: InstanceList
 * Description: InstanceList is used to store the utility and position of instances of a pattern
 */

package FUCPM;

import java.util.ArrayList;

public class InstanceList {

    public class InstanceElement {

        //the id of the itemset where the last item of the instance locates
        public int tid;

        //utility of the instance
        public int acu;

        public InstanceElement(int tid, int acu) {
            this.tid = tid;
            this.acu = acu;
        }
    }

    //InstanceList:
    ArrayList<InstanceElement> insList = new ArrayList<>(2);

    //q-sequence id
    public int sid;

    public InstanceList() {
    }

    public void add(int tid, int acu) {
        this.insList.add(new InstanceElement(tid, acu));
    }

    public void set_sid(int sid) {
        this.sid = sid;
    }

    public int get_sid() {
        return this.sid;
    }
}