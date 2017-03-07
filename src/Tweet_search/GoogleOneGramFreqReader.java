/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.BufferedReader;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;

/**
 *
 * @author ayan
 */



public class GoogleOneGramFreqReader extends AnalyzerClass{
    HashMap<String, Long> GoogleOneGram;
    Long Total_number_of_term_freq;
    Analyzer analyzer;
    
    /**
     *
     * @throws java.io.IOException aaa bbb
     */
    public GoogleOneGramFreqReader() throws IOException{
       GoogleOneGram=new HashMap<>(); 
       Total_number_of_term_freq=new Long("0");
       setAnalyzer();
       
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException, Exception {
        int indexcount=0;
        GoogleOneGramFreqReader G1g= new GoogleOneGramFreqReader();
        G1g.createGoogleHashMap("/c/Google-n-gram/DVD-1/data/1gms", G1g, indexcount);
    
    }
public void createGoogleHashMap(String s, GoogleOneGramFreqReader G1g, int indexcount) throws Exception{
    File folder = new File(s);
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
       if (file.isFile() && file.getName() != "total") {
        System.out.println(file.getAbsoluteFile());
        G1g.PutGoogleOneGramIntoHashmap(file.getAbsoluteFile().toString(), G1g,indexcount);
        }
    }
    
}
    
public void PutGoogleOneGramIntoHashmap(String file_name, GoogleOneGramFreqReader G1g, int indexcount) throws IOException, Exception
{
          /**
     *
     * @param doi_cid_map_path: path of the doi-cid-map file
     * @return doiCidMap: doi-cid map (a list) of all the papers; SORTED on DOI.
     *  To look-up the corresponding CID of a DOI, Collection.BinarySearch can be applied.
     * @throws Exception aaa bbb
     */
        TRECQuery trecquery= new TRECQuery();
        setAnalyzer();
        
        
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        
        GetCollStat getCollStat = new GetCollStat(setPropFile.propFileName,indexcount);
        getCollStat.buildCollectionStat();
        PerTermStat pts;
        
        Long ecf; //existing cf
         
    
        BufferedReader br;
        String s;
        br = new BufferedReader(new FileReader(file_name));
        String []tokens;
        String analyzedterm;
        while ((s = br.readLine()) != null) {
            tokens = s.trim().split("[ \t]+");
            analyzedterm = trecquery.queryFieldAnalyze(analyzer, tokens[0].replaceAll("\\s+", "")).replaceAll("\\s+", "");
                    //         System.out.println("term= "+analyzedterm.toString()+" cf="+tokens[1]);
            if(analyzedterm.length()>0)
            {
              
              pts=getCollStat.collectionStat.perTermStat.get(analyzedterm.trim());
           //    System.out.println("term= <"+pts.term+"> cf="+tokens[1]);
               try{
               if(pts !=null)
               {
                   ecf=G1g.GoogleOneGram.get(analyzedterm);
                   if(ecf==null)
                   {
                    //System.out.println("term= "+analyzedterm+" cf="+tokens[1]); 
                   G1g.GoogleOneGram.put(analyzedterm, Long.parseLong(tokens[1]));
                 //  System.out.println(analyzedterm.toString()+" cf="+tokens[1]);
                   }
                   else
                   {
                       ecf+=Long.parseLong(tokens[1]);
                       G1g.GoogleOneGram.put(analyzedterm, ecf);
                      // System.out.println(analyzedterm.toString()+" cf="+tokens[1]);
                   }
                   if(ecf!=null)
                       G1g.Total_number_of_term_freq+=ecf.longValue();
                   
               } 
               }
               catch (NullPointerException e){System.out.println(e);}
               
            }
         //   tokens[0],Integer.parseInt(tokens[1])
        }
    
 
}
}