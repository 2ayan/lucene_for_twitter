package Tweet_search;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class GetCollStat {

    Properties      prop;
    String          queryPath;      // path of the query file
    File            queryFile;      // the query file
    File        indexFile;          // place where the index is stored
    Analyzer    analyzer;           // the analyzer
    IndexReader     reader;
    IndexSearcher   searcher;
    CollectionStat collectionStat;
    String fname;
    public GetCollStat() {
		// TODO Auto-generated constructor stub
	}
    
    public GetCollStat(String propPath, int indexcount) throws FileNotFoundException, IOException {
    	//prop = new Properties();
       // prop.load(new FileReader(propPath));
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        /* property files are loaded */

        /* setting the analyzer */
        SetAndGetAnalyzer setGetAnalyzer = new SetAndGetAnalyzer(prop);
        setGetAnalyzer.setAnalyzer();
        analyzer = setGetAnalyzer.getAnalyzer();
        /* analyzer is set */

        /* index path setting */
        fname=prop.getProperty("indexPath");
        if(!fname.endsWith("/"))
            fname=fname.concat("/"+Integer.toString(indexcount+20));
        
        System.out.println("Using index at: "+fname);
        indexFile = new File(fname);
        System.out.println("Using file: "+indexFile);
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexFile.getAbsolutePath());
            System.out.println("Terminating");
//            boolIndexExists = false;
            System.exit(1);
        }

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));
        searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new DefaultSimilarity());
        searcher.setSimilarity(new LMJelinekMercerSimilarity(0.6f));
        /* reader and searcher set */

        /* setting query path */
        queryPath = prop.getProperty("query.file");
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        //queries = constructQuery();
        /* constructed the query */
        collectionStat = new CollectionStat();


    }
    
    public void buildCollectionStat() throws IOException {
//        System.out.println("Here");
        long colSize = 0;
        
        collectionStat.docCount = reader.maxDoc();      // total number of documents in the index
        
        /* Inserting terms of <title> into hashmap*/
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(CumulativeIndexer.FIELD_TEXT);
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
        
        /* Inserting terms of <desc> into hashmap*/
        terms = fields.terms(CumulativeIndexer.FIELD_TEXT);
        iterator = terms.iterator(null);
        byteRef = null;

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
            System.out.println(key + " --> df=" + value.df+"  cf="+value.cf);
        }
    }
    

    public static void main(String[] args) throws FileNotFoundException, IOException {
        int indexcount=9;
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        
        GetCollStat getCollStat = new GetCollStat(setPropFile.propFileName,indexcount);
        getCollStat.buildCollectionStat();
        getCollStat.showCollectionStat();
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
