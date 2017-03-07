package Tweet_search;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class DocVector extends AnalyzerClass{
    Properties prop;        // the properties file
    IndexReader []reader;
    IndexSearcher []searcher;
    String terms_from_doc[]= new String[100];
    int terms_tf_from_doc[]=new int[100];
    
    int tweet_starts_from_date;
    int tweet_ends_from_date;

    public DocVector() throws IOException {
        setAnalyzer();
        getAnalyzer();
        tweet_starts_from_date=Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date=Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));
    }

    public DocVector(String propFile) throws IOException {
        setAnalyzer();
        getAnalyzer();
        
       
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
                tweet_starts_from_date=Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date=Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));
        String indexDirectoryPath = prop.getProperty("indexPath");
        if(!indexDirectoryPath.endsWith("/"))
            indexDirectoryPath=indexDirectoryPath.concat("/");
        
        Directory []directory;
        directory=new Directory[tweet_ends_from_date-tweet_starts_from_date+1];
        reader=new IndexReader[tweet_ends_from_date-tweet_starts_from_date+1];
        searcher=new IndexSearcher[tweet_ends_from_date-tweet_starts_from_date+1];
        for(int i=0;i<tweet_ends_from_date-tweet_starts_from_date+1;i++){
            directory[i] = FSDirectory.open(new File(indexDirectoryPath.concat(Integer.toString(i+tweet_starts_from_date))));
            reader[i] = DirectoryReader.open(directory[i]);
            searcher[i] = new IndexSearcher(reader[i]);
        }
    }

    private void getDocumentVector(String docid, String fieldName, int indexcounter) throws ParseException, IOException {
    /*  Give the term vector of document 'doi' of the field 'fieldName' 
        fieldName = 'abstract', 'contexts', 'title'     */
        ScoreDoc[] hits;
        TopDocs topDocs;

       // Analyzer analyzer = new WhitespaceAnalyzer();
        QueryParser queryParser = new QueryParser(CumulativeIndexer.FIELD_ID, analyzer);
        Query query = queryParser.parse(docid);
        topDocs = searcher[indexcounter].search(query, 1);

        hits = topDocs.scoreDocs;
        if(hits == null) {
            System.out.println("Document not found");
        }
        else {
            Document d = searcher[indexcounter].doc(hits[0].doc);
            System.out.println("Lucene DocNum: "+hits[0].doc);

            Terms vector = reader[indexcounter].getTermVector(hits[0].doc, fieldName);

            TermsEnum termsEnum = null;
            termsEnum = vector.iterator(termsEnum);
            BytesRef text;
            for(int i=0;i<100;i++)
                {
                terms_from_doc[i]=null;                
                terms_tf_from_doc[i++]=0;
                }
            int i=0;
            while ((text = termsEnum.next()) != null) {
                String term = text.utf8ToString();
               // System.out.print(term+" ");
               // System.out.println(termsEnum.totalTermFreq());
                terms_from_doc[i]=term;                
                terms_tf_from_doc[i++]=(int)termsEnum.totalTermFreq();
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
//        if(args.length == 0) {
//            args = new String[1];
//            System.out.println("Usage: java DocVector <prop-file>");
//            args[0] = "/c/lucene_for_twitter/init.properties";
//            //System.exit(0);
//        }
        int indexcount=9;
        
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
      
        
        DocVector docVector = new DocVector(setPropFile.propFileName);
        
        String docid = "2015-07-26-18-000000000000";
        docVector.getDocumentVector(docid, "TEXT",indexcount);
        
        GetCollStat getcollstat= new GetCollStat(setPropFile.propFileName,indexcount);
        getcollstat.buildCollectionStat();
        int i = 0;

        
        while(docVector.terms_from_doc[i] != null){
//            df_cf=getcollstat.showCollectionStat_for_a_term((String)docVector.terms_from_doc[i]);
            PerTermStat pts = new PerTermStat();
            pts = getcollstat.collectionStat.perTermStat.get((String)docVector.terms_from_doc[i]);
//            System.out.println("term=<"+docVector.terms_from_doc[i]+"> tf="+docVector.terms_tf_from_doc[i]+" df="+df_cf[0]+" cf="+df_cf[1]);
                        System.out.println(docVector.reader[0].getDocCount("TEXT")+" term=<"+docVector.terms_from_doc[i]+"> tf="+docVector.terms_tf_from_doc[i]+" df="+pts.df+" cf="+pts.cf);
            i++;
        }
        
        
    }
} 