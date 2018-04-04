/*
* Copyright © 2015 S.J. Blair <https://github.com/sjblair>
* This work is free. You can redistribute it and/or modify it under the
* terms of the Do What The Fuck You Want To Public License, Version 2,
* as published by Sam Hocevar. See the COPYING file for more details.
*/
package generation_tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

public class DatamuseController {

    public DatamuseController() { }
    

    /**
     * Returns a list of similar words to the word/phrase supplied.
     * @param word A word of phrase.
     * @return A list of similar words.
     */
    public String findSimilar(String word) {
        String s = word.replaceAll(" ", "+");
        return getJSON("http://api.datamuse.com/words?rd="+s);
    }

    public int syllableCount(String word){
    	String s = word.replaceAll(" ","+");
    	int numSyllables = parseSyllables(getJSON("http://api.datamuse.com/words?sp="+s+"&qe=sp&md=s&max=1"));
    	 
    	return numSyllables;
    }
    
    public String synonyms(String word) {
    	String s = word.replaceAll(" ", "+");
//    	jp.parseScores(getJSON("http://api.datamuse.com/words?rel_syn=" + s));
    	return getJSON("http://api.datamuse.com/words?rel_syn=" + s);
    }

    /**
     * Query a URL for their source code.
     * @param url The page's URL.
     * @return The source code.
     */
    private String getJSON(String url) {
        URL datamuse;
        URLConnection dc;
        StringBuilder s = null;
        try {
            datamuse = new URL(url);
            dc = datamuse.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(dc.getInputStream(), "UTF-8"));
            String inputLine;
            s = new StringBuilder();
            while ((inputLine = in.readLine()) != null)
                s.append(inputLine);
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s != null ? s.toString() : null;
    }
    
    public int parseSyllables(String in) {
//    	System.out.println(in);
        JsonParserFactory factory=JsonParserFactory.getInstance();
        JSONParser parser=factory.newJsonParser();
        Map<?, ?> jsonData=parser.parseJson(in);
        List<?> al= (List<?>) jsonData.get("root");
        String syllableCount = (String) (((Map<?, ?>)al.get(0)).get("numSyllables"));
       int numSyllables = Integer.parseInt(syllableCount);
       return numSyllables;
    }
    
}
