/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package QueryExpansion;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import static jdk.nashorn.internal.objects.ArrayBufferView.buffer;
//import edu.smu.tspell.wordnet;
/**
 *
 * @author ayan
 */
public class Wordnet {
    
    public static void main(String[] args) {
        System.setProperty("wordnet.database.dir", "/y/home/mnt/ayan/.install/WordNet-3.0/dict/");
        String wordForm = "fish";
        
                        //  Get the synsets containing the wrod form
                        WordNetDatabase database = WordNetDatabase.getFileInstance();
                        Synset[] synsets = database.getSynsets(wordForm);
                        //  Display the word forms and definitions for synsets retrieved
                        if (synsets.length > 0)
                        {
                                System.out.println("The following synsets contain '" +
                                                wordForm + "' or a possible base form " +
                                                "of that text:");
                                for (int i = 0; i < synsets.length; i++)
                                {
                                        System.out.println("");
                                        String[] wordForms = synsets[i].getWordForms();
                                        for (int j = 0; j < wordForms.length; j++)
                                        {
                                                System.out.print((j > 0 ? ", " : "") +
                                                                wordForms[j]);
                                        }
                                        System.out.println(": " + synsets[i].getDefinition());
                                }
                        }
                        else
                        {
                                System.err.println("No synsets exist that contain " +
                                                "the word form '" + wordForm + "'");
                        }
    }
    
    
}
