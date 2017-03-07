/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */

public class TermLevelPRF {
    Properties      prop;
    String          queryPath;      // path of the query file
    File            queryFile;      // the query file
    WordVecs        wordVecs;
    String          nnPath;         // path of the precomputed Nearest Neighbour file
    File            nnFile;

    File        indexFile;          // place where the index is stored
    Analyzer    analyzer;           // the analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    IndexReader     reader;
    IndexSearcher   searcher;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    int             numWanted;      // number of document to retrieve
    String          runName;        // name of the run
    List<TRECQuery> queries;

    double LAMBDA;
    double MU;
    double DELTA;
    int numTopDocs;
    float sigma;
    
    ArrayList <RetrievedDocumentVector> retrievedDocVec;

    CollectionStat collectionStat;

    public TermLevelPRF(String propPath) throws IOException, Exception {
        prop = new Properties();
        prop.load(new FileReader(propPath));
        /* property files are loaded */

        /* setting the anlyzer */
        SetAndGetAnalyzer setGetAnalyzer = new SetAndGetAnalyzer(prop);
        setGetAnalyzer.setAnalyzer();
        analyzer = setGetAnalyzer.getAnalyzer();
        /* analyzer is set */

        /* index path setting */
        System.out.println("Using index at: "+prop.getProperty("indexPath"));
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexFile.getAbsolutePath());
            System.out.println("Terminating");
            boolIndexExists = false;
            System.exit(1);
        }

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));
        searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new DefaultSimilarity());
        searcher.setSimilarity(new LMJelinekMercerSimilarity(0.6f));
        /* reader and searher set */

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        queries = constructQuery();
        /* constructed the query */

        numWanted = Integer.parseInt(prop.getProperty("numWanted"));

        /* setting res path */
        runName = prop.getProperty("runName");
        runName = makeRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        /* res path set */

        /* === For Query Expansion === */
        wordVecs = new WordVecs(prop);
        System.out.println("All word vectors are loaded in worVecs.wordvecmap");
        /* All word vectors are loaded in worVecs.wordvecmap */
        wordVecs.k = Integer.parseInt(prop.getProperty("k"));

//        wordVecs.loadPrecomputedNNs();
        /* precomputed NNs are loaded into memory */
        /* NOT NEEDED */
        
        numTopDocs = Integer.parseInt(prop.getProperty("numreldocs", "100"));
    }

    private String makeRunName_ResFileName() {

        //runName = prop.getProperty("runName");
        resPath = prop.getProperty("resDirPath");
        if(Boolean.parseBoolean(prop.getProperty("rerank"))) {
            runName = "tprf-rerank";
            resPath += "tprf-rerank";
            int queryComposition = Integer.parseInt(prop.getProperty("queryComposition"));
            switch (queryComposition) {
                case 1:
                    runName += "-single";
                    resPath += "-single";
                    break;
                case 2:
                    runName += "-pair";
                    resPath += "-pair";
                    break;
                case 3:
                    runName += "-all";
                    resPath += "-all";
                    break;
                case 4:
                    runName += "-single-pair";
                    resPath += "-single-pair";
                    break;
                case 5:
                    runName += "-pair-all";
                    resPath += "-pair-all";
                    break;
            }
            runName += ("-L-"+Double.parseDouble(prop.getProperty("LAMBDA")));
            resPath += ("-L-"+Double.parseDouble(prop.getProperty("LAMBDA")));
            runName += ("-M-"+Double.parseDouble(prop.getProperty("MU")));
            resPath += ("-M-"+Double.parseDouble(prop.getProperty("MU")));
            if(Boolean.parseBoolean(prop.getProperty("considerNegativeFeedback"))) {
                runName += "-Negative"+"-D-"+Double.parseDouble(prop.getProperty("DELTA"));
                resPath += "-Negative"+"-D-"+Double.parseDouble(prop.getProperty("DELTA"));
            }
        }
        else {
            runName += "baseline";
            resPath += "baseline";
        }
        return runName;
    }

    /**
     * Initialize collectionStat:
     * docCount      - total-number-of-docs-in-index
     * colSize       - collection-size
     * uniqTermCount - unique terms in collection
     * perTermStat   - cf, df of each terms in the collection
     * @throws IOException 
     */
    public void buildCollectionStat() throws IOException {
        
        long colSize = 0;

        collectionStat = new CollectionStat();

        collectionStat.docCount = reader.maxDoc();      // total number of documents in the index

        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(Indexer.FIELD_BOW);
        TermsEnum iterator = terms.iterator(null);
        BytesRef byteRef = null;

        while((byteRef = iterator.next()) != null) {
        //* for each word in the collection
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            int docFreq = iterator.docFreq();           // df of 'term'
            long colFreq = iterator.totalTermFreq();    // cf of 'term'
            collectionStat.perTermStat.put(term, new PerTermStat(term, colFreq, docFreq));
            colSize += colFreq;
        }
        collectionStat.colSize = colSize;               // collection size of the index
        collectionStat.uniqTermCount = collectionStat.perTermStat.size();
    }
    
    public void showCollectionStat() {
        System.out.println("Collection Size: " + collectionStat.colSize);
        System.out.println("Number of documents in collection: " + collectionStat.docCount);
        System.out.println("NUmber of unique terms in collection: " + collectionStat.uniqTermCount);

        for (Map.Entry<String, PerTermStat> entrySet : collectionStat.perTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println(key + " " + value.df);
        }
    }
    
    /**
     * Sets the List-TrecQuery 'queries'.
     * <p/>
     * query file path is read from the prop file.
    */
    private List<TRECQuery> constructQuery() throws FileNotFoundException, IOException {
        TRECQuery trecQuery = new TRECQuery(prop);

        trecQuery.trecQueryParse();
        return trecQuery.queries;
    }

    public void retrieveAll() throws Exception {
        ScoreDoc[] hits;
        TopDocs topDocs;

        for (TRECQuery query : queries) {
            TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
            Query luceneQuery;
            luceneQuery = query.getBOWQuery(analyzer);
            //luceneQuery = getExpandedBOWQuery(analyzer, query);

            //System.out.println(luceneQuery);
            System.out.println("Initial retrieval for query: " + query.qId);
            searcher.search(luceneQuery, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");
            /* initial retrieval done */
            int hits_length = hits.length;

            StringBuffer resBuffer = new StringBuffer();

            if(Boolean.parseBoolean(prop.getProperty("rerank"))) {
                /* +++++ Rerank +++++ */
                System.out.println("Reranking based on TermBasedPRF");
//                if(Integer.parseInt(query.qId)==409)
                {
                List<TrecRes> resList = rerankTermLevelPRF(hits, query);
                /* ----- Rerank ----- */

                /* writing the result in buffer resBuffer */
                int res_length = resList.size();
                for (int r = 0; r < res_length; ++r) {
                    resBuffer.append(resList.get(r).qId).append("\tQ0\t").
                        append(resList.get(r).docId).append("\t").
                        append(r).append("\t").
                        append(resList.get(r).score).append("\t").
                        append(resList.get(r).runName).append("\n");                
                }
                }
            }
            else {
            /* baseline run */
                for (int i = 0; i < hits_length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    resBuffer.append(query.qId).append("\tQ0\t").
                        append(d.get(Indexer.FIELD_ID)).append("\t").
                        append((i)).append("\t").
                        append(hits[i].score).append("\t").
                        append("lm-jm-baseline").append("\n");
                }
            }

            resFileWriter.write(resBuffer.toString());
        }
        resFileWriter.close();
    }

    private QueryCompositions composeQueryTerms(String[] qTerms) {
        QueryCompositions qcomps = new QueryCompositions();
        
        //double composedVector[][];            // the composed vectors of each combinations
        //List<double[]> cv = new ArrayList<>();
        //int noOfcomposition = 0;                // possible number of composed vector from 'words'
        //int dimension;                      // dimension of the word vectors
        //int countComposed;                  // counter of the number of composition done

        int queryComposition = Integer.parseInt(prop.getProperty("queryComposition"));
        if( prop.getProperty("queryComposition") == null ) {
            System.err.println("Missing queryComposition in properties");
            System.exit(1);
        }
        /*
        # queryComposition:-
        ##      Configuration                   Size
        ##  1: individual terms                 m
        ##  2: pairwise                         m-1
        ##  3: all terms together               1
        ##  4: individual and pairwise          2m-1
        ##  5: pairwise and all term together   m
        */
        int qTitleTermsLength = qTerms.length;

        /*
        if(queryComposition == 1 || queryComposition == 5)
            noOfcomposition = qTitleTermsLength;
        switch(queryComposition){
            case 1:
                noOfcomposition = qTitleTermsLength;
                break;
            case 2:
                noOfcomposition = (qTitleTermsLength>1)?(qTitleTermsLength - 1):1;
                break;
            case 3:
                noOfcomposition = 1;
                break;
            case 4:
                noOfcomposition = 2 * qTitleTermsLength - 1;
                break;
            case 5:
                noOfcomposition = qTitleTermsLength;
                break;
        }
        */
        
        //dimension = wordVecs.wordvecmap.get(qTerms[0]).vec.length;
        //composedVector = new double[noOfcomposition][dimension];

        //countComposed = 0;
        /* 1&4: taking only the terms of the query */
        if (queryComposition == 1 || (queryComposition == 2 && qTitleTermsLength<2) ) {
            System.out.println("Query terms alone:");
            for (String word : qTerms) {
                WordVec wv = wordVecs.wordvecmap.get(word);
                qcomps.addComposition(new QueryComposition(word, wv)); //cv.add(wv.vec);
            }
        }

        /* 2&4: pairwise composition */
        if( (queryComposition == 2 || queryComposition == 4) ) {
            System.out.println("ComposingPairWise:");
            
            for (int i = 0; i < qTitleTermsLength-1; i++) {
                WordVec wv1 = wordVecs.wordvecmap.get(qTerms[i]);
                WordVec wv2 = wordVecs.wordvecmap.get(qTerms[i+1]);
                
                if (wv1 != null && wv2 != null){
                    System.out.printf("<%s> <%s>", qTerms[i], qTerms[i+1]);
                    WordVec sum = WordVec.add(wv1, wv2);
                    //for (int j=0; j<dimension; j++) {
                    //    composedVector[countComposed][j] += (wv1.vec[j]/wv1.norm + wv2.vec[j]/wv2.norm);
                    //}
                    
                    String[] qterms = new String[2];
                    qterms[0] = qTerms[i];
                    qterms[1] = qTerms[i+1];
                    qcomps.addComposition(new QueryComposition(qterms, sum)); //cv.add(composedVector[countComposed]);
                    //countComposed ++;
                }
            }
        }

        
        /* 3&5: Composing all the terms of the query together
        if(qTitleTermsLength>2) {
            if(queryComposition == 3 || queryComposition == 5) {
                System.out.println("ComposingAll:");
                for(String word : qTerms) {
                    WordVec wv = wordVecs.wordvecmap.get(word);
                    if(wv != null) {
                        System.out.printf("<%s> ", word);
                        for(int i=0; i<dimension; i++) {
                            composedVector[countComposed][i] += wv.vec[i]/wv.norm;
                        }
                        cv.add(composedVector[countComposed]);
                    }
                }
                System.out.println("");
                countComposed ++;
            }
        }
        */
        
        /* countComposed = number of composed vectors formed */
        return qcomps;
    }

    private InitialRetrStats initialRetrievedWordVocabularyBuilding(ScoreDoc[] hits, TRECQuery query, int topNumDocs) throws IOException {

        InitialRetrStats initRetrStats = new InitialRetrStats(this, hits, query, topNumDocs);
        /*
        for (String term : initRetrStats.termStats.keySet()) {
            QueryCompositions comps = initRetrStats.termStats.get(term);
            if (comps.wvec == null) {
                System.out.println(term);
            }
        }
        */
        initRetrStats.normalizeTermFreqs();

        return initRetrStats;
    }

    /** For each term of the initial retrieved documents,
     *  calculate the similarity of the term with the query (Vector similarity)
     * @param 1. Initial retrieved hits, 2. query
    */
    private List<TrecRes> rerankTermLevelPRF(ScoreDoc[] hits, TRECQuery query) throws IOException, Exception {
        int hits_length = hits.length;
        InitialRetrStats initRetrStats;

        String[] qTitleTerms = query.qTitle.split("\\s+");

        /* +++++ Query vector composition ----- */
        //List<double[]> composedVector = null;           // the composed vectors of each combinations
        //int countComposed;                  // counter of the number of composition done

        System.out.println("Composed Vector computation started");
        QueryCompositions qcompsitions = composeQueryTerms(qTitleTerms);
        //composedVector = qc.sims;
        
        System.out.println("Composed Vector computed");
        //countComposed = composedVector.size();  ///* countComposed = number of composed vectors formed 
        /* ----- Query vector composition done ----- */

        /* +++++ the initial Vocabulary making +++++ */
        retrievedDocVec = new ArrayList<>();
        initRetrStats = initialRetrievedWordVocabularyBuilding(hits, query, numTopDocs);
        System.out.println("Initial retrieved vocabulary built");
        /* ----- Initial vocabulary built ----- */

        /* +++++ Query similarity calculation +++++ */

        /* For each of the composed query vectors:
            For each of the initially retrieved words, 
            calculate the max similarity with the query terms
        */
        //System.out.println("Started computing distances for all query terms");

        initRetrStats.computeSimsWithQueryCompositions();

        /*
        //System.out.println("Sorting started");
        List<Map.Entry<String, QueryCompositions>> initialRetrievedWord_List = new ArrayList<>(initialRetrVocabMap.entrySet());
        
            Collections.sort(
                initialRetrievedWord_List, new Comparator<Map.Entry<String, QueryCompositions>>() {
                    @Override
                    public int compare(Map.Entry<String, SimilarWords> r1,
                        Map.Entry<String, SimilarWords> r2) {
                        return Double.compare(r2.getValue().querySim, r1.getValue().querySim);
                    }
                }
            );
        }
        */
        
        /*
        for (int i=0; i<initialRetrievedWord_List.size(); i++)
            System.out.println(initialRetrievedWord_List.get(i).getValue().word+
                " "+initialRetrievedWord_List.get(i).getValue().querySim+" "+
                initialRetrievedWord_List.get(i).getValue().tf+" "+initialRetrievedWord_List.get(i).getValue().df);
        */

        /* Query similarities sorted in the decreasing order of similarity */

        /* ++++ Reranking +++++ */
        int numberOfRelTerms;       // the number of terms which will be treated as relevant
        numberOfRelTerms = Integer.parseInt(prop.getProperty("numberOfRelTerms","500"));
        int sizeOfR = 0;            // size of the Relevant Set
        int sizeOfNr = 0;           // size of the Non-relevant set
        long sizeOfD = 0;            // size of the Document under consideration
        List<TrecRes> resList = new ArrayList<>();

        //int initialVocSize = initialRetrVocabMap.size();
        LAMBDA = Double.parseDouble(prop.getProperty("LAMBDA"));
        MU = Double.parseDouble(prop.getProperty("MU"));
        DELTA = Double.parseDouble(prop.getProperty("DELTA"));
        sigma = Float.parseFloat(prop.getProperty("sigma", "2"));

//        double collectionSize = reader.getSumDocFreq(Indexer.FIELD_BOW);
        double collectionSize = collectionStat.colSize;

        //HashMap<String, Double> similarityScoreComponent_HashMap = new HashMap<>();

        System.out.println("pR calculation started");
        //HashMap<String, Double> relevantSet = new HashMap<>();
        
        initRetrStats.computeP_R();        
        System.out.println("pR calculated");
        
        /* +++ Considering NonRelevant terms +++ */
        /*
        HashMap<String, Double> nonRelevantSet = new HashMap<>();
        boolean considerNegativeFeedback = Boolean.parseBoolean(prop.getProperty("considerNegativeFeedback", "false"));
        
        if (considerNegativeFeedback) {

            for (int j = initialVocSize-1; j >= initialVocSize - numberOfRelTerms; j--) 
                sizeOfNr += initialRetrievedWord_List.get(j).getValue().tf;

            for (int j = initialVocSize-1; j >= initialVocSize - numberOfRelTerms; j--) {
                String term = initialRetrievedWord_List.get(j).getKey();
                SimilarWords value = initialRetrievedWord_List.get(j).getValue();

                Term t = new Term(Indexer.FIELD_BOW, term);
//                int dfD = reader.docFreq(t);
                long dfD = collectionStat.perTermStat.get(term).df;
                //System.out.println(dfD);

                double pNRt = DELTA * ((double)value.tf/(double)sizeOfNr)
                    + (1-DELTA) * ((double)dfD / collectionSize);

                //* (1/sqrt(2*PI*SIGMA)) * e^((-1/2)*(sim(t,q)/SIGMA)^2)
                //* SIGMA = 1
                pNRt = pNRt * similarityScoreComponent_HashMap.get(term);

                    nonRelevantSet.put(term, pNRt);
            }
            System.out.println("pNR calculated");
        }
        */
        /* --- Considering NonRelevant terms --- */

        InitialRetrStats filteredStats = new InitialRetStatsFilterer(initRetrStats, numberOfRelTerms).filter();
            
        // +++
        /* for each of the initial retrieved documents */
        for (int i = 0; i < hits_length; i++) {            

            double kld = 0;             // KL-Divergence
            double kldNr = 1;           // KL-Divergence with NonRelevant set

            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            double pR = 0, pD = 0, pNr = 0;
            
            HashMap<String, Long> singletonDoc = retrievedDocVec.get(i).perTerm;  // contain term frequency of each term of the document
            sizeOfD = retrievedDocVec.get(i).docLength;

            //* for each term in top R
            for (String term : filteredStats.termStats.keySet()) {
                
                long tfD = 0;
                Long tfDLObj = singletonDoc.get(term);
                if (tfDLObj == null)    
                // the term is not present in the documents under consideration
                    tfD = 0;
                else
                    tfD = tfDLObj.longValue();

                long dfD = collectionStat.perTermStat.get(term).df;
//                System.out.println(term + " " + tfD + " " + dfD);

                /* pD = Lambda*P(t from D) + (1-Lambda)*P(t from C) */
                pD = Math.log(1 + LAMBDA/(1-LAMBDA)* tfD/(double)sizeOfD * collectionSize/dfD);// * similarityScoreComponent_HashMap.get(term);

                pR = filteredStats.getP_R(term);
                kld += (pR*log(pR/pD));
                /*
                pNr = nonRelevantSet.get(term);
                kldNr += (pNr*log(pNr)) - (pNr*log(pD));
                */
                //System.out.println(kld);
            }
//            System.out.println(kld);
//            System.out.println("For each term in top R ends");
            /* --- For each term in top R or, V --- */

            float score;
//            score = 1 / kld;
            
            //score = kldNr / kld;
            score = (float)-kld;

            TrecRes tres = new TrecRes(query.qId, d.get(Indexer.FIELD_ID) , i, score/*hits[i].score*/, runName);

            tres.luceneDocId = hits[i].doc;
            resList.add(tres);

        }
        Collections.sort (resList, new Comparator<TrecRes>(){
            @Override
            public int compare(TrecRes r1, TrecRes r2){
                return r1.score < r2.score? 1 : r1.score == r2.score? 0 : -1;
            }}
        );

        return resList;
        /* ----- Reranking ----- */
    }


    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            System.out.printf("Usage: java TermLevelPRF <init.properties>\n");
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
        }

        TermLevelPRF prf = new TermLevelPRF(args[0]);

        prf.buildCollectionStat();
//        prf.showCollectionStat();

        prf.retrieveAll();

    }

}

class RetrievedDocumentVector {
    long luceneDocId;
    long docLength;
    HashMap<String, Long> perTerm;  // term, tf

    public RetrievedDocumentVector() {
        luceneDocId = 0;
        docLength = 0;
        perTerm = new HashMap<>();
    }
}

class CollectionStat {
    int docCount;
    long colSize;
    int uniqTermCount; 
    /* NOTE: This value is different from luke-Number Of Terms 
        I think luke-Number Of Terms = docCount+uniqTermCount.
        It is same in this case
    */
    HashMap<String, PerTermStat> perTermStat;

    public CollectionStat() {
        perTermStat = new HashMap<>();
    }
}

class PerTermStat {
    String term;
    long cf;
    long df;

    public PerTermStat() {        
    }

    public PerTermStat(String term, long cf, long df) {
        this.term = term;
        this.cf = cf;
        this.df = df;
    }
}
