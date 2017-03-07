/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.lucene.index.DirectoryReader;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;



/**
 *
 * @author dwaipayan
 */
public class CumulativeIndexer extends AnalyzerClass{
//    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    File        []indexFile=new File[10];          // place where the index will be stored

    AbstractSequenceClassifier<CoreLabel> classifier;
    MaxentTagger tagger ;
            
    IndexWriter []indexWriter=new IndexWriter[10];
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
   
    static final public String FIELD_ID = "DOCNO";
    static final public String FIELD_TEXT = "TEXT";       // ANALYZED bow content
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
    static final public String FIELD_RAWTEXT= "RAWTEXT";
    static final public String FIELD_STNFORDNERTEXT="STNFORDNERTEXT";
    static final public String FIELD_STNFORDPOSTEXT="STNFORDPOSTEXT";
    
    public CumulativeIndexer() throws IOException, ClassCastException, ClassNotFoundException {
        
        String fname;
        
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        
        // Stanford ner text config
        if(prop.getProperty("ner.classify").contentEquals("true"))
            classifier = CRFClassifier.getClassifier(prop.getProperty("stanford.ner.classifier"));
        
        
        if(prop.getProperty("pos.tag").contentEquals("true"))
            tagger=new MaxentTagger(prop.getProperty("stanford.pos.model"));
        
        
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
        fname=prop.getProperty("indexPath");
        if(!fname.endsWith("/"))
            fname=fname.concat("/");
        for(int i=0;i<10;i++)
        {
            indexFile[i] = new File(fname.concat(Integer.toString(i+20)+"/"));
            if(!indexFile[i].exists())
                indexFile[i].mkdirs();
            System.out.println(indexFile[i].getAbsoluteFile());
            Directory []indexDir = new Directory[10];
                    indexDir[i]=FSDirectory.open(indexFile[i]);
            /* index path set */

            if (DirectoryReader.indexExists(indexDir[i])) {
                System.err.println("Index exists in "+indexFile[i].getAbsolutePath());
                boolIndexExists = true;
            }
            else {
                System.out.println("Will create the index in: " + indexFile[i].getName());
                boolIndexExists = false;
                /* Create a new index in the directory, removing any previous indexed documents */
                IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, analyzer);
                iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                /*  */
                indexWriter[i] = new IndexWriter(indexDir[i], iwcfg);

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
        // removing control characters
        text=remove_non_ascii_char(remove_control_char(text));
        
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
        /// Adding Raw text
        doc.add(new Field(FIELD_RAWTEXT, text,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        /// Adding stanford NER-ed text
        if(prop.getProperty("ner.classify").contentEquals("true"))
        doc.add(new Field(FIELD_STNFORDNERTEXT, NER_classify(text),
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        /// Adding stanford POS tagged-ed text
        if(prop.getProperty("pos.tag").contentEquals("true"))
        doc.add(new Field(FIELD_STNFORDPOSTEXT, POS_tag(text),
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
           
            
        /// Index <TEXT> portion with or without positional information
        if(prop.getProperty("store.positional.information").toLowerCase().contentEquals("false"))
            doc.add(new Field(FIELD_TEXT, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        
        if(prop.getProperty("store.positional.information").toLowerCase().contentEquals("true"))
            doc.add(new Field(FIELD_TEXT, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        
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

            String lang;
            if(prop.getProperty("index.only.english").replaceAll("\\s+", "").toLowerCase().equals("true"))
               lang="en";
            else 
                lang="all";
            String rt=prop.getProperty("index.retweet.also").toLowerCase().replaceAll("\\s+", "");
            
            int dateoftweet= Integer.parseInt(collDir.getName().replaceAll("statuses.log.", "").replaceAll("_trec_format.txt", "").split("-")[2]);
            
            
            for (Element docElt : docElts) {
                Element DOCNOElt = docElt.select("DOCNO").first();
                Element tweettimeElt = docElt.select("tweettime").first();
                Element timeElt = docElt.select("time").first();
                Element useridElt = docElt.select("userid").first();
                Element followerElt = docElt.select("follower").first();
                Element friendElt = docElt.select("friend").first();
                Element listedElt = docElt.select("listed").first();
                Element statusElt = docElt.select("status").first();           
                Element retweetElt = docElt.select("retweet").first();
                Element favouriteElt = docElt.select("favourite").first();
                Element langElt = docElt.select("lang").first();           
                Element retweetedfromElt = docElt.select("retweetedfrom").first();
//                String s = "201507"+docElt.select("time").first().text().split(" ")[2];
//                String dateOfTweetElt= s;
                String dateOfTweetElt= docElt.select("time").first().text().split(" ")[2];
                
                Element TEXTElt = docElt.select("TEXT").first();
                
                /*
                Calculting date of tweet time
                */
                int rangedateoftweet= Integer.parseInt(timeElt.text().split(" ")[2]);
              //  int indexforDate=Integer.parseInt(prop.getProperty("date_of_index"));
                /*
                Index only if Tweets falls into
                Monday, July 20, 2015, 00:00:00 UTC
                and
                Wednesday, July 29, 2015, 23:59:59 UTC
                */
               // System.err.println("dateoftweet"+dateoftweet+" rangedateoftweet"+rangedateoftweet);
                if((rangedateoftweet >= 20) && (rangedateoftweet <= 29) && (rangedateoftweet == dateoftweet)) 
                {
                if(lang.contentEquals("en") && langElt.text().equalsIgnoreCase("en")) // Index when language is only english
                {    
                    if(rt.toLowerCase().equals("false") && retweetedfromElt.text().contentEquals("null")) // Index when language is only english and tweet is not a re-tweet
                    {

                        System.err.println("Indexing: "+DOCNOElt.text()+ " lang=en, re-tweet=false");
                        
                        doc = constructDoc(DOCNOElt.text(), removeUrl(TEXTElt.text()), tweettimeElt.text(), 
                                timeElt.text(), useridElt.text(), followerElt.text(), friendElt.text(), 
                                listedElt.text(), statusElt.text(), retweetElt.text(), favouriteElt.text(), 
                                langElt.text(), retweetedfromElt.text(), dateOfTweetElt);

                        for(int i=dateoftweet-20; i<10; i++){
                          
                            indexWriter[i].addDocument(doc);
                        }
                        docIndexedCounter++;
                        
                    }
                    if(rt.toLowerCase().equals("true")) // Index when language is only english and tweet is a re-tweet
                        {
                        System.err.println("Indexing: "+DOCNOElt.text()+ " lang=en, re-tweet=true");
                        doc = constructDoc(DOCNOElt.text(), removeUrl(TEXTElt.text()), tweettimeElt.text(), 
                            timeElt.text(), useridElt.text(), followerElt.text(), friendElt.text(), 
                            listedElt.text(), statusElt.text(), retweetElt.text(), favouriteElt.text(), 
                            langElt.text(), retweetedfromElt.text(), dateOfTweetElt);
                        
                        for(int i=dateoftweet-20; i<10; i++){
                            

                            indexWriter[i].addDocument(doc);
                        }
                        docIndexedCounter++;
                        }   
                }
                if(lang.equalsIgnoreCase("all"))// Index all languages
                {
                    if(rt.toLowerCase().equals("false") && retweetedfromElt.text().contentEquals("null")) // Index all languages and tweet is not a re-tweet
                    {

                        System.err.println("Indexing: "+DOCNOElt.text()+ " lang=all, re-tweet=true");

                        doc = constructDoc(DOCNOElt.text(), removeUrl(TEXTElt.text()), tweettimeElt.text(), 
                                timeElt.text(), useridElt.text(), followerElt.text(), friendElt.text(), 
                                listedElt.text(), statusElt.text(), retweetElt.text(), favouriteElt.text(), 
                                langElt.text(), retweetedfromElt.text(), dateOfTweetElt);
                        for(int i=dateoftweet-20; i<10; i++){
                            

                            indexWriter[i].addDocument(doc);
                            
                        }
                        docIndexedCounter++;
                        
                    }
                    if(rt.toLowerCase().equals("true"))// Index all languages and tweet is a re-tweet
                    {
                    System.err.println("Indexing: "+DOCNOElt.text()+ " lang=all, re-tweet=false");
                    
                    doc = constructDoc(DOCNOElt.text(), removeUrl(TEXTElt.text()), tweettimeElt.text(), 
                            timeElt.text(), useridElt.text(), followerElt.text(), friendElt.text(), 
                            listedElt.text(), statusElt.text(), retweetElt.text(), favouriteElt.text(), 
                            langElt.text(), retweetedfromElt.text(), dateOfTweetElt);
                    for(int i=dateoftweet-20; i<10; i++){
                        

                        indexWriter[i].addDocument(doc);
                    }
                    docIndexedCounter++;
                    }
                }
                }
            }
        }
    }

    public void createIndex() throws Exception{
        for(int i=0;i<10;i++)
        {
        if (indexWriter[i] == null ) {
            System.err.println("Index already exists at " + indexFile[i].getName() + ". Skipping...");
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
         for(int i=0;i<10;i++)
        {
        indexWriter[i].close();
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
//        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile[indexPtr]))) {
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
       
       CalculateTotalRunTime crt= new CalculateTotalRunTime();
       
        CumulativeIndexer Cu_indexer = new CumulativeIndexer();
        
        if(Cu_indexer.boolIndexExists==false) {
            Cu_indexer.createIndex();
            Cu_indexer.boolIndexExists = true;
        }
//        if(indexer.boolIndexExists == true && indexer.boolDumpIndex == true) {
//            indexer.dumpIndex();
//        }
        crt.PrintRunTime();
        
    }

    public String NER_classify(String text) {
        return classifier.classifyToString(text);
    }
    
    public String POS_tag(String text)
    {
       return tagger.tagString(text).trim();
    }  

}
