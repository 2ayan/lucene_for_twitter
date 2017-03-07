/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author ayan
 */
public class Indexer2ndLevel extends AnalyzerClass {

    boolean boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean boolIndexFromSpec;  // true; false if indexing from collPath
    int docIndexedCounter;  // document indexed counter
    File[] indexFile4reader;          // place where the index will be stored
    Directory[] indexDir4reader;
    IndexReader[] indexReader;
    IndexWriter[] indexWriter;
    File[] indexFile4writer;          // place where the index will be stored

    boolean boolDumpIndex;      // true if want ot dump the entire collection
    String dumpPath;

    int tweet_starts_from_date;
    int tweet_ends_from_date;
    int bulkTagNo;

    NERtag nertag;
    POStag postag;

    Field fieldPOS, fieldNER;

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
    static final public String FIELD_date = "date";
    static final public String FIELD_rawtext = "rawtext";
    static final public String FIELD_POStag = "POStag";
    static final public String FIELD_NERtag = "NERtag";
    static final public String FIELD_TEXT = "TEXT";       // ANALYZED bow content

    public Indexer2ndLevel() throws IOException, ClassCastException, ClassNotFoundException {

        GetProjetBaseDirAndSetProjectPropFile setPropFile = new GetProjetBaseDirAndSetProjectPropFile();
        prop = setPropFile.prop;
        tweet_starts_from_date = Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date = Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));

        // if(prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true"))
        postag = new POStag();
        //if(prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
        nertag = new NERtag();
        setAnalyzer();
        bulkTagNo = 5000;

        /* index path setting */
        indexFile4reader = new File[tweet_ends_from_date - tweet_starts_from_date + 1];
        indexDir4reader = new Directory[tweet_ends_from_date - tweet_starts_from_date + 1];
        indexReader = new IndexReader[tweet_ends_from_date - tweet_starts_from_date + 1];

        indexFile4writer = new File[tweet_ends_from_date - tweet_starts_from_date + 1];
        Directory[] indexDir4writer = new Directory[tweet_ends_from_date - tweet_starts_from_date + 1];
        indexWriter = new IndexWriter[tweet_ends_from_date - tweet_starts_from_date + 1];

        String indexBasePath = prop.getProperty("indexPath");

        if (indexBasePath.endsWith("/")); else {
            indexBasePath = indexBasePath + "/";
        }
        System.err.println(indexBasePath);
        for (int i = tweet_starts_from_date; i <= tweet_ends_from_date; i++) {
            int j = i - tweet_starts_from_date;

            indexFile4reader[j] = new File(indexBasePath.concat(Integer.toString(i)));
            indexDir4reader[j] = FSDirectory.open(indexFile4reader[j]);
            indexReader[j] = DirectoryReader.open(indexDir4reader[j]);

            String tmp = new File(indexBasePath).getParentFile().toPath().toString().concat("/2ndLevel/" + Integer.toString(i));
            System.err.println("tmp=" + tmp);
            indexFile4writer[j] = new File(tmp);
            indexDir4writer[j] = FSDirectory.open(indexFile4writer[j]);
                //indexWriter[j]=DirectoryReader.open(indexDir4writer[j]);

            /* index path set */
            if (DirectoryReader.indexExists(indexDir4reader[j]) ) {
                System.err.println("Index exists in " + indexFile4reader[j].getAbsolutePath());
                //boolIndexExists = true;

                System.out.println("Will create the index in: " + indexFile4writer[j].getName());
                //boolIndexExists = false;
                boolIndexExists = true;
                /* Create a new index in the directory, removing any previous indexed documents */
                IndexWriterConfig[] iwcfg = new IndexWriterConfig[tweet_ends_from_date - tweet_starts_from_date + 1];

                iwcfg[j] = new IndexWriterConfig(Version.LATEST, analyzer);
                iwcfg[j].setOpenMode(IndexWriterConfig.OpenMode.CREATE);

                /*  */
                indexWriter[j] = new IndexWriter(indexDir4writer[j], iwcfg[j]);
                System.err.println("Index writer created"+j);
            } else {
                System.err.println("Index Does not exists index in " + indexFile4reader[j].getAbsolutePath());
                boolIndexExists = false;
                return;
            }
        }
        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex", "false"));
        if (boolIndexExists == true && boolDumpIndex == true) {
            dumpPath = prop.getProperty("dumpPath");
        }
    }

    public void createIndex2ndlevel() throws Exception {

        for (int i = tweet_starts_from_date; i <= tweet_ends_from_date; i++) {
            int j = i - tweet_starts_from_date;
            if (indexWriter[j] == null) {
                System.err.println("Index already exists at " + indexFile4writer[j].getName() + ". Skipping...");
                return;
            }
        }
        System.out.println("Indexing started");
        //System.out.println(prop.containsKey("collSpec"));
       // for (int i = tweet_starts_from_date; i <= tweet_ends_from_date; i++) {
            //indexWriter[i - tweet_starts_from_date].close();
//            IndexIndex(i - tweet_starts_from_date);
  //      }

        IndexIndex(tweet_ends_from_date - tweet_starts_from_date);

        for (int i = tweet_starts_from_date; i <= tweet_ends_from_date; i++) {
            int j = i - tweet_starts_from_date;
            indexWriter[j].close();
        }
        System.out.println("Indexing ends");
        System.out.println(docIndexedCounter + " files indexed");
    }

    public void IndexIndex(int j) throws IOException {
        Document d1;
        int docnoinIndex = (int) indexReader[j].numDocs();
        System.err.println(docnoinIndex);

        String[] toTag = new String[bulkTagNo];
        String[] nerTag = new String[bulkTagNo];
        String[] posTag = new String[bulkTagNo];
        int count = 0;

        for (int c = 0; c < docnoinIndex; c++) {
            if ((c % bulkTagNo == 0 || (c + 1) == docnoinIndex) && c > 0) {
                count = 0;
                System.out.println("Starting to NER Tag..");
                nerTag = nertag.NERtagStringArray(toTag);
                System.out.println("NER Tag ended.");
                System.out.println("Starting to POS Tag..");
                posTag = postag.POStagStringArray(toTag);
                System.out.println("POS Tag ended.");

                int tostart = c - bulkTagNo;
                for (int dc = tostart; dc < c; dc++) {
                    Document d = indexReader[j].document(dc);
                    d1 = this.constructDoc(d.get("DOCNO"), d.get("TEXT"), d.get("tweettime"), d.get("time"),
                            d.get("userid"), d.get("follower"), d.get("friend"), d.get("listed"), d.get("status"),
                            d.get("retweet"), d.get("favourite"), d.get("lang"), d.get("retweetedfrom"),
                            d.get("date"), d.get("rawtext"), nerTag[dc - tostart], posTag[dc - tostart]);
                   //System.err.println(dc+"\n"+nerTag[dc - tostart]+"\n"+posTag[dc - tostart]);
                    
                    int dateoftweet=Integer.parseInt(d.get("time").split(" ")[2]);
                    for(int indexcount=dateoftweet; indexcount<=tweet_ends_from_date;indexcount++)
                      
                            indexWriter[indexcount-tweet_starts_from_date].addDocument(d1);
                    //System.err.println(d1.get("DOCNO"));
                }
                System.out.println(c+" documents indexed in "+j+"th index.");
            } else {
                //if(c%bulkTagNo==0)
                toTag[count++] = indexReader[j].document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", "");
//                            else
//                                toTag=toTag.concat("\n"+indexReader[j].document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", ""));

            }
        }
    }

    public Document constructDoc(String id, String text, String tweettime, String time,
            String userid, String follower, String friend, String listed, String status,
            String retweet, String favourite, String lang, String retweetedfrom,
            String date, String rawtext, String ner, String pos) throws IOException {
        /*
         id: Unique document identifier
         content: Total content of the document
         */

        Document doc = new Document();

        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        /* unique doc-id is added */

        //String txt = refineTexts(rawtext);
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

        // if(prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true"))
        doc.add(new Field(FIELD_POStag, pos,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));

        // if(prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
        doc.add(new Field(FIELD_NERtag, ner,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));

        /// Index <TEXT> portion with or without positional information
        //if (prop.getProperty("store.positional.information", "false").toLowerCase().contentEquals("false")) {
            doc.add(new Field(FIELD_TEXT, text,
                    Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
//        }
//
//        if (prop.getProperty("store.positional.information", "false").toLowerCase().contentEquals("true")) {
//            doc.add(new Field(FIELD_TEXT, text,
//                    Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
//        }

        return doc;
    }

    public static void main(String[] args) throws IOException, ClassCastException, ClassNotFoundException, Exception {
        CalculateTotalRunTime crt=new CalculateTotalRunTime();
        Indexer2ndLevel i2l = new Indexer2ndLevel();

        if (i2l.boolIndexExists == true) {
            i2l.createIndex2ndlevel();
            i2l.boolIndexExists = false;
        }
        crt.PrintRunTime();
    }

}
