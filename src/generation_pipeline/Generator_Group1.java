package generation_pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import generation_models.DocumentQuestions;
import generation_models.QuestionStructure;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;

//-Xms512m -Xmx1408m

/**
 * Generator_Group1 is the first grouping of comprehension question transformations
 *   applied to the passage. The question generating algorithms are divided into two
 *   groups because Group2 requires a larger Java heap space than what is available
 *   on my local computer, and therefore is disabled when necessary.
 *   
 * @author TerranceTaubes
 *
 */
@SuppressWarnings("deprecation")
public class Generator_Group1 {
	
	
	public Generator_Group1() {	}
	
	/**
	 * Generate()
	 * - Main generation method, calls each of the four transformation methods.
	 * 
	 * @param pipeline
	 * @param gradeLevel
	 * @param passage
	 * @return
	 * @throws Exception
	 */
	public static DocumentQuestions Generate(StanfordCoreNLP pipeline, Integer gradeLevel, String passage) throws Exception {
		
	    DocumentQuestions dq = new DocumentQuestions();
	    
		Annotation document = new Annotation(passage);
				
		pipeline.annotate(document);
		
		    
		/*===== Question Generation =====*/
			
		/* List of Sentences */
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			    		    
		dq = transformation_1(sentences, dq);
			    
		dq = transformation_2(sentences, dq);
		 
		dq = transformation_3(sentences, dq);

		dq = transformation_4(sentences, dq);

		
	    return dq;
	}
	
	
	/**
	 * transformation_1()
	 * - The simplest of transformations, generates maximum # of sensical Fill-in-the-Blank questions.
	 * 
	 * @param sentences
	 * @param questions
	 * @return questions - questions now includes the newly generated questions
	 * @throws Exception
	 */
	public static DocumentQuestions transformation_1(List<CoreMap> sentences, DocumentQuestions dq) throws Exception {
		// Pattern:		beforeTerm  term  afterTerm
		// Question:	beforeTerm  ____  afterTerm
		// Answer:		term
		
		SemanticGraph dependencies;
		
		IndexedWord root;
		String rootWord;
		String[] words;
		List<String> wordList;
		String word;
		ArrayList<String> savedWords;
		int idx;
		List<Pair<GrammaticalRelation, IndexedWord>> children;
		
		
		QuestionStructure q;
		String curr_question;
		String curr_answer;
		ArrayList<String> curr_distractors;

		
		int i;
		
		/* Question generation process repeated for each sentence */
		for (CoreMap sentence : sentences) {
			
			/* Current Sentence */
			//System.out.println("S: " + sentence.toString());
			
			
		    /*  ANSWER WORDS  */
			savedWords = new ArrayList<String>();
			
			i = 0;
						
			/* Creates Array for words in current sentence */
			words = new String[sentence.get(TokensAnnotation.class).size()];
			
			/* Fill Array with words in current sentence */
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				word = token.get(TextAnnotation.class);
		        words[i] = word;
		        i += 1;
			}
			
			/* ArrayList of words in current sentence, used for finding indexes of words */
			wordList = Arrays.asList(words);
			
			/* Dependencies Graph */
			dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		    //System.out.println("dependency graph:\n" + dependencies);
		    //System.out.println(dependencies.getClass().getName());
		    
			
			/* Find Root, automatically used for question generation */
		    root = dependencies.getFirstRoot();
		    rootWord = dependencies.getFirstRoot().originalText();
		    //System.out.println("Root: " + rootWord);
		    savedWords.add(rootWord);
		    
		    //System.out.println(root.getClass().getName());
		    
		    /* Children of Root, used to find words for questions */
		    children = dependencies.childPairs(root);
		    
		    
		    /* Find Words in 'children' with GrammaticalRelations that can be used for questions */
		    for (Pair<GrammaticalRelation, IndexedWord> pair : children) {
		    	//System.out.println(pair.first.getShortName() + ", " + pair.second);

		    	// nsubj	NOMINAL_SUBJECT
				// subj		SUBJECT
				// dobj		DIRECT_OBJECT
				// obj		OBJECT
				// iobj		INDIRECT_OBJECT
				// csubj	CLAUSAL_SUBJECT
				// csubjpass CLAUSAL_PASSIVE_SUBJECT
				// nsubjpass NOMINAL_PASSIVE_SUBJECT
				// pobj		PREPOSITIONAL_OBJECT
				// xcomp	XCLAUSAL_COMPLEMENT
				// mod		MODIFIER
				// rcmod	RELATIVE_CLAUSE_MODIFIER
				// possessive POSSESSIVE_MODIFIER
				// agent	AGENT
		    	
		    	/* Search for Answers */
		    	if (pair.first.getShortName() == "nsubj" || pair.first.getShortName() == "subj" ||
		    		pair.first.getShortName() == "dobj"  || pair.first.getShortName() == "obj"  ||
		    		pair.first.getShortName() == "iobj"  || pair.first.getShortName() == "csubj" ||
		    		pair.first.getShortName() == "csubjpass" || pair.first.getShortName() == "pobj" ||
		    		pair.first.getShortName() == "nsubjpass" || pair.first.getShortName() == "xcomp" ||
		    		pair.first.getShortName() == "rcmod" || pair.first.getShortName() == "mod" ||
		    		pair.first.getShortName() == "nmod"  || 
		    		pair.first.getShortName() == "possessive" || pair.first.getShortName() == "agent") {
		    		//System.out.println("Answer Found");
			    	//System.out.println(pair.first.getShortName() + ", " + pair.second.originalText());
		    		savedWords.add(pair.second.originalText());
		    	}
		    	
		    }
		    
		    /*  Create fill-in-the-blanks for each saved word  */
		    for (int j = 0; j < savedWords.size(); j++) {
		    	curr_answer = savedWords.get(j);
		    	//System.out.println(curr_answer);
		    	
		    	idx = wordList.indexOf(curr_answer);
		    	//System.out.println(idx);
		    	words[idx] = "________";		    	
		    	
		    	curr_question = ""; 
		    	
		    	/* Current Question */
		    	for (int k = 0; k < words.length; k++) {
		    		if (k >= words.length-2) {
		    			//pw.append(words[k]);
		    			curr_question += words[k];
		    		} else {
		    			//pw.append(words[k] + " ");
		    			curr_question += words[k] + " ";
		    		}
		    	}		    	
		    	
		    	/* Distractors */
		    	curr_distractors = Interface_MultipleChoice.GenerateDistractors(curr_answer);
		    	
		    	
		    	q = new QuestionStructure(curr_question, curr_answer, curr_distractors);
		    	
		    	// Transformation 1 questions
		    	dq.addQuestions(q);
		    	
		    	
		    	/* Reset blank in words to actual answer */
		    	words[idx] = curr_answer;
		    	
		    }	
		}
		return dq;
	
	
		
	}
	
	
	/**
	 * transformation_2()
	 * - Generates a question following the form "Which hypernym(subject) verb object?"
	 * 
	 * @param sentences
	 * @param questions
	 * @return questions - questions now includes newly generated questions
	 * @throws Exception
	 */
	public static DocumentQuestions transformation_2(List<CoreMap> sentences, DocumentQuestions questions) throws Exception {
		// Pattern:		[NP subject] [VP verb] [NP object]
		// Question:	Which hypernym(subject) verb object?
		// Answer:		subject
		
		SemanticGraph dependencies;
		
		Pair<GrammaticalRelation, IndexedWord> subjNP;
		Pair<GrammaticalRelation, IndexedWord> objNP;
		IndexedWord verbVP;
		
		List<Pair<GrammaticalRelation, IndexedWord>> children;
		Set<IndexedWord> root_descendants;
		
		QuestionStructure q;
		String curr_question;
		String curr_answer;
		ArrayList<String> curr_distractors;
				
		
		/* Question generation process repeated for each sentence */
		for (CoreMap sentence : sentences) {
			
			/* Re-initialize saved words */
			subjNP = null;
			objNP = null;
			verbVP = null;
			
			children = null;
			root_descendants = null;
			
			/* Current Sentence */
			//System.out.println("[ " + sentence.toString() + " ]");
			
			
			/* Dependencies Graph */
			dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		    //System.out.println("dependency graph:\n" + dependencies);
		    //System.out.println(dependencies.getClass().getName());
		    
			
			/* Set verbVP to root of parse tree */
		    verbVP = dependencies.getFirstRoot();
		    //System.out.println(verbVP);		    
		    //System.out.println(root.getClass().getName());
		    
		    
		    /* Children of Root, used to find words for questions */
		    children = dependencies.childPairs(verbVP);
		    
		    root_descendants = dependencies.descendants(verbVP);
		    root_descendants.remove(verbVP);
		    //System.out.println("Children:    " + root_children);
		    //System.out.println("Descendants: " + root_descendants);
		    
		    /* Grandchildren of Root, generally det', adj's, pronouns   	   */
		    /* 	 - calls symDiff: find the inverse of the intersect of 2 sets  */
		    
		    //System.out.println("Grandchildren: " + root_grandchildren);
		    
		    
		    /* Find Words in 'children' with GrammaticalRelations that can be used for questions */
		    for (Pair<GrammaticalRelation, IndexedWord> pair : children) {
		    	//System.out.println(pair.first.getShortName() + ", " + pair.second);
		    	
		    	/* [Word Dependencies List]			 /*	 [Word POS Tag List]			*/
		    	// nsubj	NOMINAL_SUBJECT			 //	 NN	  Noun Singular
				// subj		SUBJECT					 //	 NNS  Noun Plural
				// dobj		DIRECT_OBJECT			 //	 NNP  Proper Noun Singular
				// obj		OBJECT					 //	 NNPS Proper Noun Plural
				// iobj		INDIRECT_OBJECT			 //  RB	  Adverb
				// csubj	CLAUSAL_SUBJECT			 //
				// csubjpass CLAUSAL_PASSIVE_SUBJECT 
				// nsubjpass NOMINAL_PASSIVE_SUBJECT
				// pobj		PREPOSITIONAL_OBJECT
				// xcomp	XCLAUSAL_COMPLEMENT
				// mod		MODIFIER
				// rcmod	RELATIVE_CLAUSE_MODIFIER
				// possessive POSSESSIVE_MODIFIER
				// agent	AGENT
		    	
		    	
		    	/* [NP subject] */
		    	if (pair.second.tag().equals("NN")  || pair.second.tag().equals("NNS") ||
		    		pair.second.tag().equals("NNP") || pair.second.tag().equals("NNPS")) {
		    		
		    		//System.out.println("NOUN: " + pair.second);
		    		
			    	if (pair.first.getShortName() == "nsubj" || pair.first.getShortName() == "subj" ||
			    		pair.first.getShortName() == "csubjpass" || pair.first.getShortName() == "nsubjpass" ||
			    		pair.first.getShortName() == "agent") {
			    		
			    		subjNP = pair;
				    	//System.out.println(pair.first.getShortName() + ", " + pair.second);

			    	}
		    	}
		    	
		    	/* [NP object] */
		    	if (pair.second.tag().equals("NN")  || pair.second.tag().equals("NNS") ||
			    	pair.second.tag().equals("NNP") || pair.second.tag().equals("NNPS") ||
			    	pair.second.tag().equals("RB")) {
		    		
			    	if (pair.first.getShortName() == "dobj" || pair.first.getShortName() == "obj" ||
			    		pair.first.getShortName() == "xcomp" || pair.first.getShortName() == "advmod") {
			    		
			    		objNP = pair;
				    	//System.out.println(pair.first.getShortName() + ", " + pair.second);

			    	}
		    	}
		    	
		    	
		    }
		    
	    	/* Checks if pattern is found */
		    if (subjNP != null && objNP != null) {
		    	
		    	
		    	/*==== Extracting Hypernyms (supergroup) of the Question Word ==== */
		    	/*
		    	 * - Using extended Java WordNet Library
		    	 * - Uses functions from:
		    	 * 		*  generation_pipeline.Interface_Hypernym --> generation_tools.WordNetController
		    	 */
		    	
		    	String[] hypernyms = null;
		    	String hyps = "";
		    	
		    	try {
			    	PointerTargetNodeList hypernymsPointers = Interface_Hypernym.hypernyms(subjNP.second.originalText(), "noun");
			    	
			    	
			    	for (int i = 0; i < hypernymsPointers.size(); i++) {
				    	PointerTargetNode hyp = hypernymsPointers.get(i);
				    	String val = hyp.toString();
				    	
				    	val = val.replaceAll(".*Words: ", "");
				    	val = val.replaceAll("--.*", "");
				    	val = val.replaceAll(" ", "");
				    	
				    	if (i == hypernymsPointers.size()-1)
				    		hyps += val;
				    	else
				    		hyps += val+",";
				    	
				    	
			    	}
			    	
			    	hypernyms = hyps.split(",");
			    	
			    	//System.out.println(subjNP.second.originalText() + " hypernyms:\n" + hyps);
		    	}
		    	catch (Exception e) {
		    		//System.out.println("no hypernym");
		    	}
		    	
//		    	for (String hyper : hypernyms) {
//		    		System.out.println(hyper);
//		    	}
		    	
		    	
		    	/* >> If no hypernyms exist, no question made */
		    	if (hypernyms != null) {
		    		curr_question = "Which "+ hypernyms[hypernyms.length-1] +" "+ verbVP.originalText() +" "+ objNP.second.originalText() +"?";
		    		curr_answer = subjNP.second.originalText();
		    		curr_distractors = Interface_MultipleChoice.GenerateDistractors(curr_answer);
		    		
		    		curr_question = curr_question.replace('.', '?');
		    		
		    		q = new QuestionStructure(curr_question, curr_answer, curr_distractors);
		    		questions.addQuestions(q);
		    	}

		    /* End Pattern Check for sentence*/
	    	}
		    
		/* End For each Sentence */    
		}
		
		return questions;

	}
	

	/**
	 * transformation_3()
	 * - Generates a question following the form "Who pastTense(predicate) object?"
	 * 
	 * @param sentences
	 * @param questions 
	 * @return questions - questions now includes newly generated questions
	 * @throws Exception
	 */
	public static DocumentQuestions transformation_3(List<CoreMap> sentences, DocumentQuestions questions) throws Exception {
		// Pattern:		[NP object] [VBN predicate] by [NP subject]
		// Question:	Who pastTense(predicate) object?
		// Answer:		subject
		
		SemanticGraph dependencies;
		
		Pair<GrammaticalRelation, IndexedWord> objNP;
		IndexedWord predVBN;
		Pair<GrammaticalRelation, IndexedWord> subjNP;
		
		List<Pair<GrammaticalRelation, IndexedWord>> children;
		Set<IndexedWord> root_descendants;
		
		boolean byCheck = false;
		
		QuestionStructure q;
		String curr_question;
		String curr_answer;
		ArrayList<String> curr_distractors;
				
		
		/* Question generation process repeated for each sentence */
		for (CoreMap sentence : sentences) {
			
			/* Re-initialize saved words */
			objNP = null;
			predVBN = null;
			subjNP = null;
			
			children = null;
			root_descendants = null;
			
			/* Current Sentence */
			//System.out.println("[ " + sentence.toString() + " ]");
			
			
			/* Dependencies Graph */
			dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
// -->>	    //System.out.println("dependency graph:\n" + dependencies);
		    //System.out.println(dependencies.getClass().getName());
		    
			
			/* Set predVBN to root of parse tree */
		    predVBN = dependencies.getFirstRoot();
		    //System.out.println("first root: "+predVBN);		    
		    //System.out.println(root.getClass().getName());
		    
		    
		    /* Children of Root, used to find words for questions */
		    children = dependencies.childPairs(predVBN);
		    
		    root_descendants = dependencies.descendants(predVBN);
		    root_descendants.remove(predVBN);
		    //root_descendants_arr = (IndexedWord[]) root_descendants.toArray();
		    
		    for (IndexedWord word : root_descendants) {
		    	//System.out.println(word);
		    	if (word.originalText().equals("by")) {
		    		byCheck = true;
		    		//System.out.println(byCheck);
		    	}
		    }

		    if (!byCheck) {
		    	continue;
		    }
		    
		   		    
		    
		    /* Find Words in 'children' with GrammaticalRelations that can be used for questions */
		    for (Pair<GrammaticalRelation, IndexedWord> pair : children) {
		    	//System.out.println(pair.first.getShortName() + ", " + pair.second);
		    	
		    	/* [Word Dependencies List]			 /*	 [Word POS Tag List]			*/
		    	// nsubj	NOMINAL_SUBJECT			 //	 NN	  Noun Singular
				// subj		SUBJECT					 //	 NNS  Noun Plural
				// dobj		DIRECT_OBJECT			 //	 NNP  Proper Noun Singular
				// obj		OBJECT					 //	 NNPS Proper Noun Plural
				// iobj		INDIRECT_OBJECT			 //  RB	  Adverb
				// csubj	CLAUSAL_SUBJECT			 //  RBR  Adverb Comparative
				// csubjpass CLAUSAL_PASSIVE_SUBJECT //  RBS  Adverb Superlative
				// nsubjpass NOMINAL_PASSIVE_SUBJECT //  PRP  Personal Pronoun
				// pobj		PREPOSITIONAL_OBJECT	 //  PRP$ Possessive Pronoun
				// xcomp	XCLAUSAL_COMPLEMENT
				// mod		MODIFIER
				// rcmod	RELATIVE_CLAUSE_MODIFIER
				// possessive POSSESSIVE_MODIFIER
				// agent	AGENT
		    	
		    	
		    	/* [NP object] */
		    	if (pair.second.tag().equals("NN")  || pair.second.tag().equals("NNS") ||
			    	pair.second.tag().equals("NNP") || pair.second.tag().equals("NNPS") ||
			    	pair.second.tag().equals("RB")  || pair.second.tag().equals("RBR") ||
			    	pair.second.tag().equals("RBS") || pair.second.tag().equals("JJ") ||
			    	pair.second.tag().equals("JJS")) {
		    		
			    	if (pair.first.getShortName() == "dobj" || pair.first.getShortName() == "obj" ||
			    		pair.first.getShortName() == "xcomp" || pair.first.getShortName() == "advmod" ||
			    		pair.first.getShortName() == "nsubjpass") {
			    		
			    		objNP = pair;
				    	//System.out.println(pair.first.getShortName() + ", " + pair.second);

			    	}
		    		
		    	}
		    	
		    	/* [NP subject] */
		    	if (pair.second.tag().equals("NN")  || pair.second.tag().equals("NNS") ||
		    		pair.second.tag().equals("NNP") || pair.second.tag().equals("NNPS") ||
		    		pair.second.tag().equals("PRP") || pair.second.tag().equals("PRP$")) {
		    		
		    		//System.out.println("NOUN: " + pair.second);
		    		
			    	if (pair.first.getShortName() == "nsubj" || pair.first.getShortName() == "subj" ||
			    		pair.first.getShortName() == "csubjpass" || pair.first.getShortName() == "nsubjpass" ||
			    		pair.first.getShortName() == "agent" || pair.first.getShortName() == "nmod") {
			    		
			    		subjNP = pair;
				    	//System.out.println(pair.first.getShortName() + ", " + pair.second);

			    	}
		    	}
		    	
		    	
		    }
		    
	    	/* Checks if pattern is found */
		    if (objNP != null && subjNP != null && byCheck) {
		    	
	    		curr_question = "Who " + predVBN.originalText()+" "+ objNP.second.originalText() +"?";
	    		curr_answer = subjNP.second.originalText();
	    		curr_distractors = Interface_MultipleChoice.GenerateDistractors(curr_answer);
	    		
	    		curr_question = curr_question.replace(".", "?");
	    		
	    		q = new QuestionStructure(curr_question, curr_answer, curr_distractors);
	    		questions.addQuestions(q);

	    	}
		}
	
	
		return questions;
	}
	
	
	/**
	 * transformation_4()
	 * - Generates questions following the form
	 *     "A [because | therefore | due to | since | as a result | thus ] B"
	 * 
	 * @param sentences
	 * @param questions 
	 * @return questions - questions now includes newly generated questions
	 * @throws Exception
	 */
	public static DocumentQuestions transformation_4(List<CoreMap> sentences, DocumentQuestions questions) {
		// Pattern:		A, because B.
		// Question:	Why questionForm(A)?
		// Answer:		Because B.
		
		QuestionStructure q;
		ArrayList<String> distract_temp = new ArrayList<String>();
		
		
		String sentString;
		
		String stopWord;
		String nullWord;
		String currentWord;
		
		boolean becauseCheck;
		boolean thereforeCheck;
		boolean stopWordCheck;
		boolean stopWordFirst;
		boolean commaCheck;

		Matcher matcher;

		Pattern patBecause = Pattern.compile(".*([Bb]ecause).*");
		Pattern patDueTo = Pattern.compile(".*([Dd]ue to).*");
		Pattern patSince = Pattern.compile(".*([Ss]ince).*");
		
		Pattern patTherefore = Pattern.compile(".*([Tt]herefore).*");
		Pattern patThus = Pattern.compile(".*([Tt]hus).*");
		Pattern patAsResult = Pattern.compile(".*([Aa]s a result).*");
		
		String question = "Why ";
		String answer = "";
		
		int tokenEnum;
				
		
		/* Question generation process repeated for each sentence */
		for (CoreMap sentence : sentences) {
			
			/* Current Sentence */
			//System.out.println("[ " + sentence.toString() + " ]");
			
			
			stopWord = null;
			nullWord = null;
			
			becauseCheck = false;
			thereforeCheck = false;
			
			stopWordCheck = false;
			stopWordFirst = false;
			commaCheck = false;
			tokenEnum = 0;
			
			
			sentString = sentence.toString(); 
			
			
			
			becauseCheck = (sentString.matches(".*[Bb]ecause.*") ||
							sentString.matches(".*[Dd]ue to.*") ||
							sentString.matches(".*[Ss]ince.*"));
			
			thereforeCheck = (sentString.matches(".*[Tt]herefore.*") ||
							  sentString.matches(".*[Tt]hus.*") ||
							  sentString.matches(".*[Aa]s a result.*"));				
			
			
			
			if (!becauseCheck && !thereforeCheck) { 
				//System.out.println("No oppportunities.");
				continue;
				
			} else if (becauseCheck) {
				
				//System.out.println("Because type found");
				
				matcher = patBecause.matcher(sentString);


				if (matcher.matches()) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				
				matcher = patDueTo.matcher(sentString);

				if (matcher.matches() && (stopWord == nullWord)) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				matcher = patSince.matcher(sentString);
				
				if (matcher.matches() && (stopWord == nullWord)) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				//System.out.println("stopWord: " + stopWord);
								

				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
					
				    
				    currentWord = token.get(TextAnnotation.class);
				    
				    //System.out.println("current word: " +currentWord);					
				    
					//System.out.println("stop word check: " +stopWordCheck);
					//System.out.println("stop word first: " +stopWordFirst);
					//System.out.println("comma check: " + commaCheck);
					//System.out.println("token num: " + tokenEnum);

					if ((currentWord.equals(stopWord)) && (tokenEnum == 0)) {
						
						stopWordFirst = true;
						//System.out.println("stop word first: " + stopWordFirst);
						
						
					}
										
					if (stopWordFirst && !commaCheck) {
						
						if (currentWord.equals(",")) {
							answer += ".";
							commaCheck = true;
							//System.out.println("comma check: " + commaCheck);
						} else {
							answer += currentWord+" ";
						}
						
					} 
					else if (stopWordFirst && commaCheck) {
						
						question += currentWord+" ";
						
					}
					
					
					if (!stopWordFirst) {
						
						//System.out.println("!stopWordFirst");
						
						if (!stopWordCheck) {
							
							//System.out.println("!stopWordCheck");

							
							if (currentWord.equals(stopWord)) {
								answer += currentWord+" ";
								stopWordCheck = true;
								//System.out.println("stop word check: " + stopWordCheck);

							}
							else if (!(currentWord.equals(stopWord))) {
								question += currentWord+" ";
							}
							
						}
						
						else if (stopWordCheck) {
							answer += currentWord+" ";
							
						}
							
					}
					
					tokenEnum += 1;
				   
				} // for each token ends
				
				
				
			} else if (thereforeCheck) {
				
				//System.out.println("Therefore type found");
				
				
				matcher = patTherefore.matcher(sentString);
				
				if (matcher.matches() && (stopWord == nullWord)) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				
				matcher = patThus.matcher(sentString);

				if (matcher.matches() && (stopWord == nullWord)) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				
				matcher = patAsResult.matcher(sentString);
				
				if (matcher.matches() && (stopWord == nullWord)) {
				    //System.out.println("Matched: " + matcher.group(1));
				    stopWord = matcher.group(1);
				} else {
				    //System.out.println("No match");
				}
				
				//System.out.println("stopWord: " + stopWord);
				

				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				    
				    currentWord = token.get(TextAnnotation.class);
				    
				    //System.out.println("current word: " +currentWord);					
				    
					//System.out.println("stop word check: " +stopWordCheck);
					//System.out.println("stop word first: " +stopWordFirst);
					//System.out.println("comma check: " + commaCheck);
					//System.out.println("token num: " + tokenEnum);

					if ((currentWord.equals(stopWord)) && (tokenEnum == 0)) {
						
						stopWordFirst = true;
						//System.out.println("stop word first: " + stopWordFirst);
						
					}
										
					if (stopWordFirst && !commaCheck) {
						
						if (currentWord.equals(",")) {
							question += ".";
							commaCheck = true;
							//System.out.println("comma check: " + commaCheck);
						} else {
							question += currentWord+" ";
						}
						
					} 
					else if (stopWordFirst && commaCheck) {
						
						answer += currentWord+" ";
						
					}
					
					
					
					if (!stopWordFirst) {
						
						//System.out.println("!stopWordFirst");
						
						if (!stopWordCheck) {
							
							//System.out.println("!stopWordCheck");

							
							if (currentWord.equals(stopWord)) {
								answer = "Because " + answer;
								stopWordCheck = true;
								//System.out.println("stop word check: " + stopWordCheck);

							}
							else if (!(currentWord.equals(stopWord))) {
								answer += currentWord+" ";
							}
							
						}
						
						else if (stopWordCheck) {
							question += currentWord+" ";
							
						}
							
					}
					
					tokenEnum += 1;
				   
				} // for each token ends
				
			}
			
			//System.out.println("Question: "+question);
			//System.out.println("Answer: "+answer);
			
			question = question.replace(".", "?");
			
			q = new QuestionStructure(question, answer, distract_temp);
    		questions.addQuestions(q);
			
		}
	
	
		return questions;
	}


}
