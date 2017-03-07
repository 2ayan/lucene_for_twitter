package doc2vecmodel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author dwaipayan
 */
public class WordVec implements Comparable<WordVec> {
    String  word;       // the word
    double[] vec;        // the vector for that word
    double   norm;       // the normalized value
    double   querySim;   // distance from a reference query point

    WordVec() { }
    
    WordVec(String word, double[] vec) {
        this.word = word;
        this.vec = vec;
    }

    WordVec(String word, double[] vec, double querySim) {
        this.word = word;
        this.vec = vec;
        this.querySim = querySim;
    }

    WordVec(String word, double querySim) {
        this.word = word;
        this.querySim = querySim;
    }

    WordVec(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new double[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
        norm = getNorm();
    }

    public String getWord() {
        return word;
    }

    double getNorm() {
        if (norm == 0) {
            // calculate and store
            double sum = 0;
            for (int i = 0; i < vec.length; i++) {
                sum += vec[i]*vec[i];
            }
            norm = (double)Math.sqrt(sum);
        }
        return norm;
    }

    double cosineSim(WordVec that) {
        double sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            if (that == null) {
                return 0;
            }
            sum += vec[i] * that.vec[i];
        }
        return sum / (this.norm*that.norm);
    }

    @Override
    public int compareTo(WordVec that) {
        return this.querySim > that.querySim? -1 : this.querySim == that.querySim? 0 : 1;
    }

    byte[] getBytes() throws IOException {
        byte[] byteArray;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutput out;
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            byteArray = bos.toByteArray();
            out.close();
        }
        return byteArray;
    }
    
    static WordVec add(WordVec a, WordVec b) {
        WordVec sum = new WordVec(a.word + ":" + b.word);
        sum.vec = new double[a.vec.length];
        for (int i = 0; i < a.vec.length; i++) {
            sum.vec[i] = .5 * (a.vec[i]/a.getNorm() + b.vec[i]/b.getNorm());
        }        
        return sum;
    }

}
    