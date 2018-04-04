package generation_tools;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.data.list.*;
import net.sf.extjwnl.dictionary.Dictionary;
import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.Person;
import simplenlg.features.Tense;
import simplenlg.framework.InflectedWordElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.WordElement;
import simplenlg.lexicon.XMLLexicon;
import simplenlg.realiser.english.Realiser;

import java.util.HashSet;

/**
 * WordNetController
 * 
 * @author TerranceTaubes, BenHess
 *
 */
public class WordNetController {

    private DatamuseController dc;
	

    public WordNetController() throws JWNLException {
        this.dictionary = Dictionary.getDefaultResourceInstance();
    }
    
    public final Dictionary dictionary;
    
    //need to pass synset int index as parameter and append to getDirectHypernyms
    public PointerTargetNodeList listHypernyms(IndexWord word) throws JWNLException {
    	try {
	        PointerTargetNodeList hypernyms = PointerUtils.getDirectHypernyms(word.getSenses().get(0));
	        //System.out.println("Direct hypernyms of \"" + word.getLemma() + "\":");
	        //hypernyms.print();
	        return hypernyms;
    	}
    	catch (Exception e) {
    		System.out.print(e);
    	}
    	return null;
    }
    
    
    public void listHyponyms(IndexWord word) throws JWNLException {
        PointerTargetTree hyponyms = PointerUtils.getHyponymTree(word.getSenses().get(0));
        System.out.println("Hyponyms of \"" + word.getLemma() + "\":");
        hyponyms.print();
    }

// All conversion methods based on - http://stackoverflow.com/questions/33389184/simplenlg-how-to-get-the-plural-of-a-noun

    final XMLLexicon xmlLexicon = new XMLLexicon();
    final Realiser realiser = new Realiser(xmlLexicon);

    public String convertToPP(String word) {
        WordElement wrd = xmlLexicon.getWord(word, LexicalCategory.VERB);
        InflectedWordElement verb = new InflectedWordElement(wrd);
        verb.setFeature(Feature.FORM, Form.PAST_PARTICIPLE);
        return realiser.realise(verb).toString();
    }

    public String convertToGerund(String word) {
        WordElement wrd = xmlLexicon.getWord(word, LexicalCategory.VERB);
        InflectedWordElement verb = new InflectedWordElement(wrd);
        verb.setFeature(Feature.FORM, Form.PRESENT_PARTICIPLE);
        return realiser.realise(verb).toString();
    }

    public String convertToPast(String word) {
        WordElement wrd = xmlLexicon.getWord(word, LexicalCategory.VERB);
        InflectedWordElement verb = new InflectedWordElement(wrd);
        verb.setFeature(Feature.TENSE, Tense.PAST);
        return realiser.realise(verb).toString();
    }

    public String convertToThird(String word) {
        WordElement wrd = xmlLexicon.getWord(word, LexicalCategory.VERB);
        InflectedWordElement verb = new InflectedWordElement(wrd);
        verb.setFeature(Feature.PERSON, Person.THIRD);
        return realiser.realise(verb).toString();
    }

    public String compoundWordResolution(String w){
        if(w.contains(" ")){
            w = w.split(" ")[0];
        }
        return w;

    }

    public String realizeVerb(String w, String pos){
        pos = pos.toLowerCase();
        String correct;
        if(pos.contains("present participle")){
            correct = convertToGerund(w);
        } else if(pos.contains("past participle")){
            correct = convertToPP(w);
        } else if(pos.contains("past tense")){
            correct = convertToPast(w);
        } else if(pos.equals("3rd person Singular Present")){
            correct = convertToThird(w);
        } else{
            correct = w;
        }

        System.out.println(correct);
        return correct;
    }

    public String realizeNoun(String w, String pos){
        pos = pos.toLowerCase();
        String correct = w;
        if(pos.contains("plural")) {
            WordElement word = xmlLexicon.getWord(w, LexicalCategory.NOUN);
            InflectedWordElement pluralWord = new InflectedWordElement(word);
            pluralWord.setPlural(true);
            correct = realiser.realiseSentence(pluralWord).toString();
        }
        return correct;
    }


    public HashSet<String> synonymnLookup(String word, String pos) throws JWNLException {

		// https://code.google.com/p/simplenlg/wiki/Section6
        //http://stackoverflow.com/questions/9520501/how-do-you-get-the-past-tense-of-a-verb

		if(dc == null){
			dc = new DatamuseController();
		}
	
		POS realPOS;
        pos = pos.toLowerCase();

        if (pos.contains("verb")) realPOS = POS.VERB;
        else if (pos.contains("adverb")) realPOS = POS.ADVERB;
        else if (pos.contains("adjecive")) realPOS = POS.ADJECTIVE;
        else realPOS = POS.NOUN;

        IndexWordSet newWord = this.dictionary.lookupAllIndexWords(word);

        HashSet<String> syn = new HashSet<String>();

        IndexWord w = newWord.getIndexWord(realPOS);

        if (w != null) {
            for (long offset : w.getSynsetOffsets()) {
                for (Word wordNetWord : dictionary.getSynsetAt(realPOS, offset).getWords()) {
                    String str = compoundWordResolution(wordNetWord.getLemma());

                    //ADDED IF STATEMENT TO ONLY ADD TO SYN SET IF SYN HAS LESS SYLLABLES, BASED OFF CURRENT HARDWORD PARAM
                    if(dc.syllableCount(str)<dc.syllableCount(word)){
                    if(realPOS == POS.VERB) syn.add(realizeVerb(str, pos));
                    else if(realPOS == POS.NOUN){
                       syn.add(realizeNoun(str,pos));
                    }
                    else{
                        syn.add(wordNetWord.getLemma());
                    }
                    }
                }
            }
        }
            if(syn.size()==0){
            	PointerTargetNodeList hypernyms = PointerUtils.getDirectHypernyms(w.getSenses().get(0));
//            	String first = hypernyms.get(0).toString();
            	for(int i=0; i<hypernyms.size(); i++){
            		syn.add(hypernyms.get(i).toString());
            	}
            }
            return syn;
    }


}
