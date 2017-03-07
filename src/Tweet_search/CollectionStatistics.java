/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
public class CollectionStatistics {
    String      indexPath;
    IndexReader indexReader;
    String      field;
    int         docCount;
    long        colSize;
    int         uniqTermCount;
    static Properties prop;
    /* NOTE: This value is different from luke-Number Of Terms 
        I think luke-Number Of Terms = docCount+uniqTermCount.
        It is same in this case
    */
    HashMap<String, PerTermStat> perTermStat;

    public CollectionStatistics(IndexReader indexReader, String field) throws IOException {
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        this.field = field;
        perTermStat = new HashMap<>();
    }

    public CollectionStatistics() throws IOException {
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
        perTermStat = new HashMap<>();
    }

    /**
     * Initialize collectionStat:
     * docCount      - total-number-of-docs-in-index
     * colSize       - collection-size
     * uniqTermCount - unique terms in collection
     * perTermStat   - cf, df of each terms in the collection
     * @return 
     * @throws IOException 
     */
    public CollectionStatistics buildCollectionStat() throws IOException {
        
        long colSize = 0;

        CollectionStatistics collectionStat = new CollectionStatistics();

        collectionStat.docCount = indexReader.maxDoc();      // total number of documents in the index

        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(field);
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
        
        return collectionStat;
    }
    
    public void showCollectionStat(CollectionStatistics collStat) {
        System.out.println("Collection Size: " + collStat.colSize);
        System.out.println("Number of documents in collection: " + collStat.docCount);
        System.out.println("NUmber of unique terms in collection: " + collStat.uniqTermCount);

        for (Map.Entry<String, PerTermStat> entrySet : collStat.perTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println(key + " " + value.df);
        }
    }

    public static void main(String[] args) throws IOException {
        CollectionStatistics sc = new CollectionStatistics();
        sc.indexPath = prop.getProperty("indexPath"); //"/store/ir-data/indexed/toy/";
        sc.indexReader = DirectoryReader.open(FSDirectory.open(new File(sc.indexPath)));
        sc.field = "TEXT";

        sc = sc.buildCollectionStat();
        sc.showCollectionStat(sc);
    }
}
/*
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

}*/

