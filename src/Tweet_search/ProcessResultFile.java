/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author ayan
 */
public class ProcessResultFile{
        Properties prop;        // the properties file
        int SortingOrder;
public ProcessResultFile() throws IOException
{
    GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
    prop=setPropFile.prop;
}

public String [][]readResfileAndPutInAMatrix(String Filename) throws FileNotFoundException, IOException
{
  BufferedReader in = new BufferedReader(new FileReader(Filename));
  String line;
  int line_count=0,col_count=0;
  while ((line = in.readLine()) != null)
  {   if(line_count==0)
        col_count=line.split(" ").length;
      line_count++;
  }
  in.close();
  BufferedReader in1 = new BufferedReader(new FileReader(Filename));
  
  String [][]matrix = new String[line_count][col_count];
  String []buff=null;
  int row=0;
  while ((line = in1.readLine()) != null)
            {
            buff=line.split(" ");
             for(int column=0;column<buff.length;column++)   
                    //System.err.println(buff[column]);
                 matrix[row][column]=buff[column];
             row++;
            }
  in1.close();
  return matrix;
}
public void processResFile(String Filename) throws IOException, Exception
{
   String [][]matrix;
   matrix=this.readResfileAndPutInAMatrix(Filename);
   matrix=this.CalculateTemporalDensityValue(matrix);
   matrix=this.reRankResult(matrix, 1); // Enter 1 to sort in decending order 0 to sort in accending order
    System.err.println("Reranked.");
   
   TweetSearcher ts= new TweetSearcher();
   List<TRECQuery> queries = ts.constructQuery();;
   
   
   int mat_row_no=matrix.length;
   int mat_col_no=matrix[0].length;
   int daily_tweet_max_notification_no=Integer.parseInt(prop.getProperty("daily_tweet_max_notification_no"));
   Filename=Filename.replaceAll("\\.res$", "-trec-output.res");

   FileWriter fw = new FileWriter(Filename);
   String created_day;
   
   for(int i=20;i<30;i++) 
   {
       created_day="201507".concat(Integer.toString(i));
       for (TRECQuery q : queries)
        {
            int notification_count=0;
            for(int row=0;row<mat_row_no;row++)
            {

               StringBuffer buff = new StringBuffer();

               String qno=matrix[row][0];
               String tweetid=matrix[row][2];
               int rank=Integer.parseInt(matrix[row][3]);
               double score=Double.parseDouble(matrix[row][4]);
               String algo=matrix[row][5];
               String DOCNO=matrix[row][6];
               String created_at=matrix[row][7];
               
               if(q.qId.equalsIgnoreCase(qno))
               {    
                    if(created_day.equalsIgnoreCase(created_at))
                    {
                        if(daily_tweet_max_notification_no > notification_count)
                        {
                          buff.append(created_at+" "+qno+" Q0 "+tweetid+" "+(notification_count+1)+" "+score+" "+algo+"\n");
                          fw.write(buff.toString());
                          notification_count++;
                        }
                        else
                            break;
                    }
                    else
                        break;
               }

            }    
        }    
   }
   fw.close();
   
}

public static void main(String[] args) throws Exception {
    ProcessResultFile p=new ProcessResultFile();
    TweetSearcher searcher = new TweetSearcher();
    //p.processResFile(searcher.resultsFile.toString());
    p.processResFile("/c/lucene_for_twitter/var/results/20150828-210704-lm-jm-lambda0.5-qfield-title-narr-desc-topres5000.res");
}   

public String[][] reRankResult(String[][] matrix, int order) {
// Enter order=1 to sort in decending order order=0 to sort in accending order
    this.SortingOrder=order;
    Arrays.sort(matrix, new Comparator<String[]>() {
    @Override
    public int compare(String[] s1, String[] s2) {
        double t1 = Double.parseDouble(s1[4]);
        double t2 = Double.parseDouble(s2[4]);
        
        if(SortingOrder==0) // sorting in assending order
            return (t1>t2) ? 1 : ( (t1<t2) ? -1 : 0) ;
        else // sorting in decending order
            return (t1>t2) ? -1 : ( (t1<t2) ? 1 : 0) ;
            }
        });
    return matrix;
    }

public String[][] CalculateTemporalDensityValue(String[][] matrix) throws Exception {
    int num_ret=Integer.parseInt(prop.getProperty("retrieve.num_wanted"));
    int col_num=matrix[0].length;
    int matrix_size=matrix.length;
    String [][]m=new String[num_ret][col_num];
   
   TweetSearcher ts= new TweetSearcher();
   List<TRECQuery> queries = ts.constructQuery();;
    String created_day;
   
   for(int i=20;i<30;i++) 
   {
       created_day="201507".concat(Integer.toString(i));
       for (TRECQuery q : queries)
        {
            for(int row=0;row<matrix_size;row++)
            {
               int row_m=0;
               for(int col=0; col<col_num;col++)
               {
                if(matrix[row][7].contentEquals(created_day))
                if(col==1)
                    m[row_m][1]="Q0";
                else 
                    m[row_m][col]=matrix[row_m][col];
               }
            }
            
            
        }
    
   }
    return m;
}

}
