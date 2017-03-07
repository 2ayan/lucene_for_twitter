/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

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
 * @author ayan
 */
public class AnalyzerClass extends TextProcess{
    
    Analyzer analyzer;
    Properties prop; 

    AnalyzerClass() throws IOException {
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
    }
    
    AnalyzerClass(String rPropFile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
     public final void setAnalyzer() throws IOException {
      

        String stopFile = prop.getProperty("stopFile");
        
        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(stopFile);
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
        // return analyzer;
    }
    
    public org.apache.lucene.analysis.Analyzer getAnalyzer() {
        return analyzer;
    }
    
    
}
