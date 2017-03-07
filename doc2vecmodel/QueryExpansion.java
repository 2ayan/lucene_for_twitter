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
import java.io.StringReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class QueryExpansion {
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
//    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
//    int         docIndexedCounter;
    IndexReader     reader;
    IndexSearcher   searcher;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    int             numWanted;      // number of document to retrieve
    String          runName;        // name of the run
    List<TRECQuery> queries;

    public QueryExpansion(String propPath) throws IOException, Exception {
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

        /* setting res path */
        resPath = prop.getProperty("resPath");
        resFileWriter = new FileWriter(resPath);
        /* res path set */

        numWanted = Integer.parseInt(prop.getProperty("numWanted"));
        runName = prop.getProperty("runName");

        /* === For Query Expansion === */
        /* reading the precomputed NNs from file */
        if(prop.containsKey("NNDumpPath"))
            nnPath = prop.getProperty("NNDumpPath");
        else {
            System.err.println("NNDumpPath not set in properties");
            System.exit(1);
        }
        wordVecs = new WordVecs(prop);
        /* All word vectors are loaded in worVecs.wordvecmap */
        wordVecs.k = Integer.parseInt(prop.getProperty("k"));

//        wordVecs.loadPrecomputedNNs();
        /* precomputed NNs are loaded into memory */
        /* NOT NEEDED */

    }

    private List<TRECQuery> constructQuery() throws FileNotFoundException, IOException {
        TRECQuery trecQuery = new TRECQuery(prop);

        trecQuery.trecQueryParse();
        return trecQuery.queries;
    }

    String queryFieldAnalyze(Analyzer analyzer, String queryField) throws Exception {
        StringBuffer buff = new StringBuffer(); 
        TokenStream stream = analyzer.tokenStream(Indexer.FIELD_BOW, new StringReader(queryField));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();        
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            buff.append(term).append(" ");
        }
        stream.end();
        stream.close();
        return buff.toString();
    }

    /* returns the cosine similarity of two vectors */
    double calculateCosSim(double v1[], double v2[]) {
        int k = v1.length;
        double dotProduct = 0.0f;
        double magnitudeV1 = 0.0f;
        double magnitudeV2 = 0.0f;
        double euclideanProduct;

        for (int i = 0; i < k; i++) {
            dotProduct += v1[i]*v2[i];
            magnitudeV1 += v1[i]*v1[i];
            magnitudeV2 += v2[i]*v2[i];
        }

        euclideanProduct = (double) (Math.sqrt(magnitudeV1) * Math.sqrt(magnitudeV2));
        return dotProduct / euclideanProduct;
    }

    double calculateDistance(double v1[], double v2[]) {
        int k = v1.length;
        double distance;

        distance = 0;
        for (int i = 0; i < k; i++) {
            distance += v1[i]*v2[i];
        }

        return distance;
    }

    /* Return the mostSimilarWords to the composed forms of titleTerms 
     * n - for each composed form, take n most similar words to that composed form
     * pairwise - true: consider vectors in moving-window-pair; false- consider only all word composed vector 
    */
    LinkedHashMap<String, Double> returnSimilarWords(String words[], int n, boolean pairWise) {
        double composedVector[][];            // the composed vectors of each combinations
        /*
        *   Suppose words[] = {A,B,C}
        *   composedVector[0][] = {ABC}
        *   if pairWise == true: 
        *       composedVector[1][] = {AB}
        *       composedVector[2][] = {BC}
        */
        int noOfcomposition;                // possible number of composed vector from 'words'
        int dimension;                      // dimension of the word vectors
        int countComposed = 0;              // counter of the number of composition done
        double simScore[];
        String simWords[];
        WordVec composedVectorSim[];

        noOfcomposition = (words.length==2)?1:words.length;
        /* A,B=> AB; For all others, it is the number of words */

        dimension = wordVecs.wordvecmap.get(words[0]).vec.length;
        composedVector = new double[noOfcomposition][dimension];

        /* Composing all the terms of the query together */
        System.out.printf("Composing: ");
        for(String word : words) {
            WordVec wv = wordVecs.wordvecmap.get(word);
            if(wv != null) {
                System.out.printf("<%s> ", word);
                for(int i=0; i<dimension; i++) {
                    composedVector[0][i] += wv.vec[i]/wv.norm;
                }
            }
        }
        System.out.println("");
        /* all words composed normalized-vector always saved in 0th index */
        countComposed ++;

        if(pairWise && words.length>2) {
        /* if pairWise and no. of words is > 2:
            then only pairwise composition is possible
        */
            for (int i = 0; i < words.length-1; i++) {
                WordVec wv1 = wordVecs.wordvecmap.get(words[i]);
                WordVec wv2 = wordVecs.wordvecmap.get(words[i+1]);
                if(wv1 != null && wv2 != null){
                    for(int j=0; j<dimension; j++) {
                        composedVector[countComposed][j] += (wv1.vec[j]/wv1.norm + wv2.vec[j]/wv2.norm);
                    }
                    System.out.printf("Composing: <%s> <%s>\n", words[i], words[i+1]);
                }
                countComposed ++;
            }
        }
//        System.out.println("Composed forms: " + countComposed);
        /* countComposed = number of composed vectors formed */

        /* Now, for each of the composed vectors, 
            say, N NN words with distance will be computed.
           Then from those distances, take the top, say n words with max similarity.
        */
        simScore = new double[countComposed*n];
        simWords = new String[countComposed*n];
        composedVectorSim = new WordVec[countComposed*n];
        for (int i = 0; i < countComposed*n; i++) {
            composedVectorSim[i] = new WordVec();
        }

        for (int processed = 0; processed < countComposed; processed++) {

            double length = 0;
            for (int i=0; i<dimension; i++) {
                length += (composedVector[processed][i]*composedVector[processed][i]);
            }
            length = Math.sqrt(length);
            /* length normalization factor initialized for a composed vector */

            for (int i=0; i<dimension; i++)
                composedVector[processed][i] /= length;
            /* normalization of the composed vector */

            double dist;

            for(String key : wordVecs.wordvecmap.keySet()) {

                boolean sameWord = false;
                for(String s : words) {
                    if(key.equals(s)) {
                        sameWord = true;
                        break;
                    }
                }
                if(!sameWord) {

                    double v[] = new double[dimension];
                    WordVec wv = wordVecs.wordvecmap.get(key);
                    for (int i = 0; i < dimension; i++) {
                        v[i] = wv.vec[i]/wv.norm;
                    }

                    dist = calculateDistance(composedVector[processed], v);
                    for (int a = 0; a < n; a++) {
//                        if (dist > simScore[a+processed*n]) {
                        if (dist > composedVectorSim[a+processed*n].querySim) {
                            for (int d = n - 1; d > a; d--) {
                                composedVectorSim[d+processed*n].querySim = composedVectorSim[d-1+processed*n].querySim;
                                composedVectorSim[d+processed*n].word = composedVectorSim[d-1+processed*n].word;
//                                simScore[d+processed*n] = simScore[d - 1+processed*n];
//                                simWords[d+processed*n] = simWords[d - 1+processed*n];
                            }
                            composedVectorSim[a+processed*n].querySim = dist;
                            composedVectorSim[a+processed*n].word = key;
//                            simScore[a+processed*n] = dist;
//                            simWords[a+processed*n] = key;
                            break;
                        }
                    }
                }
            }
        }

        Arrays.sort(composedVectorSim);
        /*
        // Prining the sorted words with distance to the composed vectors
        for (int i = 0; i < countComposed*n; i++) {
            System.out.println(composedVectorSim[i].word+" "+composedVectorSim[i].querySim);
        }
        */

        /* Duplicate removal:
            Take the unique term along with the highest similarity value for duplicate terms
        */
        LinkedHashMap<String, Double> mostSimilarWords = new LinkedHashMap();
        for (int i = 0; i < countComposed*n; i++) {
            if(null == mostSimilarWords.get(composedVectorSim[i].word))
            /* if the word is not already in the HashMap: then add the word */
                mostSimilarWords.put(composedVectorSim[i].word, composedVectorSim[i].querySim);
            else
            /* if the word is already present in the HashMap: ignore */
                continue;
        }

        /*
        // Printing the words after removing the duplicates
        Iterator<String> keySetIterator = mostSimilarWords.keySet().iterator();
        System.out.println("After removing duplicates:");
        while(keySetIterator.hasNext()){
            String key = keySetIterator.next();
            System.out.println(key + " " + mostSimilarWords.get(key));
        }
        */

        return mostSimilarWords;
    }

    /* returns the word which is similar to the v[] with respect to the vector representation */
    String returnNearestWord(double vec[]) {
        double mostSimScore = -1;
        double dist;
        String mostSimWord="";
        double[] mostSimVec = null;

//        System.out.println(wordVecs.wordvecmap.size());
        for(String key : wordVecs.wordvecmap.keySet()) {

            /* CosineSimilarity not working */
//            sim = calculateCosSim(vec, wordVecs.wordvecmap.get(key).vec);
            double len=0;
            int l = wordVecs.wordvecmap.get(key).vec.length;
            double v[] = new double[l];
            for (int i = 0; i < l; i++) {
                v[i] = wordVecs.wordvecmap.get(key).vec[i];
                len += (v[i]*v[i]);
            }
            len = (double) Math.sqrt(len);
            for (int i = 0; i < l; i++) {
                v[i] /= len;
            }
            dist = calculateDistance(vec, v);
//            System.out.println(wordVecs.wordvecmap.get(key).word + " " + sim);
            if(dist > mostSimScore) {
                mostSimScore = dist;
                mostSimWord = wordVecs.wordvecmap.get(key).word;
                mostSimVec = wordVecs.wordvecmap.get(key).vec;
            }
        }
        System.out.println(mostSimWord + " " + mostSimScore);
        System.out.println(vec.length);
        for (int i = 0; i < vec.length; i++) {
//            System.out.print(mostSimVec[i] + ":" + vec[i] + " ");
            System.out.print(mostSimVec[i] + " ");
        }
        System.out.println();
        return mostSimWord;
    }

    /* Add two vectors and returns the resultant vectr */
    double[] addVector(double v1[], double v2[]) {
        int l = v1.length;
        double vec[] = new double[l];
        double len = 0;

        for (int i = 0; i < l; i++) {
            vec[i] = (v1[i]+v2[i]);
            len += vec[i] * vec[i];
        }
        len = Math.sqrt(len);
        for (int i = 0; i < l; i++) 
            vec[i] /= len;

        return vec;
    }

    private Query getExpandedBOWQuery(Analyzer analyzer, TRECQuery query) throws Exception {
        BooleanQuery booleanQuery;
        boolean pairWise;
        
        pairWise = true;

//        booleanQuery = (BooleanQuery) query.getBOWQuery(analyzer);
        booleanQuery = new BooleanQuery();
        String[] titleTerms = queryFieldAnalyze(analyzer, query.qTitle).split("\\s+");

        System.out.println(Arrays.toString(titleTerms));
        LinkedHashMap<String, Double> mostSimilarWords;

        /* return the mostSimilarWords to the composed forms of titleTerms 
         * 10 - for each composed form, take 10 most similar words to that composed form
         * pairwise - true: consider vectors in moving-window-pair; false- consider only all word composed vector 
        */
        mostSimilarWords = returnSimilarWords(titleTerms, 10, pairWise);
        // mostSimilarWords now have 10*noOfComposed similar words with there distance

        // Printing the words after removing the duplicates
        if(false) // for debuging
        {
            Iterator<String> keySetIterator = mostSimilarWords.keySet().iterator();
            while(keySetIterator.hasNext()){
                String key = keySetIterator.next();
                System.out.println(key + " " + mostSimilarWords.get(key));
            }
        }

        // Adding the original query terms
        for (String term : titleTerms) {
            Term t = new Term(Indexer.FIELD_BOW, term);
            Query tq = new TermQuery(t);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
        }

        // Adding N new terms for expansion
        //// Setting the normalization factor 
        int N = 60;
        if(N>0)
        {
            int c=0;
            // Calculating the norm of the new term weight
            double norm = 0.0;
            for (Map.Entry<String, Double> entry : mostSimilarWords.entrySet()) {
                if(c>=N){
                    break;
                }
                Double value = entry.getValue();
                if(value != null) {
                    norm += value;c++;
                }
            }

            for (Map.Entry<String, Double> entry : mostSimilarWords.entrySet()) {
                String term = entry.getKey();
                Double value = entry.getValue();
                if(c>=N){
                    break;
                }
                if(value != null && term != null) {
                    Term t = new Term(Indexer.FIELD_BOW, term);
                    Query tq = new TermQuery(t);
                    tq.setBoost((float) (value/norm));
                    booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
                    c++;
                }
            }
        }

//        System.out.println(booleanQuery);
//        System.exit(1);

        return booleanQuery;
    }

    public void retrieveAll() throws Exception {
        ScoreDoc[] hits;
        TopDocs topDocs;

        for (TRECQuery query : queries) {
            TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
            Query luceneQuery;
            //luceneQuery = query.getBOWQuery(analyzer);
            luceneQuery = getExpandedBOWQuery(analyzer, query);

//            System.out.println(luceneQuery);
            System.out.println("Retrieved results for query: " + query.qId);
            searcher.search(luceneQuery, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");

            StringBuffer resBuffer = new StringBuffer();
//            System.out.println("Retrieved Length: " + hits_length);
            int hits_length = hits.length;
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
        }
        resFileWriter.close();
    }
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            System.out.printf("Usage: java QueryExpansion <init.properties>\n");
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
        }

        QueryExpansion qe = new QueryExpansion(args[0]);
        //qe.wordVecs.printNNs(); /* prints all the NNs of each of the words of the vocabulary */
        qe.retrieveAll();

    }

}
