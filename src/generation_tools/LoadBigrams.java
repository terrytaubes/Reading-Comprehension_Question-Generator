package generation_tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import generation_models.Bigram;

public class LoadBigrams {
	
	public static HashMap<String, Double> load(String yourPath) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(yourPath+"\\BigramModel\\validator_bigram_prob.txt"));
		
		String readString = null;
		String[] line;
		String[] currentWords;
		Bigram currentBigram;
		Double currentProbability;
		
		HashMap<String, Double> prob = new HashMap<String, Double>();

		while ((readString = reader.readLine()) != null) {
			//System.out.println(readString);
			
			line = readString.split("=");
			currentWords = line[0].split("/");
			
			if (currentWords.length == 2) {
				
				currentBigram = new Bigram(currentWords[0], currentWords[1]);
				currentProbability = Double.parseDouble(line[1]);
				
				prob.put(currentBigram.asString(), currentProbability);
				
			}
			
			//System.out.println(currentBigram.word[0]+", "+currentBigram.word[1]);
			//System.out.println(currentProbability);
			
		}	
		
		reader.close();
		
//		int count = 0;
//		for (String key : prob.keySet()) {
//			//if (key.word[0].equals("same") && key.word[1].equals("qualities")) {
//			//	key.print();
//			//}
//			//key.print();
//			//System.out.println(prob.get(key));
//			count += 1;
//		}
		
		//System.out.println(count + " Bigrams from Brown Corpus + Stanford Q/A Dataset.");
		
		return prob;

	}
	

}
