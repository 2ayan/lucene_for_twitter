/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
class QueryComposition {
    String pseudoTerm;  // the pseudo term indicating the concept of this query
    double sim;
    WordVec wvec;    

    public QueryComposition(String[] qTerms, WordVec wvec) {
        if (qTerms != null) {
            StringBuffer buff = new StringBuffer();
            for (String qTerm : qTerms) {
                buff.append(qTerm).append(":");
            }
            pseudoTerm = buff.toString();
            this.wvec = wvec;
        }
    }
    
    public QueryComposition(String qTerm, WordVec wvec) {
        pseudoTerm = qTerm;
        this.wvec = wvec;
    }
    
    void setSim(double sim) { this.sim = sim; }
    
    // If the term is a composed term, there has to be a : separating
    // the individual terms because that's how we stored the pseudoTerm.
    // (ref. constructor of this class).
    // If composed, return prod_t in C P(t)
    float computeP_Q(InitialRetrStats retrstats) {
        String[] qterms = pseudoTerm.split(":");
        float prob = 1.0f;
        for (String qterm : qterms) {
            prob = prob * retrstats.getNormalizedTf(qterm);
        }
        return prob;
    }
}

/*
interface IKDEstimator {
    float estimate();
}

abstract class KDEstimator implements IKDEstimator {
    InitialRetrStats stats;

    public KDEstimator(InitialRetrStats stats) {
        this.stats = stats;
    }
}

class GaussianKDEstimator extends KDEstimator {

    public GaussianKDEstimator(InitialRetrStats stats) {
        super(stats);
    }

    @Override
    public float estimate() {
    }
}
*/

class QueryCompositions implements Comparable<QueryCompositions> {
    WordVec wvec; // current term
    List<QueryComposition> qlist;
    long tf;
    long df;
    float ntf;
    float p_R;  // estimated f^\hat(t)
    
    public QueryCompositions() {
        qlist = new ArrayList<>();
    }

    void addComposition(QueryComposition qc) {
        qlist.add(qc);
    }
    
    public QueryCompositions(WordVec wvec, long tf, long df) {
        this.wvec = wvec;
        this.qlist = new ArrayList<>();
        this.tf = tf;
        this.df = df;
    }
    
    void increment(long tf, long df) {
        this.tf += tf;
        this.df += df;
    }
    
    // Fill up the info for this object wrt to the current term (wvec)
    void computeSimilarities() {
        for (QueryComposition qc : qlist) {
            qc.sim = wvec.cosineSim(qc.wvec);
        }
    }    

    @Override
    public int compareTo(QueryCompositions that) {
        return this.p_R < that.p_R? 1 : this.p_R == that.p_R? 0 : -1;
    }
}

