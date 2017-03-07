/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
class TweetSearcher extends AnalyzerClass{

    Properties prop;        // the properties file
    String propFileName;
    IndexReader []reader;
    IndexSearcher []searcher;
    MultiFieldQueryParser mFQueryParser;
    String queryFile;

    List<TRECQuery> queries;

    String resultsFile; // path of the result file

    int num_ret;
    String setSimilarityFlag;
    int num_wanted;
    String run_name;

    int tweet_starts_from_date;
    int tweet_ends_from_date;

    public TweetSearcher() throws IOException, Exception {
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        propFileName=setPropFile.propFileName;
        
        tweet_starts_from_date=Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date=Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));
        
                        /* max number of files to return for each query */
        num_ret=Integer.parseInt(setPropFile.prop.getProperty("retrieve.num_wanted"));
                /* Set Retrieval model set */
        setSimilarityFlag= setPropFile.prop.getProperty("retrieval.model");
        
        String indexDirectoryPath = prop.getProperty("indexPath");
        if(!indexDirectoryPath.endsWith("/"))
            indexDirectoryPath=indexDirectoryPath.concat("/");
        
        reader=new IndexReader[tweet_ends_from_date-tweet_starts_from_date+1];
        searcher=new IndexSearcher[tweet_ends_from_date-tweet_starts_from_date+1];
        for(int i=0;i<tweet_ends_from_date-tweet_starts_from_date+1;i++)
        {
            reader[i] = DirectoryReader.open(FSDirectory.open(new File(indexDirectoryPath.concat(Integer.toString(i+tweet_starts_from_date)))));
            searcher[i] = new IndexSearcher(reader[i]);

        /* setting the similarity function */
        /* 1-BM25, 2-LM-JM, 3-LM-D, 4-DefaultLuceneSimilarity */
        setSimilarityFn_ResFileName(i);
        /* */
        }

        /* setting the fields in which the searching will be perfomed */
        String []fields = setQueryFieldsToSearch();

//        String []fields = new String[]{"abstract", "title", "contexts"};
        /* === */

        /* using the same analyzer which is used for indexing */
        Analyzer engAnalyzer = getAnalyzer();
       mFQueryParser = new MultiFieldQueryParser(fields, engAnalyzer);

//        num_wanted = Integer.parseInt(prop.getProperty("retrieve.num_wanted","100"));
        num_wanted = num_ret;

        
        /* setting query list*/
        queries = constructQuery();

        /* queries has all the RAW data read from the query file like: 
            query_num, paper_title, paper_abtract, context etc.
        */
        String qfield="qfield-";
        for(int i=0;i<fields.length;i++)
        {
            if(i==0)
                qfield=qfield.concat(fields[i]);
            else
                qfield=qfield.concat("-").concat(fields[i]);
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	//get current date time with Date()
	Date date = new Date();
	
        
        resultsFile=dateFormat.format(date).concat("-"+resultsFile).concat("-").
                concat(qfield).concat("-topres").concat(new Integer(num_wanted).toString()).concat(".res");
        
        String path=prop.getProperty("resPath");
        if(!path.endsWith("/"))
            path=path.concat("/");
        resultsFile=path.concat(resultsFile);
        System.out.println("Result will be saved in: "+resultsFile);

    }

    public void close() throws IOException{
        for(int i=0;i<tweet_ends_from_date-tweet_starts_from_date+1;i++)
            reader[i].close();
    }

    /*
    * setFieldsFlag-
    *** 1: fields[] = {"abstract", "title", "contexts"}
    *** 2: fields[] = {"abstract", "title"}
    *** 3: fields[] = {"contexts"}
    */
    private String[] setQueryFieldsToSearch() throws IOException {
        return prop.getProperty("QueryFields2Search").split(",");
    }

    public List<TRECQuery> constructQuery() throws FileNotFoundException, IOException {
        TRECQuery trecQuery = new TRECQuery(prop);

        trecQuery.trecQueryParse();
        return trecQuery.queries;
    }


    /*
    * setSimilarityFlag-
    *** 1: BM25Similarity
    *** 2: LMJelinekMercerSimilarity
    *** 3: LMDirichletSimilarity
    *** 4: DefaultSimilarity
    * BM25Similarity-            f1: k1,    f2: b
    * LMJelinekMercerSimilarity- f1: lambda f2: dummy
    * LMDirichletSimilarity-     f1: mu     f2: dummy
    * DefaultSimilarity-         f1: dummy  f2: dummy
    *
    * setDocField- Search Document:  1-TAC, 2-TA
    * setQueryField- Search Query:  1-TAC, 2-C
    */
    private void setSimilarityFn_ResFileName(int indexcounter) throws IOException {
  
        searcher[indexcounter] = new IndexSearcher(reader[indexcounter]);
        float bm25_k1, bm25_b, lm_jm_lambda, lm_d_mu;


        switch(setSimilarityFlag.toLowerCase()){
            case "bm25":
                bm25_k1=Float.parseFloat(prop.getProperty("bm25-k1"));
                bm25_b=Float.parseFloat(prop.getProperty("bm25-b"));
                
                System.out.println("Setting BM25 with k1: "+bm25_k1+" b: "+bm25_b);
                searcher[indexcounter].setSimilarity(new BM25Similarity (bm25_k1, bm25_b));
                
                run_name = "bm25-k1="+bm25_k1+"-b="+bm25_b+"-"+num_ret;
                break;
            case "lm-jm":
                lm_jm_lambda=Float.parseFloat(prop.getProperty("lm_jm_lambda"));
                
                System.out.println("Setting LMJelinekMercer with lambda: "+lm_jm_lambda);
                searcher[indexcounter].setSimilarity(new LMJelinekMercerSimilarity(lm_jm_lambda));
                    
                run_name = "lm-jm-lambda"+lm_jm_lambda;
                break;
            case "lm-d":
                lm_d_mu=Float.parseFloat(prop.getProperty("lm_d_mu"));
                
                System.out.println("Setting LMDirichlet with mu: "+lm_d_mu);
                searcher[indexcounter].setSimilarity(new LMDirichletSimilarity(lm_d_mu));
                
                run_name = "lm-d-mu="+lm_d_mu+"-";
                break;
  
            case "default":
                System.out.println("Setting DefaultSimilarity of Lucene: ");
                searcher[indexcounter].setSimilarity(new DefaultSimilarity());
                run_name = "default-lucene";
                break;
                
            default:  
                System.out.println("Setting DefaultSimilarity of Lucene: ");
                searcher[indexcounter].setSimilarity(new DefaultSimilarity());
                run_name = "default-lucene";
                break;
        }
       
        resultsFile = run_name;
       
    }

    public void retrieveAll() throws IOException, Exception {
        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

        DocVector docVector = new DocVector(propFileName); 
//        queries = constructQueries();
//        /* queries has all the raw data read from the query file like: 
//            query_num, paper_title, paper_abtract, context etc.
//        */

    
            System.out.println("Using BOW query:");
            
            File file=new File(resultsFile);
            System.out.println("creating directory: " + file.getParentFile().getAbsolutePath());
            if(!file.getParentFile().exists())
            {
                System.out.println("creating directory: " + file.getParentFile().getName());
                boolean result = false;

                try{
                file.getParentFile().mkdir();
                result = true;
                } 
                catch(SecurityException se){
                //handle it
                }        
                if(result) {    
                System.out.println("DIR created");  
                }
            }
            

            FileWriter fw = new FileWriter(resultsFile);
            
            for(int indexcount=0;indexcount<tweet_ends_from_date - tweet_starts_from_date + 1 ;indexcount++)
            {
                int query_searched_count = 0;
                for (TRECQuery q : queries) {

                    System.out.println("Query: "+q.qId+": ");

                    setAnalyzer();
                    Query qry  = q.getBOWQuery(getAnalyzer());


                    TopScoreDocCollector collector = TopScoreDocCollector.create(num_wanted, true);

                    //System.out.println(qry.toString());
                    searcher[indexcount].search(qry, collector);


                    topDocs = collector.topDocs();
                    hits = topDocs.scoreDocs;
                    if(hits == null)
                        System.out.println("Nothing found");

                    /* writing the result in file */
                    StringBuilder buff = new StringBuilder();
                    String d[],date;
                    
                    int hits_length = hits.length;
                    System.err.println("Searching in index of date:"+(indexcount+tweet_starts_from_date));
                    int j=1;
                    for (int i = 0; i < hits_length; ++i) {
                        d=docVector.reader[indexcount].document(hits[i].doc).get("time").split(" ");
                        date=d[5].concat("08"+d[2]);
                        if((Integer.parseInt(d[2])-tweet_starts_from_date) == indexcount && Integer.parseInt(date)>20160000 && j<=100)
                        {
                            buff.append(date+" ").
                                append(q.qId).
                                append(" Q0 ").
                                append(docVector.reader[indexcount].document(hits[i].doc).get("tweettime")).append(" ").
                                append(j++).append(" ").
                                append(hits[i].score).append(" ").
                                append(run_name).append("\n");
                                //append(docVector.reader[indexcount].document(hits[i].doc).get("DOCNO")).append(" ").
                               // append(date).append("\n"); 
                        }
                    }
                    fw.write(buff.toString());
                    /* writing the result in file ends */
                    query_searched_count++;
                }
                
            System.out.println(query_searched_count + " queries searched");
            }
            fw.close();
        

    }

    /* reranking using Translation Model */
  /*  void rerankUsingTM(List<TrecRes> retList) throws IOException {
        BytesRef term = null;
        int N = reader.numDocs();
        
        for (TrecRes trecRes : retList) {
            Terms terms = reader.getTermVector(trecRes.luceneDocId, analyzer);
            if (terms == null || terms.size() == 0)
                continue;            
            
            float score = 0;
            TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
            while ((term = termsEnum.next()) != null) {// explore the terms for this field
                Term t = new Term(Indexer.FIELD_TITLE, term);
                DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one                    
                int docIdEnum;

                while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                    //get the term frequency in the document
                    int tf = docsEnum.freq();
                    int df = (int)(reader.totalTermFreq(t));
                    float idf = N/(float)df;
                    float tfidf = tf*idf;
                    score += tfidf;
                }                    
            }
            trecRes.score = score;
        }
        Collections.sort (retList, new Comparator<TrecRes>(){
            @Override
            public int compare(TrecRes r1, TrecRes r2){
                return r1.score < r2.score? 1 : r1.score == r2.score? 0 : -1;
            }}
        );
    }*/
    /* TM reranking ends */
    
    public static void main(String[] args) throws Exception {

        TweetSearcher searcher = new TweetSearcher();
                
        searcher.retrieveAll();
        
        searcher.close();

    }
}