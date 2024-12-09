package HUSPull;

import java.util.Arrays;

/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */
public class Transaction {
    private UItem[] uItems;
    private double transactionUtility;

    public Transaction(UItem[] uItems, double transactionUtility) {
        this.uItems = uItems;
        this.transactionUtility = transactionUtility;
    }

    public UItem get(int ind) {
        return uItems[ind];
    }

    public double utility() {
        return transactionUtility;
    }

    public int length() {
        return uItems.length;
    }

    @Override
    public String toString() {
        return "TransactionStr{" +
                "uItems=" + Arrays.toString(uItems) +
                ", transactionUtility=" + transactionUtility +
                '}';
    }
}
