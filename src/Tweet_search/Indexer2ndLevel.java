/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    String []index_fields;
    String []index_fields_with_analyze;
    String []INDEX_FIELDS;

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

        String tmp=prop.getProperty("index_fields","null");
        INDEX_FIELDS=new String[tmp.split(",").length+2];
        
        String[] toTag = new String[bulkTagNo];
        String[] nerTag = new String[bulkTagNo];
        String[] posTag = new String[bulkTagNo];
        int count = 0;

        for (int c = 0; c < docnoinIndex; c++) {
            if ((c % bulkTagNo == 0 || (c + 1) == docnoinIndex) && c > 0) {
                
                
                System.out.println("Starting to NER Tag..");
                nerTag = nertag.NERtagStringArray(toTag);
                System.out.println("NER Tag ended.");
                System.out.println("Starting to POS Tag..");
                posTag = postag.POStagStringArray(toTag);
                System.out.println("POS Tag ended.");

                int tostart=0;
                if((c+1)<docnoinIndex)
                    tostart = c - bulkTagNo;
                else 
                    {
                    tostart = docnoinIndex % bulkTagNo;
                    tostart = docnoinIndex - tostart;
                    c++;
                    }
                for (int dc = tostart; dc < c; dc++) {
                    Document d = indexReader[j].document(dc);
                    
                    
                    int i=0;
                    if(!tmp.contentEquals("null"))
                        {
                        index_fields=new String[tmp.split(",").length+2];
                        for(i=0;i<tmp.split(",").length;i++)
                            {
                            index_fields[i]=tmp.split(",")[i];
                            INDEX_FIELDS[i]=d.get(index_fields[i]);
                            }
                        }
                    
                   //System.out.println(toTag[dc-tostart]+"\nc===============\n"+nerTag[dc-tostart]);
                   index_fields[i]="POStag";
                   INDEX_FIELDS[i++]=nerTag[dc - tostart];
                   index_fields[i]="NERtag";
                   INDEX_FIELDS[i++]=posTag[dc - tostart];
                   
                   d1 = this.constructDoc();
                   //System.err.println(dc+"\n"+nerTag[dc - tostart]+"\n"+posTag[dc - tostart]);
                    
                    int dateoftweet=Integer.parseInt(d.get("time").split(" ")[2]);
                    for(int indexcount=dateoftweet; indexcount<=tweet_ends_from_date;indexcount++)
                      
                            indexWriter[indexcount-tweet_starts_from_date].addDocument(d1);
                    //System.err.println(d1.get("DOCNO"));
                }
                System.out.println(c+" documents indexed in "+j+"th index.");
                for(int i1=0;i1<bulkTagNo;i1++)
                    toTag[i1]="";
                count = 0;
            if((c + 1) < docnoinIndex)    
                toTag[count++] = indexReader[j].document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", "");
            
            } 
            else {  
                toTag[count++] = indexReader[j].document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", "");
               // System.err.println(toTag[count-1]);

            }
        }
    }

    public Document constructDoc() throws IOException {
        
        Document doc = new Document();

        for(int i=0; i<INDEX_FIELDS.length;i++)
            doc.add(new Field(index_fields[i], INDEX_FIELDS[i],
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));

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
