/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

/**
 *
 * @author dwaipayan
 */
public class SetAndGetAnalyzer {
    Properties      prop;
    Analyzer        analyzer;
    String          stopPath;

    public SetAndGetAnalyzer(String propPath) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propPath));
        stopPath = prop.getProperty("stopFile");
    }

    public SetAndGetAnalyzer(Properties prop) throws IOException {
        this.prop = new Properties();
        this.prop = prop;
        stopPath = prop.getProperty("stopFile");
    }

    public final void setAnalyzer() {
//        String stopPath = prop.getProperty("stopFile");
        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(stopPath);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close(); fr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

}
