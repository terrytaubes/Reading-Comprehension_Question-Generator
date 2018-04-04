package generation_pipeline;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface_MultipleChoice utilizes the Merriam-Webster Thesaurus API to extract similar words to use as distractors.
 * 
 * @author TerranceTaubes
 */
public class Interface_MultipleChoice {   
		
	public Interface_MultipleChoice() {	}
    
	/**
	 * MultipleChoiceOptions()
	 * - Creates a hashmap for all types of words related to the input word (synonyms, relevant, near, antonyms)
	 * 
	 * @param input
	 * @return options
	 * @throws Exception
	 */
    public static HashMap<String, ArrayList<String>> MultipleChoiceOptions(String input) throws Exception {
    	
        String vals;
        String[] valsArray;
    	//ArrayList<String> synonym_words = new ArrayList<String>();
    	//ArrayList<String> relevant_words = new ArrayList<String>();
    	//ArrayList<String> near_words = new ArrayList<String>();
    	//ArrayList<String> ant_words = new ArrayList<String>();
    	ArrayList<String> all_words = new ArrayList<String>();
    	HashMap<String, ArrayList<String>> options = new HashMap<String, ArrayList<String>>();
    	
    	
        String headInter = new String("http://www.dictionaryapi.com/api/v1/references/ithesaurus/xml/");
        String apiKeyInter = new String("?key=cd381c2b-de85-41a6-a86e-6787e76d4fb6"); //My API Key for Merriam webster
        String finalURLInter = headInter.trim() + input.trim()+ apiKeyInter.trim();
        

        //System.out.println("API URL: " + finalURLInter);
        //System.out.println("API URL: " + finalURLAdv);
        
        NodeList synList;
        NodeList relList;
        NodeList nearList;
        NodeList antList;
        
        try
        {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            try {
	            	Document doc = b.parse(finalURLInter);
	            
	            
	            //System.out.println("Has children: " + doc.hasChildNodes());
	            
	            doc.getDocumentElement().normalize();
	            
	            NodeList items = doc.getElementsByTagName("entry");
	            
	            for (int i = 0; i < items.getLength(); i++) {
	                Node n = items.item(i);
	                
	                //System.out.println("Node " + i + ": " + n.getNodeValue());
	
	                if (n.getNodeType() != Node.ELEMENT_NODE) {
	                    continue;
	                }
	
	                Element e = (Element) n;
	                synList = e.getElementsByTagName("syn");
	                relList = e.getElementsByTagName("rel");
	                nearList = e.getElementsByTagName("near");
	                antList = e.getElementsByTagName("ant");
	                
	                //System.out.println("\nSynonym:");
	                
	                for (int j = 0; j < synList.getLength(); j++) {
	                    Node syn = synList.item(j);
	                    
	                    if (syn.getNodeType() != Node.ELEMENT_NODE) {
	                        continue;                   
	                	}
	                    
	                    Element synElem = (Element) synList.item(j);
	                    Node synNode = synElem.getChildNodes().item(0);
	                    
	                    //System.out.println(synNode.getNodeValue());
	                    
	                    vals = synNode.getNodeValue().replaceAll("[^a-zA-Z][^a-zA-Z]+", ",");
	                    valsArray = vals.split(",");
	                    for (int m = 0; m < valsArray.length; m++) {
	                    	//synonym_words.add(valsArray[m]);
	                    	all_words.add(valsArray[m]);
	                    }
	                }
	                
	                
	                //System.out.println("\nRelevant:");
	                
	                for (int j = 0; j < relList.getLength(); j++) {
	                    Node rel = relList.item(j);
	                    
	                    if (rel.getNodeType() != Node.ELEMENT_NODE) {
	                        continue;                   
	                	}
	                    
	                    Element relElem = (Element) relList.item(j);
	                    Node relNode = relElem.getChildNodes().item(0);
	                    
	                    //System.out.println(relNode.getNodeValue());
	                    
	                    vals = relNode.getNodeValue().replaceAll("[^a-zA-Z][^a-zA-Z]+", ",");
	                    valsArray = vals.split(",");
	                    for (int m = 0; m < valsArray.length; m++) {
	                    	//relevant_words.add(valsArray[m]);
	                    	all_words.add(valsArray[m]);
	                    }
	                }
	                
	                //System.out.println("\nNear:");
	                for (int j = 0; j < nearList.getLength(); j++) {
	                    Node near = nearList.item(j);
	                    
	                    if (near.getNodeType() != Node.ELEMENT_NODE) {
	                        continue;                   
	                	}
	                    
	                    Element nearElem = (Element) nearList.item(j);
	                    Node nearNode = nearElem.getChildNodes().item(0);
	                    
	                    //System.out.println(nearNode.getNodeValue());
	                    
	                    vals = nearNode.getNodeValue().replaceAll("[^a-zA-Z][^a-zA-Z]+", ",");
	                    valsArray = vals.split(",");
	                    for (int m = 0; m < valsArray.length; m++) {
	                    	//near_words.add(valsArray[m]);
	                    	all_words.add(valsArray[m]);
	                    }
	                }
	                
	                //System.out.println("\nAntonyms:");
	                for (int j = 0; j < antList.getLength(); j++) {
	                    Node ant = antList.item(j);
	                    
	                    if (ant.getNodeType() != Node.ELEMENT_NODE) {
	                        continue;                   
	                	}
	                    
	                    Element antElem = (Element) antList.item(j);
	                    Node antNode = antElem.getChildNodes().item(0);
	                    
	                    //System.out.println(antNode.getNodeValue());
	                    vals = antNode.getNodeValue().replaceAll("[^a-zA-Z][^a-zA-Z]+", ",");
	                    valsArray = vals.split(",");
	                    for (int m = 0; m < valsArray.length; m++) {
	                    	//ant_words.add(valsArray[m]);
	                    	all_words.add(valsArray[m]);
	                    }
	                }
	            }
	      
	            
	            options.put("all", all_words);
	            //options.put("syn", synonym_words);
	            //options.put("rel",  relevant_words);
	            //options.put("near", near_words);
	            //options.put("ant", ant_words);
	            
//	            for (String s : options.keySet()) {
//	        		System.out.println("\n"+s + ": " + options.get(s).toString());
//	        	}
	            
            } catch (Exception e) {
            	System.out.println("no internet");
            	options.put("all", null);
            }
	    }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        
        return options;
        
    }
    
    /**
     * GenerateDistractors()
     * - Takes the input for distractor extraction and makes the calls to the API in MultipleChoiceOptions
     * 
     * @param answer
     * @return choices ArrayList of distractor choices
     * @throws Exception
     */
    public static ArrayList<String> GenerateDistractors(String answer) throws Exception {
        	
		
    	HashMap<String, ArrayList<String>> options = MultipleChoiceOptions(answer);
    	ArrayList<String> choices = null;
    	
    	if (options.get("all") != null)
    		choices = options.get("all");
    	else
    		choices = new ArrayList<String>();
    	
    	return choices;
    	
    }
    
    
}