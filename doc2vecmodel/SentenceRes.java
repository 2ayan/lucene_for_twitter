/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

/**
 *
 * @author dwaipayan
 */
class SentenceRes {
    String qId;
    String q0;
    String docId;
    String sentence;
    int rank;
    float score;
    String runName;
    int luceneDocId;

    public SentenceRes(String query_num, String docId, int luceneDocId, String sentence, int rank, float score, String run_name) {
        this.qId = query_num;
        this.docId = docId;
        this.luceneDocId = luceneDocId;
        this.sentence = sentence;
        this.rank = rank;
        this.score = score;
        this.runName = run_name;
    }    
}
