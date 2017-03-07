/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author ayan
 */

public class NERtag  {
  String serializedClassifier;
  AbstractSequenceClassifier<CoreLabel> classifier;
  Properties prop;
  
  public NERtag() throws IOException, ClassCastException, ClassNotFoundException
  {
  GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
  prop=setPropFile.prop;
  serializedClassifier=prop.getProperty("stanford.ner.classifier");
  
  classifier = CRFClassifier.getClassifier(serializedClassifier);
  }
  
  String []NERtagStringArray(String []toTag)
  {
      String []toReturn=new String[toTag.length];
      int i=0;
      for (String str : toTag) {
        toReturn[i++]=classifier.classifyToString(str.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Mc}\\p{P}]"," "));
      }
      return toReturn;
  }
  
  String NERtagString(String toTag)
  {
      return classifier.classifyToString(toTag.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Mc}\\p{P}]"," "));
  }
}
