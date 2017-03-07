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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

//import edu.stanford.nlp.process.DocumentPreprocessor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;


/**
 *
 * @author dwaipayan
 */
public class Indexer extends AnalyzerClass{
//    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    File        []indexFile;          // place where the index will be stored
    IndexWriter []indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    int tweet_starts_from_date;
    int tweet_ends_from_date;
    
    NERtag nertag;
    POStag postag;
    
    static final public String FIELD_ID = "DOCNO";
    static final public String FIELD_tweettime = "tweettime";   // raw, UNANALYZED content with sentence breaking
    static final public String FIELD_time = "time";
    static final public String FIELD_userid = "userid";
    static final public String FIELD_follower = "follower";
    static final public String FIELD_friend = "friend";       
    static final public String FIELD_listed = "listed";   
    static final public String FIELD_status = "status";
    static final public String FIELD_retweet = "retweet";
    static final public String FIELD_favourite = "favourite";   
    static final public String FIELD_lang = "lang";
    static final public String FIELD_retweetedfrom = "retweetedfrom";
    static final public String FIELD_date= "date";
    static final public String FIELD_rawtext= "rawtext";
    static final public String FIELD_POStag= "POStag";
    static final public String FIELD_NERtag= "NERtag";
    static final public String FIELD_TEXT = "TEXT";       // ANALYZED bow content
    
    public Indexer() throws IOException, ClassCastException, ClassNotFoundException {

        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        tweet_starts_from_date=Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date=Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));
//        
//        if(prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true"))
//            postag=new POStag();
//        if(prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
//            nertag=new NERtag();
            
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
        indexFile=new File[tweet_ends_from_date-tweet_starts_from_date+1];
        Directory []indexDir = new Directory[tweet_ends_from_date-tweet_starts_from_date+1];
        indexWriter=new IndexWriter[tweet_ends_from_date - tweet_starts_from_date + 1];
        
        String indexBasePath=prop.getProperty("indexPath");
         System.err.println(indexBasePath);
        if(indexBasePath.endsWith("/"));
        else
          indexBasePath=  indexBasePath+"/";
        System.err.println(indexBasePath);
        for(int i=tweet_starts_from_date;i<=tweet_ends_from_date;i++)
                {
                int j=i-tweet_starts_from_date;
                
                indexFile[j] = new File(indexBasePath.concat(Integer.toString(i)));
                indexDir[j] = FSDirectory.open(indexFile[j]);
                
                    /* index path set */
                    if (DirectoryReader.indexExists(indexDir[j])) {
                        System.err.println("Index exists in " + indexFile[j].getAbsolutePath());
                        boolIndexExists = true;
                    } else {
                        System.out.println("Will create the index in: " + indexFile[j].getName());
                        boolIndexExists = false;
                        /* Create a new index in the directory, removing any previous indexed documents */
                        IndexWriterConfig[] iwcfg =new IndexWriterConfig[tweet_ends_from_date-tweet_starts_from_date+1];
                        
                        iwcfg[j] = new IndexWriterConfig(Version.LATEST, analyzer);
                        iwcfg[j].setOpenMode(IndexWriterConfig.OpenMode.CREATE);

                            /*  */
                        indexWriter[j] = new IndexWriter(indexDir[j], iwcfg[j]);
                        
                    }
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


    
    public Document constructDoc(String id, String text, String tweettime, String time, 
                String userid, String follower, String friend, String listed, String status, 
                String retweet, String favourite, String lang, String retweetedfrom, String date ) throws IOException {
    /*
        id: Unique document identifier
        content: Total content of the document
    */

        Document doc = new Document();

        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        /* unique doc-id is added */

        String txt = refineTexts(text);

        // Storing the content in the index with field-name FIELD_BOW;
        // with termvector
        
        doc.add(new Field(FIELD_tweettime, tweettime,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_time, time,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_userid, userid,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_follower, follower,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_friend, friend,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_listed, listed,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_status, status,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_retweet, retweet,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_favourite, favourite,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_lang, lang,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_retweetedfrom, retweetedfrom,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_date, date,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_rawtext, text,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        
        
//        if(prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true"))
//            doc.add(new Field(FIELD_POStag, postag.POStagString(text),
//                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
//        
//        if(prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
//            doc.add(new Field(FIELD_NERtag, nertag.NERtagString(text),
//                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        
        /// Index <TEXT> portion with or without positional information
        if(prop.getProperty("store.positional.information","false").toLowerCase().contentEquals("false"))
            doc.add(new Field(FIELD_TEXT, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        
        if(prop.getProperty("store.positional.information","false").toLowerCase().contentEquals("true"))
            doc.add(new Field(FIELD_TEXT, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        
        return doc;
    }
    
    

    void indexFile(File collFile) throws Exception {

        Document doc;
        
        String docType = prop.getProperty("docType");
      
        if (docType.equalsIgnoreCase("trec")) {
            try {
            TrecDocIterator docElts = new TrecDocIterator(collFile);
            
       

            String lang;
            if(prop.getProperty("index.only.english").replaceAll("\\s+", "").toLowerCase().equals("true"))
               lang="en";
            else 
                lang="all";
            String rt=prop.getProperty("index.retweet.also").toLowerCase().replaceAll("\\s+", "");
            
            int dateoftweet= Integer.parseInt(collFile.getName().replaceAll("statuses.log.", "").replaceAll("_trec_format.txt", "").split("-")[2]);
            
            Document docElt;
            
            while (docElts.hasNext()) {
                docElt=docElts.next();
                
                String DOCNOElt;
                if(docElt == null) {
                    System.out.println("docElt null");
                    break;
                }
                
                    DOCNOElt = docElt.getField("DOCNO").stringValue();

                String tweettimeElt = docElt.getField("tweettime").stringValue();
                String timeElt = docElt.getField("time").stringValue();
                String useridElt = docElt.getField("userid").stringValue();
                String followerElt = docElt.getField("follower").stringValue();
                String friendElt = docElt.getField("friend").stringValue();
                String listedElt = docElt.getField("listed").stringValue();
                String statusElt = docElt.getField("status").stringValue();           
                String retweetElt = docElt.getField("retweet").stringValue();
                String favouriteElt = docElt.getField("favourite").stringValue();
                String langElt = docElt.getField("lang").stringValue();           
                String retweetedfromElt = docElt.getField("retweetedfrom").stringValue();
//                String s = "201507"+docElt.select("time").first().text().split(" ")[2];
//                String dateOfTweetElt= s;
                               
                String dateOfTweetElt= timeElt.split(" ")[2];
                
                String TEXTElt = docElt.getField("TEXT").stringValue();
              //  System.err.println(DOCNOElt);
                /*
                Calculting date of tweet time
                */
                int rangedateoftweet= Integer.parseInt(timeElt.split(" ")[2]);
                
                /*
                Index only if Tweets falls into
                Monday, July 20, 2015, 00:00:00 UTC
                and
                Wednesday, July 29, 2015, 23:59:59 UTC
                */
              // System.err.println("dateoftweet"+dateoftweet+" rangedateoftweet"+rangedateoftweet);
               
                    int j=rangedateoftweet-tweet_starts_from_date;
                    
                    if ((rangedateoftweet >= 20) && (rangedateoftweet <= 29) && (rangedateoftweet == dateoftweet)) {
                        if (lang.contentEquals("en") && langElt.equalsIgnoreCase("en")) // Index when language is only english
                            {
                                if (rt.toLowerCase().equals("false") && retweetedfromElt.contentEquals("null")) // Index when language is only english and tweet is not a re-tweet
                                {

                                    System.err.println("Indexing: " + DOCNOElt + " lang=en, re-tweet=false");
                                    for(int k=0;k<=j;k++)
                                    {
                                        doc = constructDoc(DOCNOElt, removeUrl(TEXTElt), tweettimeElt,
                                            timeElt, useridElt, followerElt, friendElt,
                                            listedElt, statusElt, retweetElt, favouriteElt,
                                            langElt, retweetedfromElt, dateOfTweetElt);

                                        indexWriter[k].addDocument(doc);
                                    }
                                    docIndexedCounter++;

                                }
                                if (rt.toLowerCase().equals("true")) // Index when language is only english and tweet is a re-tweet
                                {
                                    System.err.println("Indexing: " + DOCNOElt + " lang=en, re-tweet=true");
                                    for(int k=0;k<=j;k++)
                                    {
                                    doc = constructDoc(DOCNOElt, removeUrl(TEXTElt), tweettimeElt,
                                            timeElt, useridElt, followerElt, friendElt,
                                            listedElt, statusElt, retweetElt, favouriteElt,
                                            langElt, retweetedfromElt, dateOfTweetElt);

                                    indexWriter[k].addDocument(doc);
                                    }
                                    docIndexedCounter++;
                                }
                            }
                            if (lang.equalsIgnoreCase("all"))// Index all languages
                            {
                                if (rt.toLowerCase().equals("false") && retweetedfromElt.contentEquals("null")) // Index all languages and tweet is not a re-tweet
                                {

                                    System.err.println("Indexing: " + DOCNOElt + " lang=all, re-tweet=true");
                                    for(int k=0;k<=j;k++)
                                    {
                                    doc = constructDoc(DOCNOElt, removeUrl(TEXTElt), tweettimeElt,
                                            timeElt, useridElt, followerElt, friendElt,
                                            listedElt, statusElt, retweetElt, favouriteElt,
                                            langElt, retweetedfromElt, dateOfTweetElt);

                                    indexWriter[k].addDocument(doc);
                                    }
                                    docIndexedCounter++;

                                }
                                if (rt.toLowerCase().equals("true"))// Index all languages and tweet is a re-tweet
                                {
                                    System.err.println("Indexing: " + DOCNOElt + " lang=all, re-tweet=false");
                                    for(int k=0;k<=j;k++)
                                    {
                                    doc = constructDoc(DOCNOElt, removeUrl(TEXTElt), tweettimeElt,
                                            timeElt, useridElt, followerElt, friendElt,
                                            listedElt, statusElt, retweetElt, favouriteElt,
                                            langElt, retweetedfromElt, dateOfTweetElt);

                                    indexWriter[k].addDocument(doc);
                                    }
                                    docIndexedCounter++;
                                }

                            }
                        }
            }
        }
        catch (FileNotFoundException ex) {
            System.err.println("Error: '"+collFile.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        }catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+collFile.getAbsolutePath()+"'");
            ex.printStackTrace();
        }
        }
    }
    /**
     *
     * @throws Exception sds fdf
     */
    public void createIndex() throws Exception{
         for(int i=tweet_starts_from_date;i<=tweet_ends_from_date;i++)
                {
                int j=i-tweet_starts_from_date;
                if (indexWriter[j] == null ) {
                    System.err.println("Index already exists at " + indexFile[j].getName() + ". Skipping...");
                    return;
                    }
                }
        System.out.println("Indexing started");
        //System.out.println(prop.containsKey("collSpec"));

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
        for(int i=tweet_starts_from_date;i<=tweet_ends_from_date;i++)
                {
                    int j=i-tweet_starts_from_date;
                    indexWriter[j].close();
                }
        System.out.println("Indexing ends");
        System.out.println(docIndexedCounter + " files indexed");
    }

//    public void dumpIndex() {
//        System.out.println("Dumping the index in: "+ dumpPath);
//        File f = new File(dumpPath);
//        if (f.exists()) {
//            System.out.println("Dump existed.");
//            System.out.println("Last modified: "+f.lastModified());
//            System.out.println("Overwrite(Y/N)?");
//            Scanner reader = new Scanner(System.in);
//            char c = reader.next().charAt(0);
//            if(c == 'N' || c == 'n')
//                return;
//            else
//                System.out.println("Dumping...");
//        }
//        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
//            FileWriter dumpFW = new FileWriter(dumpPath);
//            int maxDoc = reader.maxDoc();
//            for (int i = 0; i < maxDoc; i++) {
//                Document d = reader.document(i);
//                //System.out.print(d.get(FIELD_BOW) + " ");
//                dumpFW.write(d.get(FIELD_TEXT) + " ");
//            }
//            System.out.println("Index dumped in: " + dumpPath);
//            dumpFW.close();
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//        }
//    }


    public static void main(String args[]) throws IOException, Exception {

        CalculateTotalRunTime crt=new CalculateTotalRunTime();
        Indexer indexer = new Indexer();

        if(indexer.boolIndexExists==false) {
            indexer.createIndex();
            indexer.boolIndexExists = true;
        }
        
        Indexer2ndLevel i2l;
        if(indexer.prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true") ||
          indexer.prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
        {
            i2l = new Indexer2ndLevel();
            i2l.createIndex2ndlevel();
        }
        
//        if(indexer.boolIndexExists == true && indexer.boolDumpIndex == true) {
//            indexer.dumpIndex();
//        }
        crt.PrintRunTime();
    }

}
