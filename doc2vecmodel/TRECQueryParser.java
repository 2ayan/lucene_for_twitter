/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doc2vecmodel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author dwaipayan
 */
class TRECQuery {
    Properties          prop;
    public String       qId;
    public String       qTitle;
    public String       qDesc;
    public String       qNarr;
//    public Query        luceneQuery;
    public List<TRECQuery>  queries;

    TRECQuery(Properties prop) {
        this.prop = prop;
        this.qId = "";
        this.qTitle = "";
        this.qDesc = "";
        this.qNarr = "";
        queries = new LinkedList<>();
    }

    public TRECQuery(Properties prop, String qId, String qTitle, String qDesc, String qNarr) {
        this.prop = prop;
        this.qId = qId;
        this.qTitle = qTitle;
        this.qDesc = qDesc;
        this.qNarr = qNarr;
        queries = new LinkedList<>();
    }


    public TRECQuery(String qId, String qTitle, String qDesc, String qNarr) {
        this.qId = qId;
        this.qTitle = qTitle;
        this.qDesc = qDesc;
        this.qNarr = qNarr;
        queries = new LinkedList<>();
    }

    TRECQuery() {
        this.qId = "";
        this.qTitle = "";
        this.qDesc = "";
        this.qNarr = "";
        queries = new LinkedList<>();
    }

    /**
     * Returns the content of the 'queryField' from the query
     * @param analyzer
     * @param queryField
     * @return (String) The content of the field
     * @throws Exception 
     */
    public static String queryFieldAnalyze(Analyzer analyzer, String queryField) throws Exception {
        StringBuffer buff = new StringBuffer(); 
        TokenStream stream = analyzer.tokenStream(Indexer.FIELD_BOW, new StringReader(queryField));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();        
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            buff.append(term).append(" ");
        }
        stream.end();
        stream.close();
        return buff.toString();
    }

    /**
     * Sets the List-TrecQUERY named 'queries'.
     * <p/>
     * The queries are read from the file (path in prop.queryPath).
     * <p/>
    */
    void trecQueryParse() throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(prop.getProperty("queryPath"));
        BufferedReader br = new BufferedReader(fr);
        String line;
        StringBuffer txtbuff = new StringBuffer();

        while ((line = br.readLine()) != null)
            txtbuff.append(line).append("\n");
        String content = txtbuff.toString();

        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements qryElts = jdoc.select("top");

//        String tags[] = {"num", "title", "desc", "narr"};
        TRECQuery query;
        for (Element qryElt : qryElts) {
            query = new TRECQuery(prop, qryElt.select("num").first().text(), 
                qryElt.select("title").first().text().replace(".", ""), 
                qryElt.select("desc").first().text(),
                qryElt.select("narr").first().text());
            queries.add(query); 
        }
    }

    /**
     * Returns the BOW representation of query as BooleanQuery;
     * takes into account the fields of the query as stated in prop.queryField.
     * 
     * <p/>
     * NOTE: if queryField is not mentioned in the properties
     * 'title' of the query is always taken as the query
     * <p/>
    */
    Query getBOWQuery(Analyzer analyzer) throws Exception {
        String queryFields = prop.getProperty("queryField");

        BooleanQuery booleanQuery = new BooleanQuery();

        String[] titleTerms = queryFieldAnalyze(analyzer, qTitle).split("\\s+");
        for(String term : titleTerms) {
            Term t = new Term(Indexer.FIELD_BOW, term);
            Query tq = new TermQuery(t);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
        }

        if(queryFields != null) {
            if(-1 != queryFields.toLowerCase().indexOf("d")) {
            //* If description to be taken in the query
                String[] descTerms = queryFieldAnalyze(analyzer, qDesc).split("\\s+");
                for(String term : descTerms) {
                    Term t = new Term(Indexer.FIELD_BOW, term);
                    Query tq = new TermQuery(t);
                    booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
                }
            }

            if(-1 != queryFields.toLowerCase().indexOf("n")) {
            //* If narration to be taken in the query
                String[] narrTerms = queryFieldAnalyze(analyzer, qNarr).split("\\s+");
                for(String term : narrTerms) {
                    Term t = new Term(Indexer.FIELD_BOW, term);
                    Query tq = new TermQuery(t);
                    booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
                }
            }
        }

        return booleanQuery;
    }
}

//public class TRECQueryParser extends DefaultHandler {
//    StringBuffer        buff;      // Accumulation buffer for storing the current topic
//    String              fileName;
//    TRECQuery           query;
//    
//    public List<TRECQuery>  queries;
//    final static String[] tags = {"num", "title", "desc", "narr"};
//
//    public TRECQueryParser(String fileName) throws SAXException {
//       this.fileName = fileName;
//       buff = new StringBuffer();
//       queries = new LinkedList<>();
//    }
//
//    public void parse() throws Exception {
//        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
//        SAXParser saxParser = saxParserFactory.newSAXParser();
////        saxParserFactory.setValidating(false);
////        SAXParser saxParser = saxParserFactory.newSAXParser();
////        saxParser.parse(fileName, this);
//        saxParser.parse(fileName, this);
//    }
//
//    @Override
//    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//        if (qName.equalsIgnoreCase("top"))
//            query = new TRECQuery();
//    }
//    
//    @Override
//    public void endElement(String uri, String localName, String qName) throws SAXException {
//        //System.out.println(buff);
//        if (qName.equalsIgnoreCase("title")) {
//            query.qTitle = buff.toString().trim();
//            buff.setLength(0);
//        }
//        else if (qName.equalsIgnoreCase("desc")) {
//            query.qDesc = buff.toString().trim();
//            buff.setLength(0);
//        }
//        else if (qName.equalsIgnoreCase("num")) {
//            query.qId = buff.toString().trim();
//            buff.setLength(0);
//        }
//        else if (qName.equalsIgnoreCase("narr")) {
//            query.qNarr = buff.toString().trim();
//            buff.setLength(0);
//        }
//        else if (qName.equalsIgnoreCase("top")) {
//            queries.add(query);
//        }        
//    }
//
//    @Override
//    public void characters(char ch[], int start, int length) throws SAXException {
//        buff.append(new String(ch, start, length));
//    }
//
//    public static void main(String[] args) {
//        if (args.length < 1) {
//            args = new String[1];
//            System.err.println("usage: java TRECQueryParser <input xml file>");
//            args[0] = "/home/dwaipayan/ir/ir-data/trec/topics_sax/trec678";
//        }
//
//        try {
//            TRECQueryParser parser = new TRECQueryParser(args[0]);
//            parser.parse();
//
//            for (TRECQuery query : parser.queries) {
//                System.out.println("ID: "+query.qId);
//                System.out.println("Title: "+query.qTitle);
//            }
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//}    
