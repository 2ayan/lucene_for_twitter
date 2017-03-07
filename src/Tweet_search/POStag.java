/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;


import static Tweet_search.CollectionStatistics.prop;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author ayan
 */
public class POStag {
  String serializedTaggerModel;
  MaxentTagger tagger;
  Properties prop;
  
  public POStag() throws IOException, ClassCastException, ClassNotFoundException
  {
  GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
  prop=setPropFile.prop;
  serializedTaggerModel=prop.getProperty("stanford.pos.model");
  tagger =  new MaxentTagger(serializedTaggerModel);
  
  }
  
  String []POStagStringArray(String []toTag)
  {
      String []toReturn=new String[toTag.length];
      int i=0;
      for (String str : toTag) {
         // System.err.println(str+" ==> "+str.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Mc}\\p{P}]"," "));
        toReturn[i++]=tagger.tagString(str.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Mc}\\p{P}]"," "));
      }
      return toReturn;
  }
  
  String POStagString(String toTag)
  {
      return tagger.tagString(toTag.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Mc}\\p{P}]"," "));
  }
}
