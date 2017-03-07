/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 *
 * @author dwaipayan
 * 
 * Do the 2nd pass indexing of the collection.
 * 
 */

class TermFreq {
    String term;
    float ntf;

    public TermFreq(String term, float ntf) {
        this.term = term;
        this.ntf = ntf;
    }
}

class PayloadAnalyzer extends Analyzer {
    private final PayloadEncoder encoder;
    
    public PayloadAnalyzer() {
        this.encoder = new FloatEncoder();
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName, Reader reader) {
//        Tokenizer source = new WhitespaceTokenizer(Version.LUCENE_4_10_4, reader);
        Tokenizer source = new WhitespaceTokenizer(reader);
        TokenStream filter = new DelimitedPayloadTokenFilter(source, WordVectorIndexer.PAYLOAD_DELIM, encoder);
        return new Analyzer.TokenStreamComponents(source, filter);
    }
}

public class WordVectorIndexer {

    Properties  prop;               // prop of the init.properties file
//    String      collPath;           // path of the collection
//    String      collSpecPath;       // path of the collection spec file
//    File        collDir;            // collection Directory
    File        indexFile;          // collection index
    File        vecIndexFile;       // place where the vector index will be stored
    Analyzer    analyzer;           // the paper analyzer
    IndexWriter indexWriter;
    boolean     boolW2vIndexExists; // boolean flag to indicate whether the index exists or not
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    File        w2vIndexFile;       // place where the word2vec expanded index will be stored
    WordVecs    wordvecs;

    private WordVectorIndexer(String propertyPath) throws IOException, Exception {
        prop = new Properties();
        prop.load(new FileReader(propertyPath));

        indexFile = new File(prop.getProperty("indexPath"));

        wordvecs = new WordVecs(prop);
    }

    static final public String FIELD_ID = "docid";
    static final public String FIELD_BOW = "content";       // ANALYZED BOW content
    static final public String FIELD_BOV = "wordVectors";       // raw, UNANALYZED content with sentence breaking
    static final public String FIELD_TITLE = "title";
    static final char PAYLOAD_DELIM = '|';

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

    private void dumpIndex() {
        System.out.println("Dumping the index in: "+ dumpPath);
        File f = new File(dumpPath);
        if (f.exists()) {
            System.out.println("Dump existed.");
            System.out.println("Last modified: "+f.lastModified());
            System.out.println("Overwrite(Y/N)?");
            Scanner reader = new Scanner(System.in);
            char c = reader.next().charAt(0);
            if(c == 'N' || c == 'n')
                return;
            else
                System.out.println("Dumping...");
        }
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(vecIndexFile))) {
            FileWriter dumpFW = new FileWriter(dumpPath);
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                //System.out.print(d.get(FIELD_BOW) + " ");
                dumpFW.write(d.get(FIELD_BOW) + " ");
            }
            System.out.println("Index dumped in: " + dumpPath);
            dumpFW.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /* 2nd pass indexing */
    /* Read each of documents of the collection and 
        smooth each term generation probability with similar words
        obtained from word2vec */
    public void expandIndex() throws IOException{
        w2vIndexFile = new File(prop.getProperty("w2vIndexPath"));

	analyzer = new PayloadAnalyzer();
        wordvecs.loadPrecomputedNNs();

        // Open the new wv index for writing
        IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        
        //if (!DirectoryReader.indexExists(FSDirectory.open(wvIndexFile)))
            indexWriter = new IndexWriter(FSDirectory.open(w2vIndexFile), iwcfg);
        //else {
            //System.err.println("wvIndex exists!");
            //return;
        //}

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
            int maxDoc = reader.maxDoc();
            Document expDoc;
            for (int i = 0; i < maxDoc; i++) {
                System.out.println("DocId: "+i);
                expDoc = expandDoc(reader, i);
                if (expDoc == null) {
                    System.err.println("Can't expand documents in 2nd pass");
                    indexWriter.close();
                    return;
                }
                indexWriter.addDocument(expDoc);
            }
        }
        indexWriter.close();
    }

    Document expandDoc(IndexReader reader, int docId) throws IOException {
	int N = reader.numDocs();
        ArrayList<TermFreq> tfvec = new ArrayList<>();

        Document newdoc = new Document();
        Document doc = reader.document(docId);

        StringBuffer buff = new StringBuffer();

        newdoc.add(new Field(FIELD_ID, doc.get(FIELD_ID), Field.Store.YES, Field.Index.NOT_ANALYZED));
  
        if (false) { 
            HashMap<String, Integer> tfMap = new HashMap<>();
            String content = doc.get(FIELD_BOW);
//            Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_4_10_4);
            Analyzer analyzer = new WhitespaceAnalyzer();
            TokenStream stream = analyzer.tokenStream(FIELD_BOW, new StringReader(content));
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            while (stream.incrementToken()) {
                String term = termAtt.toString();
                Integer tf = tfMap.get(term);
                if (tf == null)
                    tf = new Integer(0);
                tf++;
                tfMap.put(term, tf);
            }

            int docLen = 0, i = 0;
            for (Map.Entry<String, Integer> e : tfMap.entrySet()) {
                docLen += e.getValue();
            }

            for (Map.Entry<String, Integer> e : tfMap.entrySet()) {
                int tf = e.getValue();
                tfvec.add(new TermFreq(e.getKey(), tf/(float)docLen));
            }

        }
        else if (true) {
            //get terms vectors stored in 1st pass
            Terms terms = reader.getTermVector(docId, FIELD_BOW);
            if (terms == null || terms.size() == 0)
                return null;

            TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
            BytesRef term;
            int docLen = 0;

            // Calculate doc len
            while (termsEnum.next() != null) {// explore the terms for this field
                DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one

                while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                //get the term frequency in the document
                    docLen += docsEnum.freq();
                }
            }
//            System.out.println("DocLength: "+docLen);

            // Construct the normalized tf vector
            termsEnum = terms.iterator(null); // access the terms for this field
            while ((term = termsEnum.next()) != null) { // explore the terms for this field
                DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
                while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                //get the term frequency in the document
                    int tf = docsEnum.freq();
                    float ntf = tf/(float)docLen;
                    tfvec.add(new TermFreq(term.utf8ToString(), ntf));
                }
            }
        }
        
        // Iterate over the normalized tf vector
        int i, j, len = tfvec.size();
        double prob, sim, totalSim = 0.0f;
//        float mu = Float.parseFloat(prop.getProperty("mu", "0.2"));
        //float mu = Float.parseFloat(prop.getProperty("mu", "0.0"));
        //float oneMinusMu = 1-mu;
        
        // P(t|t',d)
            
        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            for (j = i+1; j < len; j++) {
                TermFreq tf_j = tfvec.get(j);
                totalSim += this.wordvecs.getSim(tf_i.term, tf_j.term);
            }
        }

        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            for (j = 0; j < len; j++) {
                if (i == j)
                    continue;
                TermFreq tf_j = tfvec.get(j);
                // CHECK: Currently not checking if t'
                // is a near neighbour of t
                sim = this.wordvecs.getSim(tf_i.term, tf_j.term);
                prob = /*mu * */ tf_j.ntf * sim/totalSim;
                tf_i.ntf += prob;
            }
            buff.append(tf_i.term).append(PAYLOAD_DELIM).append(tf_i.ntf).append(" ");
        }
        
        /* Computed at retrieval time
        // P(t|t',C)
        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            // Get the nearest neighbours of tf_i
            List<WordVec> nn_tf_i = wordvecs.getPrecomputedNNs(tf_i.term);
            if (nn_tf_i == null) {
                System.err.println("Can't get NN(" + tf_i.term + ", " + tf_i.ntf + ")");
                continue;
            }

            float normalizer = 0.0f;
            for (WordVec nn : nn_tf_i) {
                normalizer += nn.querySim;
            }
            
            for (WordVec nn : nn_tf_i) {
                long docFreq = reader.docFreq(
                        new Term(WordVecIndexer.FIELD_BAG_OF_WORDS, nn.word));
                tf_i.ntf += (float)(oneMinusMu * nn.querySim/normalizer * docFreq/(double)N); 
            }
            
        } */
        
        /*
        for (i = 0; i < len; i++) {
            TermFreq tf_i = tfvec.get(i);
            buff.append(tf_i.term).append(PAYLOAD_DELIM).append(tf_i.ntf).append(" ");
        }
         */
        
        newdoc.add(new Field(FIELD_BOV, buff.toString(),
                Field.Store.YES, Field.Index.ANALYZED));
        return newdoc;
    }


    public static void main(String args[]) throws IOException, Exception {
        if(args.length == 0) {
            System.out.printf("Usage: java WordVectorIndexer <init.properties>\n");
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
        }

        WordVectorIndexer indexer = new WordVectorIndexer(args[0]);

        if(indexer.boolW2vIndexExists==false) {
            indexer.expandIndex();
            indexer.boolW2vIndexExists = true;
        }
        else {
            System.out.println("Word2Vec expanded index already exists");
        }
    }
    
}
