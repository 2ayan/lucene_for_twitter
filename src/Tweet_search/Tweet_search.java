/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;


import java.io.FileReader;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author dwaipayan
 */


public class Tweet_search {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception aaa bbb
     */
    public static void main(String[] args) throws Exception {
        Properties prop;
        // TODO code application logic here
        CalculateTotalRunTime crt= new CalculateTotalRunTime();

        if (args.length == 0) {
            System.out.println("Usage: java Tweet_search <option1> <optiona2>\n\n");
            System.out.println("<option1> :");
            System.out.println("-i : to index.");
            System.out.println("-r : to retrieve.");
            System.out.println("\n\n<option2> :");
            System.out.println("Enter a properties file name with absolute path.");
            System.exit(0);
        }
        else if (args.length == 1){
            
            if(args[0].contentEquals("-i"))
            {
                CumulativeIndexer indexer = new CumulativeIndexer();

                if(indexer.boolIndexExists==false) {
                    indexer.createIndex();
                    indexer.boolIndexExists = true;
                }
//                if(indexer.boolIndexExists == true && indexer.boolDumpIndex == true) {
//                    indexer.dumpIndex();
//                }
            }
            else if(args[0].contentEquals("-r"))
            {
                TweetSearcher searcher = new TweetSearcher();
                searcher.retrieveAll();
                searcher.close();
            }
            else 
            {
              System.out.println("<option1> Does not match. Use");  
              System.out.println("-i : to index.");
              System.out.println("-r : to retrieve.");
            }
            
        }
        else if (args.length == 2){
            new PropFile().setPropFileName(args[1]);
        }
        
        crt.PrintRunTime();
       
        
    }
}
