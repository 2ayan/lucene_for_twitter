/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tweet_search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ayan
 */
public class TextProcess {


    public String remove_control_char(String text) {
        return text.replaceAll("[\u0000-\u001f]", "").replaceAll("\\p{Cntrl}", "");
    }

    public String remove_non_ascii_char(String text) {
        return text.replaceAll("[^\\x20-\\x7e]", " ");
    }
    
        public static String refineTexts(String txt) {
    /* removes all special characters from txt, removes numericals etc. */

        // removes the urls
        txt = removeUrl(txt);

        // removes any special characters
        txt = refineSpecialChars(txt);

        // removes any numerical values
        txt = removeNumerical(txt);

        return txt;
    }

    public static String removeNumerical(String s) {
        /* removes all numerical tokens present in s */
        StringBuffer finalStr = new StringBuffer();

        String []tokens;
        tokens = s.trim().split(" ");
        for (String token : tokens) {
            if (!(token == null) && !isNumerical(token)) {
                finalStr.append(token).append(" ");
            }
        }

        return finalStr.toString();
    }

    public static String removeUrl(String str)
    {
        try {
            String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$~_?\\+-=\\\\\\.&]*)";
            Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(str);
            int i = 0;
            while (str!=null && m.find()) {
                str = str.replaceAll(m.group(0)," ").trim();
                i++;
            }
            return str;
        }
        catch(Exception e) {
            
        }
        return str;
    }

    public static boolean isNumerical(String s) {
        boolean isInt;
        boolean isDouble = false;

        try { 
            Integer.parseInt(s); 
            isInt = true;
        } catch(NumberFormatException e) { 
            isInt = false; 
        }
        
        if(!isInt) {
            try {
                Double.parseDouble(s);
                isDouble = true;
            } catch (NumberFormatException e) {
                isDouble = false;
            }
        }

        return isInt || isDouble;
    }

    public static String refineSpecialChars(String txt) {
        if(txt!=null)
            txt = txt.replaceAll("\\p{Punct}+", " ");
        return txt;
    }
}
