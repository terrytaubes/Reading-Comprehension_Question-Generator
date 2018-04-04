package generation_pipeline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Scanner;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import generation_models.DocumentQuestions;
import generation_models.UserSimple;
import generation_tools.StanfordPipeline;

/**
 * Driver is the MAIN class that will run the generation pipeline while using Eclipse.
 *   
 * @author TerranceTaubes
 * 
 */
public class Driver {
	
	
	public static Scanner scan;
	public static PrintWriter pwQuestion;
	public static PrintWriter jsonWriter;
	public static BufferedReader br;
	
	
	
	public static void main(String[] args) throws Exception {
		
		//Pipeline("Who knows how far? Not me.", "manual", 8, 100);
		//Pipeline("short1", "file", 8, 100);
		Pipeline(null, null, null, null);
	}
	
	/** 
	 * Pipeline()
	 * - Pipeline is called when questions are to be generated.
	 * 
	 * @param passageInput String containing either the entire passage or the name of the passage's file
	 * @param uploadMethod String denoting whether the passageInput is the entire passage or the name of a passage's file
	 * @param gradeLevel Integer denoting desired grade level for generation
	 * @param numberQuestions Integer denoting desired number of questions
	 * @return dq DocumentQuestions structure that contains all generated questions/answers/distractors
	 * @throws Exception
	 */
	public static DocumentQuestions Pipeline(String passageInput, String uploadMethod, Integer gradeLevel, Integer numberQuestions) throws Exception {
		
		if (passageInput != null && uploadMethod != null && gradeLevel != null & numberQuestions != null)
			System.out.println(passageInput+"\n"+uploadMethod+"\n"+gradeLevel+"\n"+numberQuestions);
		
		
		/*===== < Initialize > =====*/
		scan = new Scanner(System.in);
		
		
		/*-- Current Directory --*/
		String currentDirectory = System.getProperty("user.dir");
		System.out.println("Path: "+currentDirectory);

		/*-- Set Output Paths --*/
		
		/* SPECIFY path to directory */
		String yourPath = "C:\\Users\\Terry\\workspace\\stanford_practice";
		/* SPECIFY path to directory */
		
		/* Uncomment PrintWriters to allow output-to-file */
		//pwQuestion = new PrintWriter(new BufferedWriter(new FileWriter(yourPath+"\\Questions\\questionOutput.txt")));
		//jsonWriter = new PrintWriter(new BufferedWriter(new FileWriter(yourPath+"\\json-files\\output.json")));
		
		/*-- Additional Classes/Variables --*/
		UserSimple terry = new UserSimple("1112223333", "Taubes", "Terrance", "korea");
		StanfordCoreNLP pipeline;
		String[] path_text = new String[2];
		
		
		/*===== < Import Passage Text > =====*/
		/* Note: path and passage text stored in String[] path_text
		/* path_text[0] = currentDirectory + \Text\passagename.txt		*/
		/* path_text[1] = (passage text)								*/
		
		path_text = LoadPassage.Read(yourPath+"\\Text\\", uploadMethod, passageInput);
		
		
		/*==== < Build StanfordCoreNLP Pipeline > ====*/
		/* Note: set full to 'true' for normal use, 'false' for limited use */
		
		/* SPECIFY full */
		boolean full = false;
		/* SPECIFY full */
		
		pipeline = StanfordPipeline.BuildStanfordPipeline(full);
		
		
		/* ====================================================== */
		/*  Simplification has been decided to be kept separate.  */
		/*	- code below is outdated but kept for reference		  */
		/* ====================================================== */
		
		/*==== < Simplification > ====*/
		
//		String simplification;
//		SimplePassage passage = null;
//
//		System.out.println("Simplify Passage? [y/n]");	
//		simplification = scan.next();
//		
//		if (simplification.equals("x")) {
//			
//			passage = ParsingController.pipeline(pipeline, difficulty, path_text[0]);
//			//passage = null;
//			
//			System.out.println(passage.analyzed);
//			System.out.println();
//			System.out.println(passage.text);
//		}
		
		
		/* ============================= */
		/*====  Question Generation  ====*/
		/* ============================= */
		
		// Timer
	    java.util.Date date = new java.util.Date();
	    Timestamp timestamp1 = new Timestamp(date.getTime());

	    
		System.out.println("Begin Generation...");
		
		/* DocumentQuestions stores each question and its information in DocumentQuestions.questions */
		DocumentQuestions dq;
				
		
		/* Group 1 of Transformations */
		dq = Generator_Group1.Generate(pipeline, gradeLevel, path_text[1]);
		System.out.println("Group 1 Success");
				
		
		/* Group 2 of Transformations */
		if (full) {
			dq = Generator_Group2.generateQuestions(pipeline, gradeLevel, path_text[1], terry, dq);
			System.out.println("Group 2 Success");
		}
		
		
		/* ============================= */
		/*====  Question Validation  ====*/
		/* ============================= */
		
		/* Note: dq is returned with each question marked valid either T|F */
		dq = Validator.calculateProbabilities(dq, pipeline, yourPath);

	
		
		/*-- Print Questions to Console --*/
		System.out.println(dq.questions.size());
		//dq.printOut();
		
		/*==== < Output to text file > ====*/
		//dq.print(pwQuestion);
		
		/*==== < JSON Output > ====*/
		//JsonObject json = OutputJson.DQtoJSON(dq, jsonWriter);
		//System.out.println(json.toString());
		
		date = new java.util.Date();
		Timestamp timestamp2 = new Timestamp(date.getTime());
		long milliseconds = timestamp2.getTime() - timestamp1.getTime();
	    int seconds = (int) milliseconds / 1000;
	    System.out.println("\nGeneration/Validation Time: " + seconds +" seconds");
		
		/*-- Close Resources --*/
		System.out.println("\nclose resourses");
		scan.close();
		//pwQuestion.close();
		//jsonWriter.close();
				
		return dq;
	}
}
