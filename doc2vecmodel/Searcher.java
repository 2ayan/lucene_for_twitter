/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
class Sentence implements Comparable {
    int index;
    double sim;
    
    @Override
    public int compareTo(Object t) {
        Sentence s = (Sentence)t;
        return sim < s.sim? 1 : sim == s.sim? 0 : -1;
    }
}

public class Searcher {
    Properties  prop;               // prop of the init.properties file
//    String      collPath;           // path of the collection
//    String      collSpecPath;       // path of the collection spec file
//    File        collDir;            // collection Directory
    File        indexFile;          // place where the index is stored
    Analyzer    analyzer;           // the analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
//    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
//    int         docIndexedCounter;
    IndexReader     reader;
    IndexSearcher   searcher;
    String          queryPath;      // path of the query file
    File            queryFile;      // the query file
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    int             numWanted;      // number of document to retrieve
    String          runName;        // name of the run
    List<TRECQuery> queries;


    public Searcher(String propPath) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propPath));
        /* property files are loaded */

        /* setting the anlyzer */
        setAnalyzer();
        analyzer = getAnalyzer();
        /* analyzer is set */

        /* index path setting */
        System.out.println("Using index at: "+prop.getProperty("indexPath"));
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexFile.getAbsolutePath());
            System.out.println("Terminating");
            boolIndexExists = true;
            System.exit(1);
        }

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new DefaultSimilarity());
        /* reader and searher set */

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        /* query path set */
        
        queries = constructQuery();

        /* setting res path */
        resPath = prop.getProperty("resPath");
        resFileWriter = new FileWriter(resPath);
        /* res path set */

        numWanted = Integer.parseInt(prop.getProperty("numWanted"));
        
        runName = prop.getProperty("runName");
    }

    public final void setAnalyzer() {
        String stopFile = prop.getProperty("stopFile");
        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close(); fr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));
    }
    
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    
    private List<TRECQuery> constructQuery() throws FileNotFoundException, IOException {
        TRECQuery trecQuery = new TRECQuery(prop);

        trecQuery.trecQueryParse();
        return trecQuery.queries;
    }

    /* calculates the cosine similarity of two vectors */
    public float calculateCosSim(double v1[], double v2[], int dimension) {
        float dotProduct = 0.0f;
        float magnitudeV1 = 0.0f;
        float magnitudeV2 = 0.0f;
        float euclideanProduct;

        for (int i = 0; i < dimension; i++) {
            dotProduct += v1[i]*v2[i];
            magnitudeV1 += v1[i]*v1[i];
            magnitudeV2 += v2[i]*v2[i];
        }

        euclideanProduct = (float) (Math.sqrt(magnitudeV1) * Math.sqrt(magnitudeV2));
        return dotProduct / euclideanProduct;
    }

    private void retrieveAll() throws Exception {
        ScoreDoc[] hits;
        TopDocs topDocs;
        int []sentCountPerDoc;       // will contain the sentence count of each document retrieved for each query
        int totalNumSent;          // will contain the total number of sentences for a particular query, in the rel-docs
        int hits_length;

        String doc2vecPyPath;       // path of the python doc2vec code
        String sentenceDocPath;     // path of the sentence file to be trained by Doc2Vec
        String sentenceVecPath;     // path of the vector file generated from Doc2Vec
        
        doc2vecPyPath = "/home/dwaipayan/doc2vecModel/src/doc2vecmodel/doc2vec.py";
        sentenceDocPath = "/tmp/sentenceDoc.tmp";
        sentenceVecPath = "/tmp/sentenceVector.txt";

        String vectorDumpPath;      // path of the parent directory to contain the vector dumps
        String vectorPath;          // path of file in which the vector for a set of relevant documents are stored
        String sentenceCountPath;   // path of file in which the sentence count of a set of relevant document are saved

        StringBuffer resBuffer;
        List<TrecRes> resList;

        vectorDumpPath = String.format("/home/dwaipayan/relevantSentenceVector-doc2vec");

        for (TRECQuery query : queries) {
            TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
            Query luceneQuery;
            luceneQuery = query.getBOWQuery(analyzer);
            totalNumSent = 0;

//            System.out.println(luceneQuery);

            if(Boolean.parseBoolean(prop.getProperty("rerank"))) {
                System.out.println("Reranking the result using sentence vector similatiry");
                //FileReader vectorFile = new FileReader(String.format("%s/%s.vec", vectorDumpPath, query.qId));
                //FileReader sentenceCountFile = new FileReader(String.format("%s/%s.sent-count", vectorDumpPath, query.qId));
                BufferedReader sentenceCountFile = new BufferedReader(new FileReader(String.format("%s/%s.sent-count", vectorDumpPath, query.qId)));
                BufferedReader vectorFile = new BufferedReader(new FileReader(String.format("%s/%s.vec", vectorDumpPath, query.qId)));

                String line;
                String val[];
//                int totalNumSent;       // total number of sentence in the vector file

                /* the first line is read to extract hits-length and
                    total number of sentences present in rel-docs for that query */
                line = sentenceCountFile.readLine();
                val = line.split(" ");
                hits_length = Integer.parseInt(val[0]);
                totalNumSent = Integer.parseInt(val[1]);
                double queryTitleVector[] = new double[200];
                double queryDescVector[] = new double[200];
                double vectors[][] = new double[totalNumSent][200];
                Sentence sentSims[] = new Sentence[totalNumSent];
                for (int i = 0; i < sentSims.length; i++)
                    sentSims[i] = new Sentence();

                /* reading the query-title vector */
                line = vectorFile.readLine();
                val = line.split(" ");
                for (int i = 1; i <= 200; i++) { // first word of each line is the sentence-vector number 
                    queryTitleVector[i-1] = Double.parseDouble(val[i]);
                }
                /* reading the query-desc vector */
                line = vectorFile.readLine();
                val = line.split(" ");
                for (int i = 1; i <= 200; i++) { // first word of each line is the sentence-vector number 
                    queryDescVector[i-1] = Double.parseDouble(val[i]);
                }

                for (int i = 0; i < totalNumSent; i++) {                        
                line = vectorFile.readLine();
                    val = line.split(" ");
                    for (int j = 1; j <= 200; j++) { // first word of each line is the sentence-vector number 
                        vectors[i][j-1] = Double.parseDouble(val[j]);
                    }
                }
                System.out.println("All vectors are read into memory");

                resList = new ArrayList<>();

                String collectionDocId;
                int luceneDocId;
                int noOfSentSingleDoc;
                int startingPoint = 0;          // starting point for a document-vector in the total vector file
                //while((line=sentenceCountFile.readLine())!=null) {
                for (int i = 0; i < hits_length; ++i) {
                    float sumScoreTitle = (float) 0.0;
                    float sumScoreDesc = (float) 0.0;
                    line=sentenceCountFile.readLine();
                    val = line.split(" ");
                    collectionDocId     = val[0];
                    luceneDocId         = Integer.parseInt(val[1]);
                    noOfSentSingleDoc   = Integer.parseInt(val[2]);
                    for (int j = startingPoint; j < startingPoint+noOfSentSingleDoc; j++) {
                        //System.out.print(j+" ");
                        //System.out.println(noOfSentSingleDoc);
                        if(j==81305)
                            j=j;
                        
                        sentSims[j].sim = calculateCosSim(queryTitleVector, vectors[j], 200);
                        sentSims[j].index = j;
                        sumScoreTitle += sentSims[j].sim;
                        //sumScoreDesc += calculateCosSim(queryDescVector, vectors[j], 200);
                        //System.out.println(calculateCosSim(queryTitleVector, vectors[j], 200));
                        //System.out.println(calculateCosSim(queryDescVector, vectors[j], 200));
                    }
                    startingPoint += noOfSentSingleDoc;

                    float totalScore = (sumScoreTitle/noOfSentSingleDoc)+(sumScoreDesc/noOfSentSingleDoc);
                    TrecRes tres = new TrecRes(query.qId, collectionDocId, luceneDocId, i, totalScore, runName);
                    resList.add(tres);
                }
                Arrays.sort(sentSims);
                for (int i = 0; i < Math.min(10, sentSims.length); i++) {
                    System.out.println(sentSims[i].index+" "+sentSims[i].sim);
                }
                System.out.println(sentSims[81305].sim);
                
                Collections.sort(resList, new Comparator<TrecRes>()
                    {
                        @Override
                        public int compare(TrecRes o1, TrecRes o2) {
                            if (o2.getScores() == o1.getScores())
                                return 0;
                            else if(o2.getScores() < o1.getScores())
                                return -1;
                            else
                                return 1;
                        }
                    });

                resBuffer = new StringBuffer();
    //            System.out.println("Retrieved Length: " + hits_length);
                for (int i = 0; i < hits_length; ++i) {
                    resBuffer.append(query.qId).append("\tQ0\t").
                        append(resList.get(i).docId).append("\t").
                        append((i)).append("\t").
                        append(resList.get(i).score).append("\t").
                        append(runName).append("\n");
                }
                resFileWriter.write(resBuffer.toString());
                /* writing complete for one query */
                break;

            }
            else if(Boolean.parseBoolean(prop.getProperty("useD2v"))) {
                System.out.println("Retrieving results for query: " + query.qId);

                String totalQuery;              // query to learn the doc2vec 
                totalQuery = query.qTitle+"\n"+ query.qDesc+"\n";

                searcher.search(luceneQuery, collector);
                topDocs = collector.topDocs();
                hits = topDocs.scoreDocs;
                if(hits == null)
                    System.out.println("Nothing found");
                hits_length = hits.length;

                resList = new ArrayList<>();
                List<SentenceRes> sentenceResLIst = new ArrayList<>();

                sentCountPerDoc = new int[hits_length];      // initializing the array with the size of the hits_length

                for (int i = 0; i < hits_length; ++i) {
                    Document d = searcher.doc(hits[i].doc);
                    String docId = d.get(Indexer.FIELD_ID);

                    TrecRes tres = new TrecRes(query.qId, docId, hits[i].doc, i, hits[i].score, runName);
                    resList.add(tres);
                    SentenceRes sentRes = new SentenceRes(query.qId, docId, hits[i].doc, d.get(Indexer.FIELD_RAW), i, hits[i].score, runName);
                    sentenceResLIst.add(sentRes);
                    sentCountPerDoc[i] = d.get(Indexer.FIELD_RAW).split("\n").length;
                    totalNumSent += sentCountPerDoc[i];
                }

                FileWriter fwSentenceDoc = new FileWriter(sentenceDocPath);
                /* writing the query-title and query-desc into the file for doc2vec training */
                fwSentenceDoc.write(totalQuery);
                for (int i=0; i<hits_length; i++) {
                    /* writing the rel-doc sentences into file will be used for training */
                    //System.out.println(i);
                    fwSentenceDoc.write(sentenceResLIst.get(i).sentence);
                }
                fwSentenceDoc.close();
                /* sentenceDocPath contains the contains the query title and desc, as well as
                    all the sentences of the documents retrieved relevant for the query 
                */

                /* Using Process.getRuntime() */
                //Process p = Runtime.getRuntime().exec("python /home/dwaipayan/doc2vecModel/src/doc2vecmodel/sentenceVector.py");
                String command = String.format("python %s %s %s", doc2vecPyPath, sentenceDocPath, sentenceVecPath);
                Process p = Runtime.getRuntime().exec(command);
                p.waitFor();
                p.destroy();
                //System.exit(1);
                /* vector is stored in: /tmp/sentenceVectors.txt or in sentenceVecPath */
                //System.out.println("vector is stored in: /tmp/sentenceVectors.txt or in sentenceVecPath");

                BufferedReader br = new BufferedReader(new FileReader(sentenceVecPath));
                //String sent_vec_lines = "";
                List<String> sentenceVectorsList = new ArrayList<>();
                String line;
                while((line = br.readLine()) != null) {
                    if (line.startsWith("SENT_")) {
//                        sent_vec_lines = sent_vec_lines + line + "\n";
                        sentenceVectorsList.add(line.substring(5));
                    }
                }
                /* sent_vec_lines has all the vectors of each sentence in unsorted order */
                /* sentenceVectorList has all the vectors of each sentences in unsorted order */
//                System.out.println("sentenceVectorList has all the vectors of each sentences in unsorted order");
                
                List<String> sortedSentenceVectorList;
                sortedSentenceVectorList = Sort.sortStringList(sentenceVectorsList);
//                System.out.println("Sorting done");

                vectorPath = String.format("%s/%s.vec", vectorDumpPath, query.qId);
                sentenceCountPath = String.format("%s/%s.sent-count", vectorDumpPath, query.qId);

                File dumpDir = new File(vectorDumpPath);

                if (!dumpDir.exists())
                {
                    System.out.println("Folder not exist. Creating..");
                    if(dumpDir.mkdirs()==false) {
                        System.err.println("Folder creation error");
                        System.exit(1);
                    }
                }
                FileWriter writer = new FileWriter(vectorPath);
                for (int i=0; i< sortedSentenceVectorList.size(); i++) {
                    writer.write(sortedSentenceVectorList.get(i));
                    writer.write("\n");
                }
                writer.close();

                writer = new FileWriter(sentenceCountPath);
                writer.write(String.format("%d %d\n", hits_length, totalNumSent));
                for (int i=0; i< hits_length; i++) {
                    writer.write(String.format("%s %d %s\n", sentenceResLIst.get(i).docId, 
                        sentenceResLIst.get(i).luceneDocId, new Integer(sentCountPerDoc[i]).toString()));
                }
                writer.close();
                /* written the vector and sentence count in files */

                /* writing the result in the .res file */
                resBuffer = new StringBuffer();
    //            System.out.println("Retrieved Length: " + hits_length);
                for (int i = 0; i < hits_length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    resBuffer.append(query.qId).append("\tQ0\t").
                        append(d.get(Indexer.FIELD_ID)).append("\t").
                        append((i)).append("\t").
                        append(hits[i].score).append("\t").
                        append(runName).append("\n");                
                }
                resFileWriter.write(resBuffer.toString());
                /* writing complete for one query */
                break;
            }

        }
        resFileWriter.close();
    }

    public static void main(String args[]) throws Exception {
        if(args.length == 0) {
            System.out.printf("Usage: java Indexer <init.properties>\n");
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
        }

        Searcher searcher = new Searcher(args[0]);
        searcher.retrieveAll();
    }
}


class Sort {

    /* Not Using */ 
    /*
    public static String sortString(String strList) throws Exception {
        Map<String, String> map=new TreeMap<String, String>();
        String line="";
        String sarr[] = strList.split("\n");
        for (String s : sarr) {
            map.put(getField(s), s);
        }
        String sorted = "";
        FileWriter writer = new FileWriter("fileToWrite");
        for(String val : map.values()){
            writer.write(val);	
            writer.write('\n');
            sorted = sorted + val + "\n";
        }
        writer.close();
        return sorted;
    }

    private static String getField(String line) {
    	return line.split(" ")[0];//extract value you want to sort on
    }
    */
    public static List<String> sortStringList(List<String> strList) throws Exception {
        Collections.sort(strList, new Comparator<String>()
            {
                @Override
                public int compare(String o1, String o2)
                {
                    if (Integer.parseInt(o1.split(" ")[0]) == Integer.parseInt(o2.split(" ")[0]))
                        return 0;
                    else if (Integer.parseInt(o1.split(" ")[0]) < Integer.parseInt(o2.split(" ")[0]))
                        return -1;
                    else
                        return 1;
                }
            });

//        List<String> sorted = new ArrayList<>();
//        for(String val : strList){
//            sorted.add(val);
//        }
//        return sorted;
        return strList;
    }
}