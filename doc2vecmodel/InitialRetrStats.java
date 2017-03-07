/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.util.ArrayList;
import java.util.Collections;
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
public class InitialRetrStats {
    Map<String, QueryCompositions> termStats;
    long sumTf;
    TermLevelPRF tlprf;

    public InitialRetrStats() {
        termStats = new HashMap<>();
    }
    
    public InitialRetrStats(TermLevelPRF tlprf, ScoreDoc[] hits, TRECQuery query, int topNumDocs) {
        this.tlprf = tlprf;
        termStats = new HashMap<>();
        try {
            load(tlprf.reader, tlprf.wordVecs, hits, query, topNumDocs);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    void setTermStats(List<QueryCompositions> qcomps) {        
        for (QueryCompositions qcomp : qcomps) {
            termStats.put(qcomp.wvec.word, qcomp);
        }
    }
    
    // Get the normalized term freq of a given term
    float getNormalizedTf(String term) {
        QueryCompositions qcompositions = termStats.get(term);
        return qcompositions.ntf;
    }
    
    private void load(IndexReader reader, WordVecs wordVecs, ScoreDoc[] hits, TRECQuery query, int topNumDocs) throws Exception {
        for (int i = 0; i < Math.min(topNumDocs, hits.length); i++) {
        /* for each of the initial retrieved documents */
            int docId = hits[i].doc;
            RetrievedDocumentVector singleDoc = new RetrievedDocumentVector();
            singleDoc.luceneDocId = docId;
            singleDoc.docLength = 0;

            Terms vector = reader.getTermVector(docId, Indexer.FIELD_BOW);
            TermsEnum termsEnum = null;
            termsEnum = vector.iterator(termsEnum);
            BytesRef text;
            
            while ((text = termsEnum.next()) != null) {
            /* for each of the words of that initially retrieved document */
                String term = text.utf8ToString();
                long termFreq = termsEnum.totalTermFreq();
                singleDoc.perTerm.put(term, termFreq);
                singleDoc.docLength += termFreq;

                QueryCompositions compositionInfo = termStats.get(term);

                if (compositionInfo == null) {
                    // seen a new term
                    WordVec wv = wordVecs.wordvecmap.get(term);
                    compositionInfo = new QueryCompositions(wv, termFreq, 1);
                    //* (term, vector, similarity, tf, df)
                    //* querySim is initialised with -2.0 
                    termStats.put(term, compositionInfo);
                }
                else {
                    compositionInfo.increment(termFreq, 1);
                }
                sumTf += termFreq;
            }
            //retrievedDocVec.add(singleDoc);
        }
    }
    
    void normalizeTermFreqs() {
        for (Map.Entry<String, QueryCompositions> e : termStats.entrySet()) {
            QueryCompositions qc = e.getValue();
            qc.ntf = qc.tf/(float)sumTf;
        }
    }
    
    void computeSimsWithQueryCompositions() {
        for (Map.Entry<String, QueryCompositions> entry : termStats.entrySet()) {
            QueryCompositions qcomps = entry.getValue();
            
            //value.similarityWithEachComposedQuery.add(sim); // new similarity added to the similarity list
            qcomps.computeSimilarities();
        }        
    }
    
    float getP_R(String term) {
        QueryCompositions qcomps = termStats.get(term);
        return qcomps.p_R;        
    }
    
    // Compute f\hat(t) over all t \in V (vocab of initial retr set)
    void computeP_R() throws Exception {
        float sigma = tlprf.sigma;
        
        for (Map.Entry<String, QueryCompositions> e : termStats.entrySet()) {
            //* for each term in top R
            QueryCompositions termQcInfo = e.getValue();
            if (termQcInfo.wvec == null)
                termQcInfo = termQcInfo;
            String term = termQcInfo.wvec.word;

            long dfD = tlprf.reader.docFreq(new Term(Indexer.FIELD_BOW, term));  //collectionStat.perTermStat.get(term).df;

            double pRt = 0;

            double similarityScoreComponent = 0;
            //* (1/sqrt(2*PI*SIGMA)) * e^((-1/2)*(sim(t,q)/SIGMA)^2)
            //* SIGMA = 1
            
            for (QueryComposition qcInfo : termQcInfo.qlist) { // for each pivot query point...
                float p_Q = qcInfo.computeP_Q(this);
                similarityScoreComponent = (1/sigma * Math.sqrt(2*Math.PI)) * Math.exp(-0.5 * Math.pow(qcInfo.sim/sigma, 2));
                pRt = pRt + p_Q * similarityScoreComponent; 
            }
            
            termQcInfo.p_R = (float)pRt;
        }        
    }
}

class InitialRetStatsFilterer {
    int numTopTerms;
    InitialRetrStats stats;
    
    public InitialRetStatsFilterer(InitialRetrStats stats, int numTopTerms) {
        this.numTopTerms = numTopTerms;
        this.stats = stats;
    }
    
    InitialRetrStats filter() {
        InitialRetrStats filteredStats = new InitialRetrStats();
        
        List<QueryCompositions> allWords = new ArrayList<>();
        
        for (Map.Entry<String, QueryCompositions> e : stats.termStats.entrySet()) {
            QueryCompositions qcomps = e.getValue();
            allWords.add(qcomps);        
        }
        
        Collections.sort(allWords);
        List<QueryCompositions> topWords = allWords.subList(0, numTopTerms);
        filteredStats.setTermStats(topWords);
        
        return filteredStats;
    }
    
}