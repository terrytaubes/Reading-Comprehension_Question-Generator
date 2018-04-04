package generation_pipeline;

import com.google.gson.*;

import generation_models.DocumentQuestions;
import generation_models.QuestionStructure;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The JsonExchange class contains the [getView] and [createQuestions] methods
 * that are extracted from the Generator .jar and are used by the Remind interface to
 * create the generator views and to create the comprehension questions.
 * 
 * @author TerranceTaubes
 */

public class Handshake {
	
	/* Main */
	
    public static void exchange(String[] args) throws Exception{
    
//    	// Start View
//        String json = getView("");
//
//        
//        // Faux user input
//        String fauxJson = getFauxView("");
//        
//        
//        // View Two - Dependent on uploadMethod dropdown user selection
//        //   -> Decides which view to select to upload passage
//        json = getView(fauxJson);
//        
//        fauxJson = getFauxView(json);
//        
//        // View Three - Validation view
//        json = getView(fauxJson);
    	
    	String json = loadFaux();
    	System.out.println(json);
    	
    	List<String> questions = createQuestions(json);
    	
    	int count = 0;
    	for (String q : questions) {
    		System.out.println(count +": "+q);
    		count += 1;
    	}
        
        
    }

    
    /**
     * createQuestions()
     * - Takes the filled out JSON, extracts the generation parameters,
     *     and generates the List<String> of questions, answers, and distractors.
     *     
     * @param json User-filled JSON object
     * @return qList List of Strings, each containing a question and the question parts
     * @throws Exception
     */
    
    public static List<String> createQuestions(String json) throws Exception{
    	
        // Parse the string into a json object.
        JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
                
        //JsonArray viewsDisplayedInOrder = jsonObj.getAsJsonArray("views");
        
        //Take the parameters
    	String passageInput = jsonObj.getAsJsonObject("upload").getAsJsonObject("parameterInfo").getAsJsonObject("passageText").get("value").toString().replaceAll("\"", "");
    	
        String methodValue = jsonObj.getAsJsonObject("start").getAsJsonObject("parameterInfo").getAsJsonObject("uploadMethod").get("value").toString().replaceAll("\"", "");

        int gradeLevel = jsonObj.getAsJsonObject("upload").getAsJsonObject("parameterInfo").getAsJsonObject("gradeLevel").get("value").getAsInt();
        
        int numberQuestions = jsonObj.getAsJsonObject("upload").getAsJsonObject("parameterInfo").getAsJsonObject("numberQuestions").get("value").getAsInt();        


        List<String> qList = new ArrayList<String>();
        
        DocumentQuestions dq = Driver.Pipeline(passageInput, methodValue, gradeLevel, numberQuestions);
        
        if (dq.questions.size() < numberQuestions)
        	numberQuestions = dq.questions.size();

        for(int i = 0; i < numberQuestions; i++){
                        
        	String question = createQuestion(dq.questions.get(i));
            qList.add(question);
            
        }
        return qList;
    }

    
    /**
     * createQuestion()
     * - Takes in a QuestionStructure, transforms the values into JSON format as a string.
     * @param qs QuestionStructure
     * @return question String of Question/Answer/Distractors
     */
    public static String createQuestion(QuestionStructure qs){
      //In here I could use template selected to create better questions. Lets say I am creating a question based on isSub, isAdd, and isMultiplication.
      String questionPartPrompt; //This is the actual question prompt
      JsonObject correctChoice, falseChoice, falseChoice2, falseChoice3;
      /*
      * Each question can have multiple question parts.
      * Each questionPart has a prompt and choices.
      * A choice can be correct or wrong, and have a type.
      *   choice types currently available can be found in our documentation, but "text" is the main one in use.
      */
      questionPartPrompt = qs.q;
      correctChoice = createChoice("text", true, qs.a);
      
      if (qs.d.size() == 0) {
    	  falseChoice = createChoice("text", false, "");
          falseChoice2 = createChoice("text", false, "");
          falseChoice3 = createChoice("text", false, "");
      } else {
	      falseChoice = createChoice("text", false, qs.d.get(0));
	      falseChoice2 = createChoice("text", false, qs.d.get(1));
	      falseChoice3 = createChoice("text", false, qs.d.get(2));
      }
      
      // Create an array of our choices
      JsonArray choices = new JsonArray();
      choices.add(correctChoice);
      choices.add(falseChoice);
      choices.add(falseChoice2);
      choices.add(falseChoice3);
      
      // We create our questionPart
      JsonObject questionPart = createQuestionPart(questionPartPrompt, 0, choices);
      
      // Add the questionPart to the questionPartsArray.
      // We could have more questionParts for this question.
      JsonArray questionParts = new JsonArray();
      questionParts.add(questionPart);

      // Now we create the question object
      JsonObject question = new JsonObject();
      question.addProperty("format", "Multiple Choice");
      question.addProperty("prompt", "What is the correct answer?");
      question.add("parts", questionParts);

      return question.toString();
    }

    
    /**
     * createChoice()
     * - Creates a Choice for a Question (three choices per question)
     * 
     * @param style Style of question (ex. "text")
     * @param isCorrect Correct Value or Not (T/F)
     * @param value Value of Choice (Answer/Distractor)
     * @return choice JsonObject of Choice
     */
    public static JsonObject createChoice(String style, Boolean isCorrect, String value){
      JsonObject choice = new JsonObject();
      choice.addProperty("style", style);
      choice.addProperty("isCorrect", isCorrect);
      choice.addProperty("value", value);
      return choice;
    }
    
    /**
     * createQuestionPart()
     * - Creates a question using the prompt, order index and choices
     * 
     * @param prompt
     * @param orderIndex
     * @param choices
     * @return questionPart Final Question Representation
     */
    public static JsonObject createQuestionPart(String prompt, int orderIndex, JsonArray choices){
      JsonObject questionPart = new JsonObject();
      questionPart.addProperty("prompt", prompt);
      questionPart.addProperty("orderIndex", orderIndex);
      questionPart.add("choices",choices);
      return questionPart;
    }

    
    /**
     * createAndAddParameterToView()
     * - Method used to add parameters to a view
     * 
     * @param viewBeingConstructed
     * @param name
     * @param guiStyle
     * @param guiParameters
     * @param title
     * @param subtitle
     * @return viewBeingConstructed Json of Current View being constructed
     * 
     * @author AndersonThomas
     */
    public static JsonObject createAndAddParameterToView(JsonObject viewBeingConstructed, String name, String guiStyle, String guiParameters, String title, String subtitle){

        // Formatted to illustrate the construction of JSON.
        String hasParameters = guiParameters.length() > 0 ? "', " : "'";
        String result = "{";
            result += "'gui': {";
                result += "'style': '" + guiStyle + hasParameters;
                result += guiParameters;
            result += "},";
            result += "'title': '"+ title +"',";
            result += "'subtitle': '"+ subtitle + "'";
        result += "}";
        result = result.replace("'", "\"");

        //Add the parameter info to the parameter info object.
        JsonObject parameter = new JsonParser().parse(result).getAsJsonObject();
        JsonObject parameterInfo;

        // Check if parameter info is defined.
        parameterInfo = viewBeingConstructed.has("parameterInfo") ? viewBeingConstructed.getAsJsonObject("parameterInfo") : new JsonObject();
        parameterInfo.add(name, parameter);
        viewBeingConstructed.add("parameterInfo", parameterInfo);

        // Check if parameterKeys are defined.
        JsonArray parameterKeys = viewBeingConstructed.has("parameterKeys") ? viewBeingConstructed.getAsJsonArray("parameterKeys") : new JsonArray();
        
        parameterKeys.add(name);
        viewBeingConstructed.add("parameterKeys", parameterKeys);

        return viewBeingConstructed;

    }

    
    /**
     * getView()  [ REVIEW ]
     * - getView is called each time a new view is to be generated and displayed to the user.
     *     
     * @param previousViews
     * @return result String of JsonObject of next View
     * @throws Exception
     */
    public static String getView(String previousViews) throws Exception{
    	
        // We need to construct a jsonObject for our next view.
        JsonObject nextViewToBeDisplayed = new JsonObject();

        // Track some flags as to not step over ourselves.
        Boolean creatingView = true;
        
        JsonObject jsonObj = null;
        
        try {
        	
        	jsonObj = new JsonParser().parse(previousViews).getAsJsonObject();
        	
        	System.out.println(jsonObj.getAsString());
        	
        	System.out.println(jsonObj.has("manual"));
        	
        } catch (Exception e) {
        	
        	System.out.println("start");
        	
        }
        
        
        JsonArray viewsDisplayedInOrder = new JsonArray();
    
        String viewName;
        String guiParam;
        String subtitle;


        if (jsonObj == null) {
        	        	
        	System.out.println("check 1: json is null");
        	
            //jsonObj.remove("isStart");
        	jsonObj = new JsonObject();
            viewsDisplayedInOrder = new JsonArray();
            
//          JsonObject viewJson = new JsonParser().parse("").getAsJsonObject();
            viewsDisplayedInOrder.add("start");
            viewName = "start";
            
            /*
             * manual upload
             * file upload
             * library select
             */
            guiParam = "'list': [{'value':'manual', 'title':'Manual Upload'},{'value':'file', 'title':'File Upload'}, {'value': 'library', 'title':'Library Select'}]";
            subtitle = "Select method to upload passage.";
            
            
            //(JsonObject viewBeingConstructed, String name, String guiStyle, String guiParameters, String title, String subtitle)
            
//			JsonObject createAndAddParameterToView(JsonObject viewBeingConstructed, String name, String guiStyle, String guiParameters, String title, String subtitle){

            /* JSON was empty, no views
             * -> added "start"
             * Add gui parameter to nextViewToBeDisplayed (viewBeingConstructed) which is returned from createAndAddParamterToView
             */
            
            nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "uploadMethod", "dropdown", guiParam, "Select Text", subtitle);
            
       
        } else if (jsonObj.has("start")) {
        	
        	System.out.println("entered: start");
        	
        	viewName = "upload";
        	
            viewsDisplayedInOrder = jsonObj.getAsJsonArray("views");
            System.out.println("views: "+viewsDisplayedInOrder.toString());
            
            String lastView = viewsDisplayedInOrder.get(viewsDisplayedInOrder.size() - 1).getAsString();
            
            if (lastView.equals("start")) {
                JsonObject viewObj = jsonObj.getAsJsonObject(lastView);
                
                //System.out.println(viewObj == null);
                //System.out.println(viewObj.toString());

                JsonElement methodValue = viewObj.getAsJsonObject("parameterInfo").getAsJsonObject("uploadMethod").getAsJsonObject("gui").get("value");  // Since we had a list of checkboxes we can expect a jsonObject with a jsonArray called values.                
                
                String nextView = methodValue.getAsString();
                viewsDisplayedInOrder.add(viewName);
                
                System.out.println("parsed passageMethod: " + viewName);
                
                if (nextView.equals("manual")) {
                	
                	System.out.println("-> in manual");
                
	                guiParam = "";
	                subtitle = "Copy and paste passage into the text input.";

	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "passageText", "inputText", guiParam, "Enter Passage", subtitle);

	                guiParam = "'min':4, 'max':8";
	                subtitle = "Select desired grade level.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "gradeLevel", "inputInteger", guiParam, "Grade Level", subtitle);
	                
	                guiParam = "min':1, 'max':100";
	                subtitle = "Select desired number of questions.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "numberQuestions", "inputInteger", guiParam, "Number of Questions", subtitle);

                }
                
                else if (nextView.equals("file")) {
                	guiParam = "";
	                subtitle = "Enter path to passage in local directory.";

	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "filePath", "inputText", guiParam, "File Path", subtitle);

	                guiParam = "'min':4, 'max':8";
	                subtitle = "Select desired grade level.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "gradeLevel", "inputInteger", guiParam, "Grade Level", subtitle);
	                
	                guiParam = "min':1, 'max':100";
	                subtitle = "Select desired number of questions.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "numberQuestions", "inputInteger", guiParam, "Number of Questions", subtitle);

                	
                }
                
                else if (nextView.equals("library")) {
                	guiParam = "";
	                subtitle = "Enter path to passage in local directory.";

	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "filePath", "inputText", guiParam, "File Path", subtitle);

	                guiParam = "'min':4, 'max':8";
	                subtitle = "Select desired grade level.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "gradeLevel", "inputInteger", guiParam, "Grade Level", subtitle);
	                
	                guiParam = "'min':1, 'max':100";
	                subtitle = "Select desired number of questions.";
	                
	                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "numberQuestions", "inputInteger", guiParam, "Number of Questions", subtitle);

                	
                }
                
            }
//            else if (lastView.equals("upload")) {
//            	
//            	
//            	System.out.println("-> entered firstView");
//            	
//            	// validation view"
//            	viewName = "validation";
//            	
//                JsonObject viewObj = jsonObj.getAsJsonObject(lastView);
//                JsonObject startObj = jsonObj.getAsJsonObject("start");
//                
//                String methodValue = startObj.getAsJsonObject("parameterInfo").getAsJsonObject("uploadMethod").getAsJsonObject("gui").get("value").toString();  // Since we had a list of checkboxes we can expect a jsonObject with a jsonArray called values.
//                //System.out.println("method value "+methodValue);
//                
//            	String passageInput = viewObj.getAsJsonObject("parameterInfo").getAsJsonObject("passageText").getAsJsonObject("gui").get("value").getAsString();  // Since we had a list of checkboxes we can expect a jsonObject with a jsonArray called values.
//                Integer gradeLevel = viewObj.getAsJsonObject("parameterInfo").getAsJsonObject("gradeLevel").getAsJsonObject("gui").get("value").getAsInt();
//            	Integer numberQuestions = viewObj.getAsJsonObject("parameterInfo").getAsJsonObject("numberQuestions").getAsJsonObject("gui").get("value").getAsInt();
//            	
//            	//DocumentQuestions dq = Driver.Pipeline(passageInput, methodValue, gradeLevel, numberQuestions);
//            	
//            
//            }
            
        	
        } else {
        	
        	// OLD CODE
        	
            // Parse the string into a json object
            viewName = "secondView";

            // Take the last view provided
            viewsDisplayedInOrder = jsonObj.getAsJsonArray("views");
            String lastView = viewsDisplayedInOrder.get(viewsDisplayedInOrder.size() - 1).getAsString();

            if (lastView.equals("start")) {
                JsonObject viewObj = jsonObj.getAsJsonObject(lastView);
                JsonArray topics = viewObj.getAsJsonObject("topics").getAsJsonArray("values");  // Since we had a list of checkboxes we can expect a jsonObject with a jsonArray called values.
                Boolean isSubOrAdd = false;
                for (JsonElement topic : topics) {
                    System.out.print(topic.getAsString() + " ");
                    if (topic.getAsString().equals("subtraction") || topic.getAsString().equals("addition")) {
                        isSubOrAdd = true;
                    }
                }
                System.out.println("");

                if (isSubOrAdd) {
                  String placeHolderPrompt = "{'value': 'disabled', 'title':'Please select a template.'}";
                  String template1 = "{'value': 'addTemp1', 'title': 'Given you have $x apples but ninjas take $y, how many do you need to find to have $x?'}";
                  String template2 = "{'value': 'addTemp2', 'title': '$Alice started the day with $5 pencils. Mark gave Alice $Y pencils that he owed her, how many pencils does alice have now?'}";
                  String template3 = "{'value': 'addTemp3', 'title': 'What is $x + $y?'}";
                  String guiParameters = "'list': [" + placeHolderPrompt + "," + template1 + "," + template2 + "," + template3 + "]";
                  String title = "Addition Subtraction Templates";
                  subtitle = "Please select a template from which to generate questions.";
                  nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "templateSelected", "dropdown", guiParameters, title, subtitle);

                } else {
                    // We want to display a set of templates.
                    String placeHolderPrompt = "{'value': 'disabled', 'title':'Please select a template.'}";
                    String template1 = "{'value': 'multTemp1', 'title':'Given $x apples are produced by a single tree, and you have 15 trees. How much fruit can you produce?'}";
                    String template2 = "{'value': 'multTemp1', 'title':'$Alice started the day with $5 pencils. A single $ninja can steal a pencil per hour, there are 2 $ninja's stealing from $Alice now. How many hours until $Alice has $1 pencil left?'}";
                    String template3 = "{'value': 'multTemp1', 'title':'If a train is travelling 90 $km/hr and there is a penny spaced every $km on the track, how many pennies could the train flatten in 1 hour?'}";
                    String guiParameters = "'list': [" + placeHolderPrompt + "," + template1 + "," + template2 + "," + template3 + "]";
                    String title = "Multiplication Template";
                    subtitle = "Please select a template from which to generate questions.";
                    nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "templateSelected", "dropdown", guiParameters, title, subtitle);
                }
                viewsDisplayedInOrder.add("secondView");
            } else {
                //We only want to display two views, the start view and then a follow up question view.
                creatingView = false;
            }

        }
        
        if (!creatingView) {
            //Set the flag that this series of views is done.
            // Setting the isFinished flag will cause this view Not to be displayed.
            jsonObj.addProperty("isFinished", true);
            
        } else {
        	
            // We add the next view to previous views, we also need to add its key value to the views array.
            jsonObj.add("views", viewsDisplayedInOrder);
            jsonObj.add(viewName, nextViewToBeDisplayed);
        }
        
        String result = jsonObj.toString();
        System.out.println("The json constructed is: ");
        System.out.println(result);
        return result;
    }
    
    
   // TESTING METHOD
   public static String getFauxView(String previousViews){
    	
        // We need to construct a jsonObject for our next view.
        JsonObject nextViewToBeDisplayed = new JsonObject();

        // Track some flags as to not step over ourselves.
        Boolean creatingView = true;
        
        JsonObject jsonObj = null;
        
        try {
        	
        	jsonObj = new JsonParser().parse(previousViews).getAsJsonObject();
        	
        } catch (Exception e) {
        	
        	//System.out.println("start");
        	
        }
        
        
        JsonArray viewsDisplayedInOrder = new JsonArray();
    
        String viewName;
        String guiParam;
        String subtitle;

        if (jsonObj == null) {
        	        	
        	
            //jsonObj.remove("isStart");
        	jsonObj = new JsonObject();
            viewsDisplayedInOrder = new JsonArray();
            
//          JsonObject viewJson = new JsonParser().parse("").getAsJsonObject();
            viewsDisplayedInOrder.add("start");
            viewName = "start";
            
            /*
             * manual upload
             * file upload
             * library select
             */
            
            //guiParam = "'value': 'manual'";
            guiParam = "'value': 'file'";
            subtitle = "";
            
            
            //(JsonObject viewBeingConstructed, String name, String guiStyle, String guiParameters, String title, String subtitle)
            
//			JsonObject createAndAddParameterToView(JsonObject viewBeingConstructed, String name, String guiStyle, String guiParameters, String title, String subtitle){

            /* JSON was empty, no views
             * -> added "start"
             * Add gui parameter to nextViewToBeDisplayed (viewBeingConstructed) which is returned from createAndAddParamterToView
             */
            
            nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "uploadMethod", "dropdown", guiParam, "Select Text", subtitle);
            
//            subtitle = "Please input your difficulty range";
//            guiParam = "'min':1, 'max':100";
//            nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "difficulty", "range", guiParam, "Difficulty", subtitle);
//            subtitle = "How many questions would you like to generate?";
//            // Gui param is the same.
//            nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "questionCount", "inputInteger", guiParam, "Question Count", subtitle);
        
        
            
        } else if (jsonObj.has("start")) {
        	
        	//System.out.println("entered: start");
        	
        	viewName = "upload";
        	
            viewsDisplayedInOrder = jsonObj.getAsJsonArray("views");
            //System.out.println("views: "+viewsDisplayedInOrder.toString());
            
            String lastView = viewsDisplayedInOrder.get(viewsDisplayedInOrder.size() - 1).getAsString();
            
            if (lastView.equals("upload")) {
                JsonObject viewObj = jsonObj.getAsJsonObject(lastView);
                                
                //guiParam = "'value':'Atari decided to skip testing due to time limitations.'";
                guiParam = "'value':'short1'";
                subtitle = "";

                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "passageText", "inputText", guiParam, "Enter Passage", subtitle);

                guiParam = "'value':5";
                subtitle = "";
                
                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "gradeLevel", "inputInteger", guiParam, "Grade Level", subtitle);
                
                guiParam = "'value':50";
                subtitle = "";
                
                nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "numberQuestions", "inputInteger", guiParam, "Number of Questions", subtitle);
                
            }            
        
        	
        } else {
            // Parse the string into a json object
            viewName = "secondView";

            // Take the last view provided
            viewsDisplayedInOrder = jsonObj.getAsJsonArray("views");
            String lastView = viewsDisplayedInOrder.get(viewsDisplayedInOrder.size() - 1).getAsString();

            if (lastView.equals("start")) {
                JsonObject viewObj = jsonObj.getAsJsonObject(lastView);
                JsonArray topics = viewObj.getAsJsonObject("topics").getAsJsonArray("values");  // Since we had a list of checkboxes we can expect a jsonObject with a jsonArray called values.
                Boolean isSubOrAdd = false;
                for (JsonElement topic : topics) {
                    System.out.print(topic.getAsString() + " ");
                    if (topic.getAsString().equals("subtraction") || topic.getAsString().equals("addition")) {
                        isSubOrAdd = true;
                    }
                }
                System.out.println("");

                if (isSubOrAdd) {
                  String placeHolderPrompt = "{'value': 'disabled', 'title':'Please select a template.'}";
                  String template1 = "{'value': 'addTemp1', 'title': 'Given you have $x apples but ninjas take $y, how many do you need to find to have $x?'}";
                  String template2 = "{'value': 'addTemp2', 'title': '$Alice started the day with $5 pencils. Mark gave Alice $Y pencils that he owed her, how many pencils does alice have now?'}";
                  String template3 = "{'value': 'addTemp3', 'title': 'What is $x + $y?'}";
                  String guiParameters = "'list': [" + placeHolderPrompt + "," + template1 + "," + template2 + "," + template3 + "]";
                  String title = "Addition Subtraction Templates";
                  subtitle = "Please select a template from which to generate questions.";
                  nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "templateSelected", "dropdown", guiParameters, title, subtitle);

                } else {
                    // We want to display a set of templates.
                    String placeHolderPrompt = "{'value': 'disabled', 'title':'Please select a template.'}";
                    String template1 = "{'value': 'multTemp1', 'title':'Given $x apples are produced by a single tree, and you have 15 trees. How much fruit can you produce?'}";
                    String template2 = "{'value': 'multTemp1', 'title':'$Alice started the day with $5 pencils. A single $ninja can steal a pencil per hour, there are 2 $ninja's stealing from $Alice now. How many hours until $Alice has $1 pencil left?'}";
                    String template3 = "{'value': 'multTemp1', 'title':'If a train is travelling 90 $km/hr and there is a penny spaced every $km on the track, how many pennies could the train flatten in 1 hour?'}";
                    String guiParameters = "'list': [" + placeHolderPrompt + "," + template1 + "," + template2 + "," + template3 + "]";
                    String title = "Multiplication Template";
                    subtitle = "Please select a template from which to generate questions.";
                    nextViewToBeDisplayed = createAndAddParameterToView(nextViewToBeDisplayed, "templateSelected", "dropdown", guiParameters, title, subtitle);
                }
                viewsDisplayedInOrder.add("secondView");
            } else {
                //We only want to display two views, the start view and then a follow up question view.
                creatingView = false;
            }

        }
        
        if (!creatingView) {
            //Set the flag that this series of views is done.
            // Setting the isFinished flag will cause this view Not to be displayed.
            jsonObj.addProperty("isFinished", true);
            
        } else {
        	
            // We add the next view to previous views, we also need to add its key value to the views array.
            jsonObj.add("views", viewsDisplayedInOrder);
            jsonObj.add(viewName, nextViewToBeDisplayed);
        }
        String result = jsonObj.toString();
        System.out.println("The json constructed is: ");
        System.out.println(result);
        return result;
    }
   
   // TESTING METHOD
   public static String loadFaux() throws IOException {
	   String currentDirectory = System.getProperty("user.dir");

	   BufferedReader reader = new BufferedReader(new FileReader(currentDirectory+"\\json-files\\json.txt"));
		
		String json = "";
		String readString = null;
		
		while ((readString = reader.readLine()) != null) {
			json += readString+" ";
		}		
		
		return json;
   }
   
}