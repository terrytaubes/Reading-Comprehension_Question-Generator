package generation_models;

import java.io.PrintWriter;
import java.util.ArrayList;


public class DocumentQuestions {
	
	/*
	 * Interacting Classes:
	 * - QuestionStructure.java
	 * - OutputJson.java
	 */
	
	// List of each question (Question/Answer/Distractors) generated
	public ArrayList<QuestionStructure> questions;
	
	
	public DocumentQuestions() {
		
		questions = new ArrayList<QuestionStructure>();
		
	}
	
	
	public void addQuestions(QuestionStructure q) {
		
		questions.add(q);
		
	}
	
	public void printOut() {
		
		for (int b = 0; b < questions.size(); b++) {
	    	System.out.println("Question "+(b+1));
			System.out.println(questions.get(b).question.toString());
			System.out.println(questions.get(b).answer.toString());
			System.out.println(questions.get(b).distractors.toString());
			System.out.println(questions.get(b).valid+"\n");
		}
		
	}
	
	
	public void print(PrintWriter pw) {
		
		QuestionStructure curr_q;
		
		for (int i = 0; i < questions.size(); i++) {
			curr_q = questions.get(i);
			pw.append(curr_q.question+" {"+curr_q.answer+"}\n"+curr_q.distractors.toString()+"\n\n");
		}
		
	}

}
