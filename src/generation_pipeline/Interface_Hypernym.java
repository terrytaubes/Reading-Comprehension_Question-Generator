package generation_pipeline;

import generation_tools.WordNetController;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.list.PointerTargetNodeList;


/**
 * Interface_Hypernym makes use of the Extended Java Wordnet Library in order to extract hypernyms for words.
 * 
 * @author TerranceTaubes
 */
public class Interface_Hypernym {
	
	/**
	 * hypernyms()
	 * - Returns a list of hypernyms for the desired word and part-of-speech
	 * 
	 * @param word
	 * @param pos
	 * @return hypernyms
	 * @throws JWNLException
	 */
	public static PointerTargetNodeList hypernyms(String word, String pos) throws JWNLException {
		
		WordNetController wnc = new WordNetController();
		
		POS realPOS;
        pos = pos.toLowerCase();
        
        if (pos.contains("verb")) realPOS = POS.VERB;
        else if (pos.contains("adverb")) realPOS = POS.ADVERB;
        else if (pos.contains("adjecive")) realPOS = POS.ADJECTIVE;
        else realPOS = POS.NOUN;
        
        IndexWordSet newWord = wnc.dictionary.lookupAllIndexWords(word);
        IndexWord w = newWord.getIndexWord(realPOS);

		
		PointerTargetNodeList hypernyms = wnc.listHypernyms(w);
		
		return hypernyms;
		
	}

	
	
}
