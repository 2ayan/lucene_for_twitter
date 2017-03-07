package Tweet_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
/*import org.apache.lucene.document.TextField;*/

import org.apache.lucene.analysis.TokenFilter;
/**
 *
 * @author dwaipayan
 */
public class TrecDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
        protected String parseTags;//="DOCNO,tweettime,time,userid,follower,friend,listed,status,retweet,favourite,lang,retweetedfrom,TEXT";
        protected String []parseTagArray;
	
        Pattern []patternTag;
        Properties  prop;
        
	public TrecDocIterator(File file) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
//            System.out.println("Reading " + file.toString());
           
            
            GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
            prop=setPropFile.prop;
            parseTags=prop.getProperty("index_fields","null");
            parseTagArray=new String[parseTags.split(",").length];
            parseTagArray=parseTags.split(",");
	}
	
	@Override
	public boolean hasNext() {
            return !at_eof;
	}

	@Override
	public Document next() {
            Document doc = new Document();
            StringBuffer sb = new StringBuffer();
            String regexPattern="(.*)";
            
            try {
                String line;
               
                patternTag=new Pattern[parseTagArray.length];
                
                for(int i=0;i<parseTagArray.length;i++)
                {
                    String tmp="<"+parseTagArray[i]+">"+regexPattern+"</"+parseTagArray[i]+">";
                    patternTag[i]= Pattern.compile(tmp);
                   // System.err.println(tmp);
                }
                    
                boolean in_doc = false;
                while (true) {
                    line = rdr.readLine();
//                    char []c=line.toCharArray();
//                    for(int i=0;i<c.length;i++)
// {
// // replace with Y
//                    System.err.println(c[i]+"="+Character.UnicodeBlock.of(c[i])+" ");
//}
//                    
//                    System.err.println("line="+line);
                    if (line == null|| line=="null") {
                        at_eof = true;
                        return null;
                    }
                    if (!in_doc) {
                        if (line.startsWith("<DOC>"))
                            in_doc = true;
                        else
                            continue;
                    }
                    if (line.startsWith("</DOC>")) {
                        in_doc = false;
                        sb.append(line);
                        break;
                    }
 
                    sb.append(" ");
                    sb.append(line.replaceAll("[\\x80-\\x8f]", ""));
                    
                }
                in_doc = false;
                
            for(int i=0;i<parseTagArray.length;i++)
                        patternMatcher(i, parseTagArray[i], sb.toString(), doc);
            sb=null;

            } catch (IOException e) {
                doc = null;
            }
            return doc;
	}

	@Override
	public void remove() {
            // Do nothing, but don't complain
	}
        
        protected void patternMatcher(int i,String tag, String toParse, Document doc)
        {
            Matcher m = patternTag[i].matcher(toParse);
                    if (m.find()) {
                        String content = m.group(1);
                        if(content==null)
                        {
                            doc.add(new StringField(tag, "null", Field.Store.YES));
                            //System.err.println("content null="+content);
                        }
                        else
                        {
                            doc.add(new StringField(tag, content, Field.Store.YES));
                            //System.err.println("content="+content);
                        }
                    }
        }
        

}
