package generation_tools;

import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.JsonObject;
import generation_models.DocumentQuestions;

/**
 * OutputJson is used to send the document questions to a text file in a json-esque format.
 * 
 * @author TerranceTaubes
 */
public class OutputJson {
	
	/**
	 * DQtoJSON()
	 * - Takes DocumentQuestions as input and prints JSON representation to text file
	 * 
	 * @param dq
	 * @param jsonWriter
	 * @return json - 'JsonObject' of dq's content
	 * @throws IOException
	 */
	public static JsonObject DQtoJSON(DocumentQuestions dq, PrintWriter jsonWriter) throws IOException {
		
		JsonObject json = new JsonObject();
		JsonObject item;
		
		for (int i = 0; i < dq.questions.size(); i++ ) {
			
			item = new JsonObject();
			item.add("q", dq.questions.get(i).question.get("q"));
			item.add("a", dq.questions.get(i).answer.get("a"));
			item.add("d", dq.questions.get(i).distractors.get("d"));
			
			json.add(Integer.toString(i), item);
		}
		
		//System.out.println(json.toString());
		
		jsonWriter.print(json.toString());
		
		return json;
	}
	

}
