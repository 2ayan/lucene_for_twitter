/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;


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
class TRECQuery extends AnalyzerClass{
    Properties          prop;
    public String       qId;
    public String       qTitle;
    public String       qDesc;
    public String       qNarr;
//    public Query        luceneQuery;
    public List<TRECQuery>  queries;
  
    

    TRECQuery(Properties prop) throws IOException {
        this.prop = prop;
        this.qId = "";
        this.qTitle = "";
        this.qDesc = "";
        this.qNarr = "";
        queries = new LinkedList<>();
        setAnalyzer();
    }

    public TRECQuery(Properties prop, String qId, String qTitle, String qDesc, String qNarr) throws IOException {
        this.prop = prop;
        this.qId = qId;
        this.qTitle = qTitle;
        this.qDesc = qDesc;
        this.qNarr = qNarr;
        queries = new LinkedList<>();
        setAnalyzer();
    }


    public TRECQuery(String qId, String qTitle, String qDesc, String qNarr) throws IOException {
        this.qId = qId;
        this.qTitle = qTitle;
        this.qDesc = qDesc;
        this.qNarr = qNarr;
        queries = new LinkedList<>();
        setAnalyzer();
    }

    TRECQuery() throws IOException {
        this.qId = "";
        this.qTitle = "";
        this.qDesc = "";
        this.qNarr = "";
        queries = new LinkedList<>();
        setAnalyzer();
    }

    /**
     * Returns the content of the 'queryField' from the query text
     * @param analyzer
     * @param queryField
     * @return (String) The content of the field
     * @throws Exception 
     */
    public String queryFieldAnalyze(Analyzer analyzer, String queryField) throws Exception {
        StringBuffer buff = new StringBuffer(); 
        TokenStream stream = analyzer.tokenStream(CumulativeIndexer.FIELD_TEXT, new StringReader(queryField));
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
        GetProjetBaseDirAndSetProjectPropFile setPropFile= new GetProjetBaseDirAndSetProjectPropFile();
        prop=setPropFile.prop;
    	
        FileReader fr = new FileReader(prop.getProperty("query.file"));
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
                qryElt.select("title").first().text(), 
                qryElt.select("desc").first().text(),
                qryElt.select("narr").first().text() );
            queries.add(query); 
           // System.out.println("Query num "+qryElt.select("num").first().text()+"  query title="+qryElt.select("title").first().text());
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
        String []queryFields = prop.getProperty("QueryFields2Search").split(",");

        BooleanQuery booleanQuery = new BooleanQuery();

        String[] titleTerms = queryFieldAnalyze(analyzer, qTitle).split("\\s+");
        for(String term : titleTerms) {
            Term t = new Term(CumulativeIndexer.FIELD_TEXT, term);
            Query tq = new TermQuery(t);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
        }

        if(queryFields != null)
        {
        for(int i=0;i<queryFields.length;i++)
        {
            if(queryFields[i].toLowerCase().equalsIgnoreCase("desc")) {
                System.err.println("Processing Desc");
            //* If description to be taken in the query
                String[] descTerms = queryFieldAnalyze(analyzer, qDesc).split("\\s+");
                for(String term : descTerms) {
                    Term t = new Term(CumulativeIndexer.FIELD_TEXT, term);
                    Query tq = new TermQuery(t);
                    booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
                }
            }

            if(queryFields[i].toLowerCase().equalsIgnoreCase("narr")) {
                System.err.println("Processing Narr");
            //* If narration to be taken in the query
                String[] narrTerms = queryFieldAnalyze(analyzer, qNarr).split("\\s+");
                for(String term : narrTerms) {
                    Term t = new Term(CumulativeIndexer.FIELD_TEXT, term);
                    Query tq = new TermQuery(t);
                    booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
                }
            }
        }
        }
        System.err.println("query length="+booleanQuery.toString().length() +" "+"query= "+booleanQuery.toString().replaceAll("TEXT:", ""));
        
        return booleanQuery;
    }
    
    public static void main(String args[]) throws IOException, Exception {

        TRECQuery trecquery= new TRECQuery();
    	trecquery.trecQueryParse();
        //setAnalyzer();
        trecquery.queryFieldAnalyze(trecquery.analyzer, "title");
        
        String atq[];
        for(int i=0;i<(int)trecquery.queries.size();i++)
        {
        String analyzedQuery = trecquery.queryFieldAnalyze(trecquery.analyzer, trecquery.queries.get(i).qTitle);
        atq=analyzedQuery.split(" ");
        for(int j=0;j<atq.length;j++)
            System.out.print(" "+atq[j]);
        System.out.println();
        }
        
    }
    }
