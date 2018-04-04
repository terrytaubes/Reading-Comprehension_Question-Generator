package generation_tools;


import java.util.Properties;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * StanfordPipeline builds the StanfordCoreNLP class with the desired NLP tools
 * 
 * @author TerranceTaubes
 */
public class StanfordPipeline {
	
	public StanfordPipeline() { }
	
	/**
	 * BuildStanfordPipeline()
	 * - Builds the StanfordCoreNLP object for natural language processing.
	 * - Setting the parameter full to true builds the StanfordCoreNLP object with the full set of NLP tools
	 *     needed to utilize the transformations in both Generator_Group1 and Generator_Group2.
	 * - Setting full to false allows for only the use of Generator_Group1.
	 * 
	 * @param full - 'boolean' specify the use of the either the full or partial list of properties.
	 * @return pipeline - 'StanfordCoreNLP' instance for NLP.
	 */
	public static StanfordCoreNLP BuildStanfordPipeline(boolean full) {
		/*===== Memory Check =====*/
		
		//System.out.println(Runtime.getRuntime().maxMemory());
		//1427243008
		
		/*===== Create StanfordCoreNLP Pipeline =====*/
		
		Properties props = new Properties();

		if (full) {
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, mention, dcoref");
			props.setProperty("dcoref.postprocessing", "true");
		} else {
			props.setProperty("annotators", "tokenize, ssplit, pos, parse");
		}
		
		// Pipeline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		return pipeline;
	}
		

}
