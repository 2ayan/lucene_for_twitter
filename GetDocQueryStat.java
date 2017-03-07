/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Math.log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author ayan
 */
public class GetDocQueryStat {
    
    public int total_no_of_docs_in_coll;
    DocVector docVector;
    
    
    public GetDocQueryStat(int indexcount) throws IOException{
      GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
      docVector = new DocVector(setPropFile.propFileName); 
      total_no_of_docs_in_coll=docVector.reader[indexcount].getDocCount("title"); 
      
    }
    
    public static void main(String[] args) throws IOException, ParseException, Exception 
    {
//        if(args.length == 0) {
//            args = new String[2];
//            System.out.println("Usage: java DocVector <prop-file>");
//            args[0] = "/c/lucene_for_twitter/init.properties";
//            args[1] = "/c/twitter-tools/twitter-tools-core/tmp_trec_format.txt";
//            //System.exit(0);
//        }
        /* Get collection path and others from .properties file*/
       GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
       String collPath=setPropFile.prop.getProperty("collPath");
       String googleOneGramPath=setPropFile.prop.getProperty("googleOneGramPath");
       /* Set collection path and others from .properties file*/
       
        GoogleOneGramFreqReader gfr=new GoogleOneGramFreqReader();
        gfr.createGoogleHashMap(googleOneGramPath, gfr);
       
       
       GetDocQueryStat dqs=new GetDocQueryStat(0);

       
       File folder = new File(collPath);
       File[] listOfFiles = folder.listFiles();
       Writer out;
       String output_file_name, output_dir_path="/c/twitter-tools/twitter-tools-core/TREC_MBtrack_2015_coll/TREC_OUTPUT/";

    for (File file : listOfFiles) {
       if (file.isFile()) {
        System.out.println(file.getAbsoluteFile());
        output_file_name=output_dir_path.concat(file.getName());
        System.out.println(output_file_name);
        
        out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(output_file_name), "UTF-8"));
        dqs.QueryDocSimilarity(/*file.getAbsoluteFile().toString(),*/out, dqs, gfr);
        out.flush();
        out.close();
        }
    }
       
       
       
    }
    
  
    
//    
//    public void QueryDocSimilarity(/*String query_file_name, */Writer out, GetDocQueryStat dqs, GoogleOneGramFreqReader gfr) throws IOException, Exception
//    {
//        
//        Fields fields;
//        Iterator hashmap_itr, term_fields_itr;
//        Object element;
//        Terms term_field_name;
//        TermsEnum termsEnum = null;
//        BytesRef text;
//        String term;
//        Integer etf;
//        HashMap<String, Integer>termsfromadoc;
//        Long cf,df;
//        Integer tf;
//        Double similarity;  
//        
//        TRECQuery trecquery= new TRECQuery();
//    	//trecquery.trecQueryParse(query_file_name);
//        trecquery.trecQueryParse();
//        AnalyzerClass analyzerClass= new AnalyzerClass();
//        analyzerClass.setAnalyzer();
//        
//        
//       for(int i=0;i<dqs.total_no_of_docs_in_coll;i++)
//       {
//           
//           fields=dqs.docVector.reader.getTermVectors(i);
//           term_fields_itr=fields.iterator();
//           termsfromadoc=new HashMap<>();
//           while(term_fields_itr.hasNext()) 
//           {
//             element=term_fields_itr.next();
//             
//             term_field_name=fields.terms(element.toString());
//            //   System.err.print(element.toString()+" -->");
//            termsEnum = term_field_name.iterator(termsEnum);
//            while((text = termsEnum.next()) != null)
//            {
//               term = text.utf8ToString();
//               etf=termsfromadoc.get(term);
//               if(etf==null)
//                  termsfromadoc.put(term, 1);
//               else
//                  termsfromadoc.put(term, etf+1);               
//             // System.err.print(term+" ");
//            }
//               //System.err.println("");
//           }
//           
//           //hashmap_itr=termsfromadoc.entrySet().iterator();
//           //while(hashmap_itr.hasNext())
//           //{
//            String atq[];
//            for(int k=0; k<(int)trecquery.queries.size(); k++)
//            {
//            similarity=0.0;
//            String analyzedQuery = trecquery.queryFieldAnalyze(analyzerClass.analyzer, trecquery.queries.get(k).qTitle);
//            atq=analyzedQuery.split(" ");
//            for(int j=0;j<atq.length;j++)
//            {
//               //Map.Entry pair = (Map.Entry) hashmap_itr.next();
//               df=cf=gfr.GoogleOneGram.get(atq[j]);
//               tf=termsfromadoc.get(atq[j]);
//               if(tf != null ){
//                   if(df != null && cf != null && gfr.Total_number_of_term_freq != null) 
//                    similarity += dqs.similarity_BM25(tf, df, cf, gfr.Total_number_of_term_freq);
//                    else
//                       similarity += dqs.similarity_BM25(tf, df+tf, cf+tf, gfr.Total_number_of_term_freq);
//                //System.out.println(atq[j] + " tf=" +tf+" df="+df+" cf="+cf+" sim="+similarity);
//                   
//               }
//             }
//            if(similarity>0.0)
//            //System.out.println(dqs.docVector.reader.document(i).get("docid")+"*"+trecquery.queries.get(k).qId+"*"+trecquery.queries.get(k).qNarr+" sim="+similarity);
//                out.append(dqs.docVector.reader.document(i).get("docid")+" "+trecquery.queries.get(k).qId+" "+trecquery.queries.get(k).qNarr+" "+similarity+"\n");
//           }
//            
//           termsfromadoc.clear();
//           System.err.println("Ends query no. "+i);
//       }
//    }
//    
//      public double similarity_BM25(Integer tf, Long df, Long cf, Long N){
//        double idf=0, sim=0;
//        double k1=1.6;
//        idf=log((N.longValue()-df.longValue()+0.5)/(df.longValue()+0.5));
//        sim=idf*((tf.intValue()*(k1+1))/(tf.intValue()+k1));
//        return sim;
//    }
//    
}
