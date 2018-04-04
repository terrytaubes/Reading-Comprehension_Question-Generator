package generation_models;

import java.util.ArrayList;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;


public class QuestionStructure {
	
	
	// Strings for q, a, d
	public String q;
	public String a;
	public ArrayList<String> d;
	
	// Boolean to mark if a generated question is valid
	public boolean valid;
	
	// JsonObjects to be used in creating final JSON file
	public JsonObject question;
	public JsonObject answer;
	public JsonObject distractors;
	
	
	public QuestionStructure(String question, String answer, ArrayList<String> distractors) {
		
		this.q = question;
		this.a = answer;
		this.d = distractors;
		
		this.valid = false;
				
		JsonParser parser = new JsonParser();
		this.question = parser.parse("{\"q\": \""+question+"\"}").getAsJsonObject();
		this.answer = parser.parse("{\"a\": \""+answer+"\"}").getAsJsonObject();

		
		String dists = "";

		for (int i = 0; i < distractors.size(); i++) {
			if (i == distractors.size()-1) {
				dists += distractors.get(i);
			}
			else {
				dists += distractors.get(i)+",";
			}
		}
		
		
		this.distractors = parser.parse("{\"d\": \""+dists+"\"}").getAsJsonObject();
		
	}

}
