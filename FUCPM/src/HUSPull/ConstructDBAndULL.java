package HUSPull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */

public class ConstructDBAndULL {
    /**
     * Get database
     * 
     * @param pathname input file pathname
     * @return database
     * <p>
     * input file format: 1[1] 2[4] -1 3[10] -1 6[9] -1 7[2] -1 5[1] -1 -2  SUtility:27
     */
    public static DataBase getDataBase(String pathname) {
        ArrayList<String> rawData = readFileByLine(pathname);

        Transaction[] transactions = new Transaction[rawData.size()];
        double databaseUtility = 0;
        int lineIndex = 0;
        for (String currentLine : rawData) {
//        	System.out.println("===" + currentLine);
        	
            currentLine = currentLine.trim();  // remove blank
            String[] token = currentLine.split(" ");

            // token.length - 3:  sutility:xx
            UItem[] uItems = new UItem[token.length - 3];

            for (int i = 0; i < token.length - 3; ++i) {
                if (token[i].startsWith("-1")) {  // the end of itemset
                    uItems[i] = new UItem(-1, -1);
                } else {  // item[utility]
                    int item = Integer.parseInt(token[i].split("\\[")[0]);
                    double utility = Double.parseDouble(token[i].split("\\[")[1].replace("]", ""));
                    uItems[i] = new UItem(item, utility);
                }
            }

            // substring(9): just return the 9-th substring
            double transactionUtility = Double.parseDouble((token[token.length - 1].substring(9)));
            databaseUtility += transactionUtility;

            transactions[lineIndex] = new Transaction(uItems, transactionUtility);
            lineIndex += 1;
        }
        
        // return the formated database
        return new DataBase(transactions, databaseUtility);
    }


    /**
     * read file by line, used for line-based files...
     * use BufferedReader for buf, avoid much IO
     *
     * @param pathname
     * @return: the array of string (each line data)
     */
    public static ArrayList<String> readFileByLine(String pathname) {
        ArrayList<String> ret = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(pathname), "utf8"));  // FileNotFoundException
            // reader = new BufferedReader(new FileReader(pathname));
            String tempString;
            while ((tempString = reader.readLine()) != null) {  // IOException
                ret.add(tempString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    System.out.println("close error.");
                    e1.printStackTrace();
                }
            }
        }
        
        return ret;
    }


    /**
     * calculate map of item to twu, used to reduce items whose twu < minUtility
     *
     * @param dataBase database
     * @return map of item to twu
     */
    public static HashMap<Integer, Double> getMapItemTwu(DataBase dataBase) {
        HashMap<Integer, Double> mapItemTwu = new HashMap<Integer, Double>();
        
        // for each transaction
        for (int i = 0; i < dataBase.length(); ++i) {
            Transaction transaction = dataBase.get(i);
            
            // for each item in this transaction
            for (int j = 0; j < transaction.length(); ++j) {
                int item = transaction.get(j).itemName();
                double twu = mapItemTwu.getOrDefault(item, 0.0);
                //double twu = mapItemTwu.get(item);
                
                // update the TWU of each item
                mapItemTwu.put(item, twu + transaction.utility());
            }
        }
        
        return mapItemTwu;
    }
    
    
    /**
     * Get ULink-list of DB
     * 
     * @param dataBase:  original dataBase
     * @param minUtility:  the minimum utility threshold
     * @return dataBase:  consists of ULinkList
     */
    public static ULinkList[] getULinkListDB(DataBase dataBase, double minUtility) {
    	// get the set of mapItemTwu by method getMapItemTwu()
        HashMap<Integer, Double> mapItemTwu = ConstructDBAndULL.getMapItemTwu(dataBase);

        // new the set of qMatrices
        ArrayList<ULinkList> uLinkListDB = new ArrayList<ULinkList>();
        
        // process each transaction in database
        for (int i = 0; i < dataBase.length(); ++i) {
            
            // for each transaction
            Transaction transaction = dataBase.get(i);
            // new a revisedTransaction
            ArrayList<UItem> newTransaction = new ArrayList<UItem>();
            
            /**
             * get a new (revised) transaction
             */
            int itemsetNum = 0;  // itemsetNum: the number of itemsets (-1)
            for (int j = 0; j < transaction.length(); ++j) {
                UItem uItem = transaction.get(j);
                if (uItem.itemName() == -1) {
                    itemsetNum += 1;
                    newTransaction.add(uItem);
                } else if (mapItemTwu.get(uItem.itemName()) >= minUtility) {
                    newTransaction.add(uItem);
                }
            }

            /**
             * for a new (revised) transaction
             * 
             * newTransaction.size(): the number of remaining items and -1, except those items:
             * twu < minUtil. newTransaction may be empty (only include -1), means that all
             * items: twu < minUtil. */
            if (newTransaction.size() == itemsetNum) continue;
            
            /**
             * 
             * add to the set of uLinkListDB
             */
            uLinkListDB.add(new ULinkList(newTransaction.toArray(new UItem[newTransaction.size()]), i));
        }
        
        // return the final set of uLinkListDB 
        return uLinkListDB.toArray(new ULinkList[uLinkListDB.size()]);
    }



}
