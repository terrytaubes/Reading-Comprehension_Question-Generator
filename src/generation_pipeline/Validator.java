package generation_pipeline;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import generation_models.Bigram;
import generation_models.DocumentQuestions;
import generation_models.ProbabilityStructure;
import generation_models.QuestionStructure;
import generation_tools.LoadBigrams;
//import generation_tools.StanfordPipeline;


public class Validator {
	
	
	public static List<List<Bigram>> processDQ(DocumentQuestions dq, StanfordCoreNLP pipeline) {
		
		List<List<Bigram>> QuestionBigrams = new ArrayList<List<Bigram>>();
		
		Annotation document;
		
		document = new Annotation("%");
		pipeline.annotate(document);;
		
	    List<CoreLabel> tokens = document.get(TokensAnnotation.class);
	    
	    CoreLabel padding = tokens.get(0);
	    
	    List<Bigram> bigramList;
	    
		
		for (int b = 0; b < dq.questions.size(); b++) {
			
			//System.out.println(dq.questions.get(b).q.toString());
			
			document = new Annotation(dq.questions.get(b).q);
			
			pipeline.annotate(document);
			
		    tokens = document.get(TokensAnnotation.class);
		    
		    tokens.add(0, padding);
		    tokens.add(padding);
		    
		    bigramList = new ArrayList<Bigram>();
		 	
		 	Bigram currentBigram;
		    
		    for (int i = 0; i < tokens.size()-1; i++) {
		    	
		    	currentBigram = new Bigram(tokens.get(i).originalText().toLowerCase(), tokens.get(i+1).originalText().toLowerCase());		    	
		    	bigramList.add(currentBigram);
		    	
		    		
		    }
		    
		    QuestionBigrams.add(bigramList);
		    
//		    System.out.println("Bigrams:");
//		    for (int j = 0; j < bigramList.size(); j++) {
//				bigramList.get(j).print();
//			}

			
		}
		
		return QuestionBigrams;
		
	}
	
	
	public static DocumentQuestions calculateProbabilities(DocumentQuestions dq, StanfordCoreNLP pipeline, String yourPath) throws IOException {
		
		ArrayList<ProbabilityStructure> probabilities = new ArrayList<ProbabilityStructure>();
		
		//dq.printOut();
		
		// Get ArrayList (node per question) of ArrayLists (node per bigram)
		List<List<Bigram>> QuestionBigrams = processDQ(dq, pipeline);
		
		// Load Bigram Probability Model into HashMap
		HashMap<String, Double> prob = LoadBigrams.load(yourPath);
		
		String question;
		int countedProbability;
		double addedProbability;
		double multiProbability;
		
		
		int count = 0;
		
		ProbabilityStructure ps;
		
		for (List<Bigram> list : QuestionBigrams) {
			
			ps = null;
			
			question = dq.questions.get(count).q;
			countedProbability = 0;
			addedProbability = 0;
			multiProbability = 1;

			
			for (Bigram bigram : list) {
				
				//bigram.print();
				//System.out.println(prob.containsKey(bigram.asString()));
				
				if (prob.containsKey(bigram.asString())) {
					countedProbability += 1;
					addedProbability += prob.get(bigram.asString());
					multiProbability *= prob.get(bigram.asString());
					//System.out.println(countedProbability +" "+ addedProbability +" "+ multiProbability);
				}
	
			}
			
			ps = new ProbabilityStructure(question, countedProbability, addedProbability, multiProbability);
			
			//System.out.println("question: "+count);
			//System.out.println(ps.question);
			//System.out.println("match count: "+ps.count);
			//System.out.println("added prob:  "+ps.added);
			//System.out.println("multi prob:  "+ps.multi);
			
			probabilities.add(ps);
			
			count += 1;
			
		}
		
		int countPass = 0;
		int addedPass = 0;
		int multiPass = 0;
		int invalid = 0;
		boolean[] passCheck = new boolean[3];
		

		for (int i = 0; i < probabilities.size(); i++) {
			
			//System.out.println("[ "+probabilities.get(i).question+" ]");
			//System.out.println(dq.questions.get(i).a);
			
			if (probabilities.get(i).count > 1) {
				//System.out.println(probabilities.get(i).count);
				//System.out.println(probabilities.get(i).question);

				countPass += 1;
				passCheck[0] = true;
			}
			else {
				//System.out.println("fail count test");
				passCheck[0] = false;
			}
			
			if (probabilities.get(i).added > 0.0035) {
				//System.out.println(probabilities.get(i).added);
				//System.out.println(probabilities.get(i).question);

				addedPass += 1;
				passCheck[1] = true;
			}
			else {
				//System.out.println("fail added test");
				passCheck[1] = false;
			}
			
			if (probabilities.get(i).multi < 0.0005) {
				//System.out.println(probabilities.get(i).multi);
				//System.out.println(probabilities.get(i).question);

				multiPass += 1;
				passCheck[2] = true;
			}
			else {
				//System.out.println("fail multi test");
				passCheck[2] = false;
			}
			
			if ((passCheck[0] == false) && (passCheck[1] == false) && (passCheck[2] == false)) {
				dq.questions.get(i).valid = false;
				invalid += 1;
				//System.out.println(dq.questions.get(i).question);
				//System.out.println(dq.questions.get(i).a);


			}
			else {
				dq.questions.get(i).valid = true;
			}

		}
		
		int len = dq.questions.size();
		
		//dq.printOut();
		
		System.out.println("\nValidation Results");
		System.out.println("count - "+countPass+"/"+len);
		System.out.println("added - "+addedPass+"/"+len);
		System.out.println("multi - "+multiPass+"/"+len);
		System.out.println("invalid - "+invalid+"/"+len+" below:");
		
		ArrayList<Integer> remove = new ArrayList<Integer>();
		
		for (int i = 0; i < len; i++) {
			if (dq.questions.get(i).valid == false) {
				System.out.println("-> "+dq.questions.get(i).q);
				remove.add(0, i);
			}
		}
		
		//for (int j = 0; j < remove.size(); j++) {
		//	dq.questions.remove(remove.get(j));
		//}
		
		
		return dq;
	}
	
	// TESTING METHOD
	public static DocumentQuestions populateDQ() {
		
		DocumentQuestions dq = new DocumentQuestions();
		
		ArrayList<String> d = new ArrayList<String>();
		d.add("first");
		d.add("second");
		d.add("third");
		
		String a = "fourth";
						
		String[] questions = new String[6];
		questions[0] = "What is in front of the Notre Dame Main Building?";
		questions[1] = "Where will emergency air be?";
		questions[2] = "When will it happen?";
		questions[3] = "Where did it take place?";
		questions[4] = "Who did this thing?";
		questions[5] = "What exactly happened here?";
		
		QuestionStructure temp;
	
		for (int i = 0; i < 6; i++) {
			temp = new QuestionStructure(questions[i], a, d);
			dq.addQuestions(temp);
		}
		
		return dq;
		
	}
	
//	public static void main(String[] args) throws IOException {
//		
//		DocumentQuestions dq = populateDQ();
//		StanfordCoreNLP pipeline = StanfordPipeline.BuildStanfordPipeline(false);
//
//		dq = calculateProbabilities(dq, pipeline);
//	}

}
