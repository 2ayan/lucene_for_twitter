/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import static java.lang.Character.isLetter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *
 * @author dwaipayan
 * 
 * Calculates the similar terms of each of the word of the vocabulary dump
 * 
 */
public class WordVecs {

    Properties prop;
    int k;          // k in kNN
    HashMap<String, WordVec> wordvecmap;
    HashMap<String, List<WordVec>> nearestWordVecsMap; // Store the pre-computed NNs after read from file
    static WordVecs singleTon;

    //ArrayList<WordVec> distList;

//    public WordVecs() {
//        
//    }

    public WordVecs(String propFile) throws Exception {
        
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        if(prop.contains("vectorPath")) {
        //distList = new ArrayList<>(wordvecmap.size());
            String wordvecFile = prop.getProperty("vectorPath");

            k = Integer.parseInt(prop.getProperty("k", "20"));        
            wordvecmap = new HashMap();
            try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
                String line;

                while ((line = br.readLine()) != null) {
                    WordVec wv = new WordVec(line);
                    if(isLegalToken(wv.word))
                        wordvecmap.put(wv.word, wv);
                }
            }

            //computeAndStoreNNs();
            //loadPrecomputedNNs();
        }
        else {
            System.err.println("vectorPath not set in properties");
            System.exit(1);
        }

    }

    public WordVecs(Properties prop) throws Exception { 
        this.prop = prop;

        //distList = new ArrayList<>(wordvecmap.size());

        if(prop.containsKey("vectorPath")) {
            String wordvecFile = prop.getProperty("vectorPath");

            k = Integer.parseInt(prop.getProperty("k", "20"));
            wordvecmap = new HashMap();
            try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
                String line;

                while ((line = br.readLine()) != null) {
                    WordVec wv = new WordVec(line);
                    if(isLegalToken(wv.word))
                        wordvecmap.put(wv.word, wv);
                }
            }
    //        computeAndStoreNNs();
    //        loadPrecomputedNNs();
        }
        else {
            System.err.println("vectorPath not set in properties");
            System.exit(1);
        }
    }

    public void printNNs() {
        for (Map.Entry<String, List<WordVec>> entry : nearestWordVecsMap.entrySet()) {
            List<WordVec> nns = entry.getValue();
            String word = entry.getKey();
            StringBuffer buff = new StringBuffer(word);
            buff.append(" ");
            for (WordVec nn : nns) {
                buff.append(nn.word).append(":").append(nn.querySim);
            }
            System.out.println("<");
            System.out.println(buff.toString());
            System.out.println(">");
        }
    }

    static public WordVecs createInstance(Properties prop) throws Exception {
        if(singleTon == null) {
            singleTon = new WordVecs(prop);
            singleTon.loadPrecomputedNNs();
            System.out.println("Precomputed NNs loaded");
        }
        return singleTon;
    }

    /* compute the similer words for each of the words and store them in a file */
    public void computeAndStoreNNs() throws FileNotFoundException {
        String nnDumpPath = prop.getProperty("nnDumpPath");
        if(nnDumpPath!=null) {
            File f = new File(nnDumpPath);
        }
        else {
            System.out.println("Null found");
            return;
        }

        System.out.println("Dumping the NN in: "+ nnDumpPath);
        PrintWriter pout = new PrintWriter(nnDumpPath);

        System.out.println("Precomputing NNs for each word");
//        nearestWordVecsMap = new HashMap<>(wordvecmap.size());
//        System.out.println("Size: "+ wordvecmap.size());

        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if(isLegalToken(wv.word)) { 
                System.out.println("Precomputing NNs for " + wv.word);
                List<WordVec> nns = computeNNs(wv.word);
                if (nns != null) {
                    pout.print(wv.word + "\t");
                    for (int i = 0; i < nns.size(); i++) {
                        WordVec nn = nns.get(i);
                        pout.print(nn.word + ":" + nn.querySim + "\t");
                    }
                    pout.print("\n");
//                    nearestWordVecsMap.put(wv.word, nns);
                }
            }
        }
        pout.close();
    }

    /* returns the already computed similar words of 'queryWord' */
    public List<WordVec> getPrecomputedNNs(String queryWord) {
        return nearestWordVecsMap.get(queryWord);
    }

    /* compute the similar words of 'queryWord' */
    public List<WordVec> computeNNs(String queryWord) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }

//    public WordVec getVec(String word) {
//        return wordvecmap.get(word);
//    }

    public double getSim(String u, String v) {
        WordVec uVec = wordvecmap.get(u);
        WordVec vVec = wordvecmap.get(v);
        if (uVec == null || vVec == null) {
//            System.err.println("words not found...<" + ((uVec == null)?u:v) + ">");
            return 0;
        }

        return uVec.cosineSim(vVec);
    }

    private boolean isLegalToken(String word) {
        boolean flag = true;
        for ( int i=0; i< word.length(); i++) {
//            if(isDigit(word.charAt(i))) {
//                flag = false;
//                break;
            if(isLetter(word.charAt(i))) {
                continue;
            }
            else {
                flag = false;
                break;
            }
        }
        return flag;
    }
    
    /* load the precomputed NNs into hash table */
    public void loadPrecomputedNNs() throws FileNotFoundException, IOException {
        nearestWordVecsMap = new HashMap<>();
        String NNDumpPath = prop.getProperty("NNDumpPath");
        if (NNDumpPath == null) {
            System.out.println("NNDumpPath Null while reading");
            return;
        }
        System.out.println("Reading from the NN dump at: "+ NNDumpPath);
        File NNDumpFile = new File(NNDumpPath);
        
        try (FileReader fr = new FileReader(NNDumpFile);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " \t:");
                List<String> tokens = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    tokens.add(token);
                }
                List<WordVec> nns = new LinkedList();
                int len = tokens.size();
                //System.out.print(tokens.get(0)+" > ");
                for (int i=1; i < len-1; i+=2) {
                    nns.add(new WordVec(tokens.get(i), Float.parseFloat(tokens.get(i+1))));
                    //System.out.print(tokens.get(i) + ":" + tokens.get(i+1));
                }
                //System.out.println();
                nearestWordVecsMap.put(tokens.get(0), nns);
            }
            System.out.println("NN dump has been reloaded");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
	if(args.length == 0) {
            args = new String[2];
            args[0] = "/home/dwaipayan/doc2vecModel/init.properties";
	}
        try {
	    WordVecs qe = new WordVecs(args[0]);
            qe.computeAndStoreNNs();
            //qe.loadPrecomputedNNs();
//            List<WordVec> nwords = qe.computeNNs("conclus");
//            for (WordVec word : nwords) {
//                System.out.println(word.word + "\t" + word.querySim);
 //           }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

    

