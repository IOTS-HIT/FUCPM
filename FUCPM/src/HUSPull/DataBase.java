package HUSPull;

/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */
public class DataBase {
    // total utility of DB
    private double databaseUtility;
    // store all sequences
    private Transaction[] transactions;

    /**
     * build the database
     * 
     * @param transactions
     * @param databaseUtility
     */
    public DataBase(Transaction[] transactions, double databaseUtility) {
        this.transactions = transactions;
        this.databaseUtility = databaseUtility;
    }

    public Transaction get(int ind) {
        return transactions[ind];
    }

    public double utility() {
        return databaseUtility;
    }

    public int length() {
        return transactions.length;
    }

    /**
     * Get the maximum item name in database
     * 
     * @return
     */
    public int getMaxItemName() {
        int maxItemName = -1;
        
        // for each sequence in database
        for (Transaction transaction: transactions) {
            for (int j = 0; j < transaction.length(); ++j) {
                int curItemName = transaction.get(j).itemName();
                if (maxItemName < curItemName) {
                    maxItemName = curItemName;
                }
            }
        }
        
        return maxItemName;
    }

    /**
     * Get maximum TXNLen
     * 
     * @return
     */
    public int getMaxTXNLen() {
        int maxTXNLen = -1;
        
        // for each sequence in database
        for (Transaction transaction: transactions) {
            maxTXNLen = Math.max(maxTXNLen, transaction.length());
        }
        
        return maxTXNLen;
    }

    @Override
    public String toString() {
        String ret = "DataBase Utility: " + databaseUtility + "\n";
        for (Transaction transaction: transactions) {
            ret += transaction + "\n";
        }
        return ret;
    }
}