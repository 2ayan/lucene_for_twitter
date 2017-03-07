/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.stanford.nlp.process.DocumentPreprocessor;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Scanner;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;


/**
 *
 * @author dwaipayan
 */
public class Indexer {
    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    File        indexFile;          // place where the index will be stored
    Analyzer    analyzer;           // the paper analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    /* for 2nd Pass indexing */
    File        w2vIndexFile;       // place where the word2vec expanded index will be stored

    static final public String FIELD_ID = "docid";
    static final public String FIELD_BOW = "content";       // ANALYZED bow content
    static final public String FIELD_RAW = "raw-content";   // raw, UNANALYZED content with sentence breaking
    static final public String FIELD_TITLE = "title";

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

    private Indexer(String propFile) throws IOException {

        prop = new Properties();
        prop.load(new FileReader(propFile));
        setAnalyzer();
        /* property files are loaded */

        /* collection path setting */
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else {
            System.err.println("Neither collPath not collSpec is present");
            System.exit(1);
        }
        /* collection path set */

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Will create the index in: " + indexFile.getName());
            boolIndexExists = false;
            /* Create a new index in the directory, removing any previous indexed documents */
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            /*  */
            indexWriter = new IndexWriter(indexDir, iwcfg);

            /*
            System.out.println("Exit(E/e) or, Proceed(P/p)");
            Scanner reader = new Scanner(System.in); char c = reader.next().charAt(0);
            if(c == 'P' || c == 'p') System.out.println("Proceeding");
            else {System.out.println("Terminated.");System.exit(1);}
            */
        }

        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex","false"));
        if(boolIndexExists == true && boolDumpIndex == true){
            dumpPath = prop.getProperty("dumpPath");
        }
    }

    private void indexDirectory(File collDir) throws Exception {
        File[] files = collDir.listFiles();
        //System.out.println("Indexing directory: "+files.length);
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory: " + f.getName());
                indexDirectory(f);  // recurse
            }
            else {
                System.out.println((docIndexedCounter+1)+": Indexing file: " + f.getName());
                indexFile(f);
                docIndexedCounter++;
            }
        }
    }

    public static String refineTexts(String txt) {
    /* removes all special characters from txt, removes numericals etc. */

        // removes the urls
        txt = removeUrl(txt);

        // removes any special characters
        txt = refineSpecialChars(txt);

        // removes any numerical values
        txt = removeNumerical(txt);

        return txt;
    }

    public static String removeNumerical(String s) {
        /* removes all numerical tokens present in s */
        StringBuffer finalStr = new StringBuffer();

        String []tokens;
        tokens = s.trim().split(" ");
        for (String token : tokens) {
            if (!(token == null) && !isNumerical(token)) {
                finalStr.append(token).append(" ");
            }
        }

        return finalStr.toString();
    }

    public static String removeUrl(String str)
    {
        try {
            String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$~_?\\+-=\\\\\\.&]*)";
            Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(str);
            int i = 0;
            while (str!=null && m.find()) {
                str = str.replaceAll(m.group(0)," ").trim();
                i++;
            }
            return str;
        }
        catch(Exception e) {
            
        }
        return str;
    }

    public static boolean isNumerical(String s) {
        boolean isInt;
        boolean isDouble = false;

        try { 
            Integer.parseInt(s); 
            isInt = true;
        } catch(NumberFormatException e) { 
            isInt = false; 
        }
        
        if(!isInt) {
            try {
                Double.parseDouble(s);
                isDouble = true;
            } catch (NumberFormatException e) {
                isDouble = false;
            }
        }

        return isInt || isDouble;
    }

    public static String refineSpecialChars(String txt) {
        if(txt!=null)
            txt = txt.replaceAll("\\p{Punct}+", " ");
        return txt;
    }

    String getSentenceFromString(String text) {
        StringBuffer buff = new StringBuffer();
        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));

        for (List sentence : dp) {
            for (Iterator iter = sentence.iterator(); iter.hasNext();) {
                buff.append(iter.next());
                buff.append(" ");
            }
            buff.append("\n");
        }

        return buff.toString();
    }
    
    Document constructDoc(String id, String content) throws IOException {
    /*
        id: Unique document identifier
        content: Total content of the document
    */

        Document doc = new Document();

        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        /* unique doc-id is added */

        /*
        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        StringBuffer buff = new StringBuffer();
        
        Elements elts = jdoc.getElementsByTag("p");
        for (Element elt : elts) {
            buff.append(elt.text()).append(" ");
        }
        */

        String txt = refineTexts(content);

        // Storing the content in the index with field-name FIELD_BOW;
        // with termvector
        doc.add(new Field(FIELD_BOW, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        /* FIELD_BOW: analyzed, bow content is added */

        // Storing the raw, unanalyzed, sentence separated content with field-name FIELD_RAW
        //txt = getSentenceFromString(content);
//        System.out.println(txt);
        //doc.add(new Field(FIELD_RAW, txt, Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
        /* FIELD_RAW: unanalyzed, raw content is added */

        return doc;
    }

    void indexFile(File collDir) throws Exception {

        Document doc;
        String line;
        FileReader fr = new FileReader(collDir);
        BufferedReader br = new BufferedReader(fr);

        String docType = prop.getProperty("docType");

        if (docType.equalsIgnoreCase("trec")) {
            //System.out.println("Indexing TREC type documents:");
            StringBuffer txtbuff = new StringBuffer();
            while ((line = br.readLine()) != null)
                txtbuff.append(line).append("\n");
            String content = txtbuff.toString();

            org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
            Elements docElts = jdoc.select("DOC");

            for (Element docElt : docElts) {
                Element docIdElt = docElt.select("DOCNO").first();
                System.out.println("Indexing: "+docIdElt.text());
                doc = constructDoc(docIdElt.text(), docElt.text());
                indexWriter.addDocument(doc);
                docIndexedCounter++;
            }
        }
    }

    public void createIndex() throws Exception{

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");
        System.out.println(prop.containsKey("collSpec"));

        if (boolIndexFromSpec) {
            /* if collectiomSpec is present, then index from the spec file*/
            String specPath = prop.getProperty("collSpec");
            System.out.println("Reading from spec file at: "+specPath);
            try (BufferedReader br = new BufferedReader(new FileReader(specPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                   indexFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                indexDirectory(collDir);
            else
                indexFile(collDir);
        }

        indexWriter.close();

        System.out.println("Indexing ends");
        System.out.println(docIndexedCounter + " files indexed");
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
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
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
    public void expandIndex(){
        w2vIndexFile = new File(prop.getProperty("w2vIndexPath"));
    }

    public static void main(String args[]) throws IOException, Exception {
        if(args.length == 0) {
            System.out.printf("Usage: java Indexer <init.properties>\n");
//            System.exit(1);
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
        }

        Indexer indexer = new Indexer(args[0]);

        if(indexer.boolIndexExists==false) {
            indexer.createIndex();
            indexer.boolIndexExists = true;
        }
        if(indexer.boolIndexExists == true && indexer.boolDumpIndex == true) {
            indexer.dumpIndex();
        }
    }
}
