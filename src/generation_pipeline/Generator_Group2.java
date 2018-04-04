package generation_pipeline;

import edu.stanford.nlp.dcoref.*;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.IntTuple;
import edu.stanford.nlp.util.Pair;
import generation_models.Actor;
import generation_models.Component;
import generation_models.DocumentQuestions;
import generation_models.Fact;
import generation_models.QuestionStructure;
import generation_models.UserSimple;

import java.util.*;
import java.util.Map.Entry;


/**
 * Generator_Group2 requires specific properties to be included in the StanfordPipeline that require lots of heap space.
 *   The groups are separated because Group1 can still be run on lesser-performing computers.
 * - To utilize Group2, assign boolean full = true in Driver.java
 *   
 * @author TerranceTaubes
 *
 */
public class Generator_Group2 {

	// Lesson identification globals
	public static String globalPrompt;
	public static UserSimple globalUser;

	public Generator_Group2(){}
	private static ArrayList<Actor> actorMatrix;
	private static HashMap<Fact, Short> factFrequencyMatrix;
	public static List<String> whatDistractors;

	// Lists of labels that can fulfill a particular part of speech need (all nouns, all adjectives, ...)
	private static List<String> nounLabels = Arrays.asList("NN", "NNP", "NNPS", "NNS", "PRP");
	private static List<String> verbLabels = Arrays.asList("VB", "VBD", "VBG", "VBN", "VBP", "VBZ");
	private static List<String> adjectiveLabels = Arrays.asList("JJ", "JJR", "JJS", "DT", "PRP$"); // adjectives (comparatives, prepositions, etc.)
	private static List<String> modifierLabels = Arrays.asList("MD", "ADVP", "RB"); // verb modifiers (adverbs, helping verbs, ...)
	private static List<String> verbHelpers = Arrays.asList("MD", "VBD"); // words that can be part of the whole verb ("could", "did", "will", ...)

	// List of words that mean the verb is negative
	private static List<String> negatives = Arrays.asList("not", "n't", "never");

	// Classifier information
	private static AbstractSequenceClassifier<CoreLabel> classifier;

	// Verb extraction globals
	private static int maxDepth;
	private static Tree deepestVerb;
	private static String actorName;
	

	/**
	 * findDeepestVerb()
	 * - Recursive search for deepest verb node in a certain tree
	 * 
	 * @param currentDepth, the current depth this recursion operates at
	 * @param root, the root of the subtree we are exploring at this recursive step
	 */
	public static void findDeepestVerb(int currentDepth, Tree root) {
		// first try all of the children of this node
		for (Tree child : root.getChildrenAsList()) {
			findDeepestVerb(currentDepth + 1, child);
		}

		// if this verb is deeper than our current deepest verb, make it the new deepest verb
		if (verbLabels.contains(root.label().toString()) && currentDepth > maxDepth) {
			maxDepth = currentDepth;
			deepestVerb = root;
		}
	}
	
	/**
	 * extractAttributes()
	 * - Extract all the attributes of a particular node
	 * 
	 * @param component, the target node whose attributes we want
	 * @param sentence, the tree containing the original sentence
	 * @return attributes
	 */
	public static List<Tree> extractAttributes(Tree component, Tree sentence) {
		List<Tree> attributes = new ArrayList<Tree>();
		List<String> targetLabels = new ArrayList<String>();

		// if (component == null || sentence == null) {
		// 	return null;
		// }

		// collect all siblings of this component
		List<Tree> siblings = component.siblings(sentence);

		// verb attributes must be adverbs, etc., etc.
		if (adjectiveLabels.contains(component.label().toString())) {
			targetLabels = Arrays.asList("RB");
		} else if (nounLabels.contains(component.label().toString())) {
			targetLabels = Arrays.asList("ADJP", "CD", "DT", "JJ", "NP", "POS", "PRP$", "QP");
		} else if (verbLabels.contains(component.label().toString())) {
			targetLabels = Arrays.asList("ADVP", "RB", "MD");
		}

		// iterate over all siblings and collect only the ones decided above in attributes
		if(siblings != null){
			for (Tree node : siblings) {
				if (targetLabels.contains(node.label().toString())) {
					attributes.add(node);
				}
			}
		}

		List<Tree> uncles = null;

		// collect all uncles of this component
		if (component.parent(sentence) != null) {
			uncles = component.parent(sentence).siblings(sentence);
		} else {
			uncles = new ArrayList<Tree>();
		}

		// iterate over all uncles and collect the appropriate modifiers
		for (Tree node : uncles) {
			if ((nounLabels.contains(component.label().toString()) || adjectiveLabels.contains(component.label().toString())) && node.label().toString().equals("PP")) {
				attributes.add(node);
			} else if (verbLabels.contains(component.label().toString()) && (verbLabels.contains(node.label().toString()) || modifierLabels.contains(node.label().toString()))) {
				// Add the direct children of an adverb phrase (cousins!)
				if (node.label().toString().equals("ADVP")) {
					for (Tree child : node.children()) {
						attributes.add(child);
					}
				// Directly add standalone adverbs and modifiers like "could" and "not"
				} else {
					attributes.add(node);
				}
			}
		}

		return attributes;
	}

	/**
	 * extractOneFact()
	 * - Extract a single Fact object from a sentence
	 * 
	 * @param mySentence - the Stanford-parsed and annotated sentence (including lemmas, etc.)
	 * @return
	 */
	public static Fact extractOneFact(CoreMap mySentence) {
		Tree sentence = mySentence.get(TreeCoreAnnotations.TreeAnnotation.class).skipRoot();

		//sentence.pennPrint(System.out);

		/***** Local variables *****/
		Tree subjectCore = null, verbCore = null, objectCore = null, indirectObjectCore = null;
		Tree subjectTree = null, predicateTree = null, objectTree = null, indirectObjectTree = null;
		List<Tree> potentialObjectTrees = new ArrayList<Tree>();

		Fact fact;
		Component subject = null, verb = null, object = null, indirectObject = null;
		String properNoun = "OTHER";

		maxDepth = 0;
		deepestVerb = null;
		actorName = null;

		/***** Fact Extraction *****/
		// Cannot extract a fact from a null sentence
		if (sentence == null) {
			return null;
		}

		// Iterate over all the sentence's children, looking for the subject and predicate
		for (Tree child : sentence.getChildrenAsList()) {
			// If we do not yet have a subject, keep looking for the first NP to become the subject
			if (subjectTree == null) {
				if (child.label().toString().equals("NP")) {
					subjectTree = child;

				}
				// If we do not yet have a verb, keep looking for the first VP to become the predicate
			} else if (predicateTree == null) {
				if (child.label().toString().equals("VP")) {
					predicateTree = child;
				}
				// If we have both a subject and a predicate, we don't need to look anymore ((later: add support for multiple clauses))
			} else {
				break;
			}
		}

		// Cannot extract a fact from a sentence without a subject or without a predicate
		if (subjectTree == null || predicateTree == null) {
			return null;
		}

		// extract subject from the NP--use BFS
		Queue<Tree> bfsQueue = new LinkedList<Tree>();

		bfsQueue.add(subjectTree);

		// examine every child in BFS and find the first noun
		while (!bfsQueue.isEmpty()) {
			Tree node = bfsQueue.remove();

			if (nounLabels.contains(node.label().toString())) {
				// first noun in BFS search
				// TODO: if NNP, find any siblings that are also NNP and change this node's value to be NNP??? and remove the siblings?? tree surgery
				subjectCore = node;
				break;
			} else {
				for (Tree child : node.getChildrenAsList()) {
					bfsQueue.add(child);
				}
			}
		}

		// If we fail to find a NP, look for a pronoun (determiner):
		// (don't look for a determiner first because we might have a sentence that starts with a determiner and a noun like "those shoes")
		if (subjectCore == null) {
			bfsQueue = new LinkedList<Tree>();

			bfsQueue.add(subjectTree);

			// examine every child in BFS and find the first noun
			while (!bfsQueue.isEmpty()) {
				Tree node = bfsQueue.remove();

				if (node.label().toString().equals("DT")) {
					// first noun in BFS search
					// TODO: if NNP, find any siblings that are also NNP and change this node's value to be NNP??? and remove the siblings?? tree surgery
					subjectCore = node;
					break;
				} else {
					for (Tree child : node.getChildrenAsList()) {
						bfsQueue.add(child);
					}
				}
			}
		}

		if (subjectCore == null) {
			return null;
		}

		// extract verb from the VP--find deepest verb
		findDeepestVerb(0, predicateTree);
		verbCore = deepestVerb;

		// Now that we have the subject and predicate, hunt through the predicate's children for potential objects
		List<Tree> verbSiblings = deepestVerb.siblings(sentence);
		if(verbSiblings!= null){
			for (Tree potentialObject : verbSiblings) {
				// if we have not found any objects yet,
				if (potentialObjectTrees.size() == 0) {
					// if the current child is an ADJP, it is our object and we stop (can only have one adjective in a clause at this level)
					if (potentialObject.label().toString().equals("ADJP")) {
						objectTree = potentialObject;
						break;
						// if it is a noun, it is an object candidate and we continue (could have a direct object and an indirect object)
					} else if (potentialObject.label().toString().equals("NP")) {
						potentialObjectTrees.add(potentialObject);
					}

					// if we HAVE found some object but there is more to search,
				} else {
					// if we find another NP we save it and we'll have to decide which is the IO and which is the DO
					if (potentialObject.label().toString().equals("NP")) {
						potentialObjectTrees.add(potentialObject);
						// TODO: if we find an ADJP it becomes our complement and we stop...except stanford doesn't understand object complements like that
						// an adjective complement cannot exist without a direct object (above)
						//				} else if (potentialObject.label().toString().equals("ADJP")) {
						//					System.out.println("adjectival");
						//					complementTree = potentialObject;
						//					break;
					}

				}

			}
		}
		

		// if we have not yet found an object but we have candidate objects,
		if (objectTree == null && potentialObjectTrees.size() > 0) {
			// if we have only one candidate it becomes our object and we continue
			if (potentialObjectTrees.size() == 1) {
				objectTree = potentialObjectTrees.get(0);
				// else if we have more than one candidate we must choose
			} else if (potentialObjectTrees.size() == 2) { // can't imagine a situation in which we should have three or more
				// imagine the IO will be a person or organization and the DO will not (awkward sentence otherwise)
				// (save the trees to local variables to save on access time for accessing them a bunch of times below)
				Tree firstTree = potentialObjectTrees.get(0);
				Tree secondTree = potentialObjectTrees.get(1);
				
				if (secondTree.yield().get(0).value().equals("PERSON") || secondTree.yield().get(0).value().equals("ORGANIZATION") || secondTree.label().equals("PRP")) {
					objectTree = firstTree;
					indirectObjectTree = secondTree;
				} else {
					// the more common ordering is for the IO to be before the DO, so by default, do that.
					indirectObjectTree = firstTree;
					objectTree = secondTree;
				}
			}
		}

		// If we have indeed found an object, extract its core
		if (objectTree != null) {
			// now extract the core from the object tree
			List<String> targetObjectLabels = null;

			// If the object is a NP, look for nouns
			if (objectTree.label().toString().equals("NP")) {
				targetObjectLabels = nounLabels;
				// If the object is an ADJP, look for adjectives
			} else {
				targetObjectLabels = adjectiveLabels;
			}

			// Now perform BFS search for the first noun or adjective (whichever appropriate) to extract the core
			bfsQueue = new LinkedList<Tree>();
			bfsQueue.add(objectTree);

			while (!bfsQueue.isEmpty()) {
				Tree node = bfsQueue.remove();

				if (targetObjectLabels.contains(node.label().toString())) {
					objectCore = node;
					break;
				} else {
					for (Tree child : node.getChildrenAsList()) {
						bfsQueue.add(child);
					}
				}
			}
		}

		// If we have indeed found an INDIRECT object, extract its core.
		// Indirect objects must be nouns (or personal pronouns).
		if (indirectObjectTree != null) {
			// Now perform BFS search for the first noun or adjective (whichever appropriate) to extract the core
			bfsQueue = new LinkedList<Tree>();
			bfsQueue.add(indirectObjectTree);

			while (!bfsQueue.isEmpty()) {
				Tree node = bfsQueue.remove();

				if (nounLabels.contains(node.label().toString())) {
					indirectObjectCore = node;
					break;
				} else {
					for (Tree child : node.getChildrenAsList()) {
						bfsQueue.add(child);
					}
				}
			}
		}

		// now make Components and craft final Fact
		subject = new Component();
		subject.setCore(subjectCore);
		subject.setAttributes(extractAttributes(subjectCore, sentence));

		// TODO: remove object complement from attribute lists? assuming we ever manage to extract it

		properNoun = classifier.classifyToString(subject.getCore().yield().get(0).value()).split("/")[1];
		//System.out.println(classifier.classifyToString(subject.getCore().yield().get(0).value()));

		subject.setProperNounType(properNoun);

		// if subject is a proper noun, see if it has multiple parts
		if ((subjectCore.label().toString().equals("NNP")) && (subjectCore.parent(sentence) != null)) {
			actorName = "";
			for (Tree sibling : subjectCore.parent(sentence).getChildrenAsList()) {
				if (sibling.label().toString().equals("NNP")) {
					actorName += (sibling.yield().get(0).value() + " ");
				}
			}
		} else {
			actorName = null;
		}

		verb = new Component();
		verb.setCore(verbCore);
		verb.setAttributes(extractAttributes(verbCore, sentence));

		if (objectCore != null) {
			object = new Component();
			object.setCore(objectCore);
			object.setAttributes(extractAttributes(objectCore, sentence));
			object.setProperNounType(classifier.classifyToString(objectCore.yield().get(0).value()).split("/")[1]);
		}

		if (indirectObjectCore != null) {
			indirectObject = new Component();
			indirectObject.setCore(indirectObjectCore);
			indirectObject.setAttributes(extractAttributes(indirectObjectCore, sentence));
			indirectObject.setProperNounType(classifier.classifyToString(indirectObjectCore.yield().get(0).value()).split("/")[1]);
		}

		// Finally craft the final Fact
		fact = new Fact();
		
		String s = "";
		for (Label l : sentence.yield()) {
			s += l.value() + " ";
		}
		
		fact.setSentence(s.trim() + ".");
		fact.setSubject(subject);
		fact.setVerb(verb);
		if (object != null) {
			fact.setObject(object);
		}

		if (indirectObject != null) {
			fact.setIndirectObject(indirectObject);
		}

		fact.setNegative(false);

		for (Tree attr : verb.getAttributes()) {
			s = "";
			
			for (Label l : attr.yield()) {
				s += l.value() + " ";
			}
			
			s = s.trim();
			
			// this should only catch things that are ONLY "not"--not the phrase "not unlike" or any such thing
			if (negatives.contains(s)) {
				fact.setNegative(true);
			}
		}

		/***** RESULTS PRINTING *****/
		//System.out.println("\n\nSUBJECT:");
		//subjectCore.pennPrint();		
		//System.out.println("It is a " + subject.getProperNounType());

//		for (Tree att : subject.getAttributes()) {
//			//System.out.println("---SUBJECT ATTRIBUTE:");
//			//att.pennPrint();
//		}
//
//		//System.out.println("\n\nVERB:");
//		//verbCore.pennPrint();
//
//		for (Tree att : verb.getAttributes()) {
//			//System.out.println("---VERB ATTRIBUTE:");
//			//att.pennPrint();
//		}
//
//		//System.out.println("\n\nOBJECT:");
//		if (objectCore != null) {
//			//objectCore.pennPrint();	
//			//System.out.println("It is a " + object.getProperNounType());
//
//			for (Tree att : object.getAttributes()) {
//				//System.out.println("---OBJECT ATTRIBUTE:");
//				//att.pennPrint();
//			}
//		} else {
//			//System.out.println("(none)");
//		}
//
//		//System.out.println("\n\nINDIRECT OBJECT:");
//		if (indirectObjectCore != null) {
//			//indirectObjectCore.pennPrint();	
//			//System.out.println("It is a " + indirectObject.getProperNounType());
//
//			for (Tree att : indirectObject.getAttributes()) {
//				//System.out.println("---OBJECT ATTRIBUTE:");
//				//att.pennPrint();
//			}
//		} else {
//			//System.out.println("(none)");
//		}

		//System.out.println("Negative sentence: " + fact.isNegative());

		// System.out.println("COMPLEMENT");
		// if (complementTree != null) {
		// 	complementTree.pennPrint();
		// }

		// If the object is a person or thing, add them to the "what" distractors
		if (object != null && nounLabels.contains(objectCore.label().toString())) {
			String corrChoice = "";
	        for(Label test : object.getCore().yield()){
				corrChoice = corrChoice + test.value();
			}
			corrChoice = corrChoice.trim();

			whatDistractors.add(corrChoice);
		}

		return fact;
	}


	/**
	 * insertFact()
	 * - Add one Fact into the fact matrix coresponding to its proper Actor.
	 * - If the Actor of this Fact doesn't exist, make a new Actor. Otherwise, find its Actor and put it there.
	 * 
	 * @param Fact - the fact that is to be inserted. 
	 * @return
	 */
	public static void insertFact(Fact fact) {
		if (fact == null) {
			return;
		}

		// Get the subject of this fact as a String
		String s = "";
		// If it has a longer proper name, make that the actor's name
		if (actorName == null) {
			for (Label label : fact.getSubject().getCore().yield()) {
				s += label.value() + " ";
			}
			s = s.trim();
		} else {
			s = actorName;
		}

		// Iterate over all existing Actors and see if this Fact belongs anywhere
		boolean found = false;
		Actor chosenActor;

		for (Actor actor : actorMatrix) {
			// if the names are the same, put this fact under this actor
			if (actor.getName().trim().equals(s)) {
				actor.getFactMatrix().add(fact);
				found = true;
				chosenActor = actor;
			}

			// if the fact's name contains the actor's name, make the fact's name the actor's new name and add it to their aliases and put this fact under this actor
			if (s.contains(actor.getName().trim())) {
				actor.setName(s.trim());
				actor.getAliases().add(s);
				actor.getFactMatrix().add(fact);
				found = true;
				chosenActor = actor;
			}

			// if the actor's name contains the fact's name, add the fact's name to their aliases and put this fact under this actor
			if (actor.getName().contains(s)) {
				actor.getAliases().add(s);
				actor.getFactMatrix().add(fact);
				found = true;
				chosenActor = actor;
			}

			// TODO: What if we have names like "Adam Smith" and "Mr. Smith"? Conversely, what about "Mr. Smith" and "Mrs. Smith" in the same passage?
		}

		// If we did not find an actor to go with this fact, make a new one
		if (!found) {
			Actor newActor = new Actor(s.trim(), fact.getSubject().getProperNounType());

			newActor.getAliases().add(s.trim());
			newActor.getFactMatrix().add(fact);

			actorMatrix.add(newActor);
			chosenActor = newActor;
		}

		found = false;

		// Find this fact in the frequency matrix and increase its frequency by 1
		// (I think this technically has another name than "frequency" since it's not a probability less than 1 but)
		for (Fact f : factFrequencyMatrix.keySet()) {
			if (f.predicateEquals(fact)) {
				factFrequencyMatrix.put(f, (short)(factFrequencyMatrix.get(f) + 1));
				found = true;
			}
		}

		if (!found) {
			factFrequencyMatrix.put(fact, (short)1);
		}
	}

	
	/**
	 * generateComplexChooseQuestions()
	 * - Generates questions like "Which of the following did John not do?"
	 * - Chooses 3 things John has done and 1 thing he has not as answers.
	 * 
	 * @param n, the number of questions of this type to generate.
	 * @return dq
	 * @throws Exception 
	 */
	public static DocumentQuestions generateComplexChooseQuestions(int n, DocumentQuestions dq) throws Exception {
		// Cannot generate questions if the matrix is smaller than 2 actors
		if (actorMatrix == null || actorMatrix.size() < 2) {
			return dq;
		}
		
		QuestionStructure q;

		for (int x = 0; x < n; x++) {
			Random rng = new Random();
			boolean foundActor = false;
			int tries = 0;
			Actor candidateActor = null, secondActor = null;

			// Try to find an actor with 3 or more actions at most 20 times.
			// If we do not find such an actor, we cannot generate questions of this type.
			while (!foundActor && tries < 20) {
				int i = rng.nextInt(actorMatrix.size()); 
				candidateActor = actorMatrix.get(i);
				tries++;

				if (candidateActor.getFactMatrix().size() >= 3) {
					foundActor = true;
				}
			}

			if (!foundActor) {
				return dq;
			}

			// Now we have a candidate actor and the promise that other actors exist
			// Look for a second actor who has done at least one thing the first actor has not.
			boolean foundSecond = false;
			tries = 0;
			List<Fact> possibleAnswers = new ArrayList<Fact>();

			while (!foundSecond && tries < 30) {
				int i = rng.nextInt(actorMatrix.size()); 
				secondActor = actorMatrix.get(i);
				tries++;

				// Do not compare an actor with themselves
				if (secondActor.getName().equals(candidateActor.getName())) {
					continue;
				}

				// This bit almost has to be O(mn), m = # facts from actor 1, n = # facts from actor 2
				for (Fact secondFact : secondActor.getFactMatrix()) {
					boolean duplicate = false;
					// if this fact is not the same as any of actor 1's facts, add it to a list.
					comparisonFor: for (Fact candidateFact : candidateActor.getFactMatrix()) {
						// check if the two facts are "equal"
						if (secondFact.predicateEquals(candidateFact)) {
							duplicate = true;
							// if this fact is a duplicate of any of actor 1's facts, it cannot be used--break immediately
							break comparisonFor;
						}
					}

					// Add this new fact to the list if it is not also in actor 1's fact matrix
					// Accept actor 2 as the second actor if they have one fact not in actor 1's fact matrix
					if (!duplicate) {
						foundSecond = true;
						possibleAnswers.add(secondFact);
					}
				}
			}

			if (!foundSecond) {
				return dq;
			}

			// Grab the correct content (the thing actor 1 did NOT do)
			int i = rng.nextInt(possibleAnswers.size());
			Fact correctFact = possibleAnswers.get(i);

			// Add the question into the database
			String question = "Which of the following things did " + candidateActor.getName().trim() + " not do?";
			
			
		    // Add the correct choice (the thing the actor did NOT do)
		    String answer = correctFact.stringWithoutSubject();
		    //System.out.println("answer: " + answer);
		    
//		    ArrayList<String> distractors = Interface_MultipleChoice.GenerateDistractors(answer);
		    
		    
		    ArrayList<String> dists = new ArrayList<String>();

	        //Add distractors (things that the actor DID do)
	        ArrayList<Fact> allDistractors = new ArrayList<Fact>();
	        allDistractors.addAll(candidateActor.getFactMatrix());
	        
	        LinkedList<Integer> chosen = new LinkedList<Integer>();
		    for (int j = 0; j < 3; j++) {
		        int idx = new Random().nextInt(allDistractors.size());
		        if(chosen.contains((Integer)idx))
		        	j--;
		        else{
		        	chosen.add((Integer)idx);
		        	String choiceText = allDistractors.get(idx).stringWithoutSubject().trim();
		        	dists.add(choiceText);
		        }
		        
			}
		    
		    /*====  Question Structure Added (( 1 [Which was not done?] ))  ====*/
		    
		    q = new QuestionStructure(question, answer, dists);
    		dq.addQuestions(q);
    		
    		/*============================================*/
		    

//			questions.add(part);
		}
		
		return dq;
	}
	
	
	/**
	 * generateSimpleQuestions()
	 * - Generates simple questions using only one fact.
	 * - Generates all possible simple questions.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public static DocumentQuestions generateSimpleQuestions(CoreMap mySentence, DocumentQuestions dq) throws Exception {
		// go over every actor and every fact they did
		Random rng = new Random();
		
		QuestionStructure q;

		actorLoop: for (Actor actor : actorMatrix) {
			//System.out.println("Actor is " + actor.getName());
			factLoop: for (Fact fact : actor.getFactMatrix()) {
				//System.out.println("Fact is " + fact.stringWithoutSubject());
				Component subject = fact.getSubject();
				Component object = fact.getObject();
				Component verb = fact.getVerb();
				Component indirectObject = fact.getIndirectObject();

				if (subject == null) {
					continue factLoop;
				}

				/**
				 * Try to make a "WHO" question with this fact
				 */
				if (actor.getProperNounType().equals("PERSON") && fact.getObject() != null && actorMatrix.size() > 2) {
					// collect distractors from the list
					//System.out.println("Testing this");
					ArrayList<String> distractors = new ArrayList<String>();

					// look through all actors for enough distractors -- they must be people
					for (Actor secondActor : actorMatrix) {
						if (!secondActor.did(fact) && secondActor.getProperNounType().equals("PERSON")) {
							//System.out.println("Adding " + secondActor.getName() + " as distractor");
							distractors.add(secondActor.getName());
						}
					}

					// generate the question if we found enough distractors
					if (distractors.size() > 2) {
						String question = "Who ";
					
						// Add the verb and any attributes
						if(verb != null){
							// Add any verb attributes that should precede the verb
							for(Tree test2 : verb.getAttributes()){
								if(modifierLabels.contains(test2.label().toString()) || verbLabels.contains(test2.label().toString())){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}
							}

							// Add the verb itself
							for(Label test : verb.getCore().yield()){
								question = question + test.value() + " ";
							}
						}

						// Add the indirect object and any attributes (if it exists)
						if(indirectObject != null){
							// Add any attributes that should precede the indirect object
							for(Tree test2 : indirectObject.getAttributes()){
								if(adjectiveLabels.contains(test2.label().toString())){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}

							}

							// Add the indirect object itself
							for(Label test : indirectObject.getCore().yield()){
								question = question + test.value() + " ";
							}

							// Add any attributes that should succeed the indirect object
							for(Tree test2 : indirectObject.getAttributes()){
								if(test2.label().toString().equals("PP")){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}
							}
						}
						
						// Add the direct object and any attributes (if it exists)
						if(object != null){
							// Add any attributes that should precede the direct object
							for(Tree test2 : object.getAttributes()){
								if(adjectiveLabels.contains(test2.label().toString())){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}
							}

							// Add the direct object itself
							for(Label test : object.getCore().yield()){
								question = question + test.value() + " ";
							}

							// Add any attributes that should succeed the direct object
							for(Tree test2 : object.getAttributes()){
								if(test2.label().toString().equals("PP")){
									for(Label test : test2.yield()){
										//
										question = question + test.value() + " ";
									}
								}
							}
						}

						question = question.trim() + "?";
						//System.out.println("SIMPLE QUESTION GENERATED " + question);
						//System.out.print("Answer is ");

				        String corrChoice = "";
				        for(Label test : subject.getCore().yield()){
							corrChoice = corrChoice + test.value() + " ";
						}
				        
				        // Answer
						corrChoice = corrChoice.trim();
				        
				        //System.out.println(corrChoice);
						
					    
						
					    /*====  Question Structure Added (( 2 [WHO] ))  ====*/
					    
					    q = new QuestionStructure(question, corrChoice, distractors);
			    		dq.addQuestions(q);
			    		
			    		/*============================================*/
						
				        
				        int index = 0;

				        // TODO: do not add every single distractor?
						while(index < distractors.size()){
							//System.out.println("Distractor: " + distractors.get(index));
				        	index++;
				        }
	       					        
//				        questions.add(part);
				        //System.out.println("TESTING THESE QUESTIONS " + questions.size());
					}
				}

				/**
				 * Try to make a "WHERE" question with this fact
				 */
				if (actor.getProperNounType().equals("LOCATION") && fact.getObject() != null && actorMatrix.size() > 2) {
					// collect distractors from the list
					ArrayList<String> distractors = new ArrayList<String>();

					// look through all actors for enough distractors -- they must be locations
					for (Actor secondActor : actorMatrix) {
						if (!secondActor.did(fact) && secondActor.getProperNounType().equals("LOCATION")) {
							//System.out.println("Adding " + secondActor.getName() + " as distractor");
							distractors.add(secondActor.getName());
						}
					}

					// generate the question if we found enough distractors
					if (distractors.size() > 2) {
						String question = "Where ";
			
						// Add verb and any attributes
						if(verb != null){
							// Add any attributes that should precede the verb
							for(Tree test2 : verb.getAttributes()){
								for(Label test : test2.yield()){
									String temp;
									if(test.value().toCharArray()[test.value().length()] == 's'){
										temp = test.value().substring(0, test.value().length()-1);
									}
									else
										temp = test.value();
									//TODO implement this if statement 
									//if(verbLabels.contains(test.toString()))
									question = question + temp + " ";
								}

							}

							// Add the verb itself
							for(Label test : verb.getCore().yield()){
								question = question + test.value() + " ";
							}
						}

						// Add the indirect object and any attributes (if it exists)
						if(indirectObject != null){
							// Add any attributes that should precede the indirect object
							for(Tree test2 : indirectObject.getAttributes()){
								if(adjectiveLabels.contains(test2.label().toString())){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}

							}

							// Add the indirect object itself
							for(Label test : indirectObject.getCore().yield()){
								question = question + test.value() + " ";
							}

							// Add any attributes that should succeed the indirect object
							for(Tree test2 : indirectObject.getAttributes()){
								if(test2.label().toString().equals("PP")){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}
							}
						}
						
						// Add the direct object and any attributes
						if(object != null){
							// Add any attributes that should precede the direct object
							for(Tree test2 : object.getAttributes()){
								if(adjectiveLabels.contains(test2.label().toString())){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}

							}

							// Add the direct object itself
							for(Label test : object.getCore().yield()){
								question = question + test.value() + " ";
							}

							// Add any attributes that should succeed the direct object
							for(Tree test2 : object.getAttributes()){
								if(test2.label().toString().equals("PP")){
									for(Label test : test2.yield()){
										question = question + test.value() + " ";
									}
								}
							}
						}
						
						question = question.trim() + "?";
						//System.out.println(question);
						//System.out.print("Answer is ");
						

				        String corrChoice = "";
				        for(Label test : subject.getCore().yield()){
							corrChoice = corrChoice + test.value() + " ";
						}
						corrChoice = corrChoice.trim();						
						
						/*====  Question Structure Added (( 3 [WHERE] ))  ====*/
					    
					    q = new QuestionStructure(question, corrChoice, distractors);
			    		dq.addQuestions(q);
			    		
			    		/*============================================*/

				        int index = 0;

				        // TODO: do not add every single distractor?
						while(index < distractors.size()){
				        	index++;
				        }

				        //System.out.println(questions.size());

					}
				}

				/**
				 * Try to make a "WHAT" question with this fact
				 */
				 if (fact.getObject() != null && nounLabels.contains(fact.getObject().getCore().label().toString()) && whatDistractors.size() > 2) {
				 	// collect distractors from the list
				 	ArrayList<String> distractors = new ArrayList<String>();

				 	// try 30 times to find enough distractors
				 	distractorFinder: for (int i = 0; i < 30; i++) {
				 		String candidate = whatDistractors.get(rng.nextInt(whatDistractors.size()));

				 		// just want to make sure actor does NOT have a fact with the same verb as fact and this distractor as the object
				 		// then this distractor is allowable
				 		factFinder: for (Fact testFact : actor.getFactMatrix()) {
				 			// if the verbs are different, continue looking for the distractor
				 			String verb1 = "", verb2 = "";
				 			for (Label l : fact.getVerb().getCore().yield()) {
				 				verb1 += l.value() + " ";
				 			}

				 			for (Label l : testFact.getVerb().getCore().yield()) {
				 				verb2 += l.value() + " ";
				 			}

				 			if (!verb1.equals(verb2)) {
				 				continue factFinder;
				 			}

				 			// if the objects are the same, veto this distractor (continue distractorFinder)
				 			String testObject = "";
				 	        for(Label test : object.getCore().yield()){
				 				testObject += test.value();
				 			}
				 			testObject = testObject.trim();

				 			if (testObject.equals(candidate)) {
				 				continue distractorFinder;
				 			}
				 		}
				 	}

				 	// generate the question if we found enough distractors
				 	if (distractors.size() > 2) {
				 		String question;
				 		// TODO: CONSIDER: Do we want these questions to be in the past tense all the time?
				 		// Add the question words
				 		String helpingVerb = "did ";

				 		// Hunt down any parts of the verb like "will" or "could" that might be more appropriate
				 		for (Tree attr : verb.getAttributes()) {
				 			if (verbHelpers.contains(attr.label().toString())) {
				 				helpingVerb = "";
				 				for (Label l : attr.yield()) {
				 					helpingVerb += l.value() + " ";
				 				}
				 			}
				 		}

				 		if (object.getProperNounType().equals("PERSON")) {
				 			question = "Who " + helpingVerb;
				 		} else {
				 			question = "What " + helpingVerb;
				 		}

				 		// Add the subject and any attributes
				 		if(subject != null){
				 			// Add any adjectival attributes that precede the subject
				 			for(Tree test2 : subject.getAttributes()){
				 				if(adjectiveLabels.contains(test2.label().toString())){
				 					for(Label test : test2.yield()){
				 						question = question + test.value() + " ";
				 					}
				 				}
				 			}
				 			// Add the subject itself
				 			for (Label test : subject.getCore().yield()) {
				 				question += test.value() + " ";
				 			}
				 		}

				 		// Add the verb (in its lemmatized form, since we're asking "did")
				 		if (verb != null) {
				 			// Add attributes (modifiers were already hunted down for the question word, so look for adverbs only)
				 			for (Tree test2 : verb.getAttributes()){
				 				if (test2.label().toString().equals("RB")) {
				 					for (Label test : test2.yield()){
				 						question = question + test.value() + " ";
				 					}
				 				}
				 			}

				 			// Add the lemmatized verb
				 			for (Label test : verb.getCore().yield()) {
				 				for (CoreLabel token : mySentence.get(TokensAnnotation.class)) {
				 					if (token.value().equals(test.value())) {
				 						question += token.lemma() + " ";
				 					}
				 				}
				 			}
				 		}

				 		// Add the indirect object and any attributes (if it exists)
				 		if(indirectObject != null){
				 			// Add any attributes that should precede the indirect object
				 			for(Tree test2 : indirectObject.getAttributes()){
				 				if(adjectiveLabels.contains(test2.label().toString())){
				 					for(Label test : test2.yield()){
				 						question = question + test.value() + " ";
				 					}
				 				}

				 			}

				 			// Add the indirect object itself
				 			for(Label test : indirectObject.getCore().yield()){
				 				question = question + test.value() + " ";
				 			}

				 			// Add any attributes that should succeed the indirect object
				 			for(Tree test2 : indirectObject.getAttributes()){
				 				if(test2.label().toString().equals("PP")){
				 					for(Label test : test2.yield()){
				 						question = question + test.value() + " ";
				 					}
				 				}
				 			}
				 		}

				 		// Add any phrasal attributes of the object (prepositional phrases) (the object must exist for this type of question)
				 		for (Tree test2 : object.getAttributes()) {
				 			if (test2.label().toString().equals("PP")) {
				 				for (Label test : test2.yield()) {
				 					question += test.value() + " ";
				 				}
				 			}
				 		}

				 		question = question.trim() + "?";
							
				 		//System.out.println(question);
				 		//System.out.print("Answer is ");

				 		// Add the question into the database
//				 	    String prompt = globalPrompt;

				         String corrChoice = "";
				         for(Label test : object.getCore().yield()){
				 			corrChoice = corrChoice + test.value();
				 		}
				 		corrChoice = corrChoice.trim();
			            
			            /*====  Question Structure Added (( 4 [WHO/WHAT] ))  ====*/
					    
					    q = new QuestionStructure(question, corrChoice, distractors);
			    		dq.addQuestions(q);
			    		
			    		/*============================================*/

				         // TODO: do not add every single distractor?
//				 		while(index < distractors.size()){
//				         	index++;
//				         }

				 	    //questions.add(part);
					}
				}
			}
		}
		return dq;
	}

	
	/**
	 * generateComplexAndQuestions()
	 * - Generates questions like "Who did x and y"
	 * - Chooses three actors who did not do both x and y as distractors.
	 * - Probability increases if other actors did only x or only y.
	 * 
	 * @param n, the number of questions of this type to generate.
	 * @return
	 */
	public static DocumentQuestions generateComplexAndQuestions(int n, DocumentQuestions dq) {
		// Cannot generate questions if the matrix is smaller than 2 actors
		if (actorMatrix == null || actorMatrix.size() < 2) {
			return dq;
		}

		questionLoop: for (int i = 0; i < n; i++) {
			// System.out.println("---Loop " + i);
			// Find the most commonly done fact
			// If this fact is as common as the most common fact, flip a coin to determine whether to replace it
			Set<Entry<Fact, Short>> frequencies = factFrequencyMatrix.entrySet();
			short max = 0;
			Fact maxFact = null;

			// Find the most common fact, or one of the most common facts
			for (Entry<Fact, Short> e : frequencies) {
				if (e.getValue() > max) {
					// We need a fact that has been done by at least two people in order to choose smartly
					// If we have already have a repeated fact, 60% chance to accept a more common fact (for variety!)
					if (max < 2 || (max >= 2 && Math.random() <= 0.60)) {
						max = e.getValue();
						maxFact = e.getKey();
					}
				} else if (e.getValue() == max) {
					// TODO: tune the probability of the coin flip
					if (Math.random() <= (double)(1.0 / frequencies.size())) {
						maxFact = e.getKey();
					}
				}
			}

			// If we have a repeated fact, try to choose smartly
			if (max >= 2) {
				//System.out.println("Chose " + maxFact.stringWithoutSubject() + " as the repeated fact.");

				// Collect all the Actors who have done the most common fact
				List<Actor> didTheAction = new ArrayList<Actor>();

				// First collect all the actors who HAVE done this fact
				for (Actor firstActor : actorMatrix) {
					//System.out.println(firstActor.getName() + " has done the frequent fact");

					for (Fact f : firstActor.getFactMatrix()) {
						if (f.predicateEquals(maxFact)) {
							didTheAction.add(firstActor);
						}
					}

						// Jane went to the store. Sam went to the store. Kelly went to the store. Alice went to the store yesterday. Alice bought a pineapple. Sam bought a pear.
				}

				// Try to find someone who has done this fact and at least one other fact
				// Try to choose the other fact such that three distractors have not done it, but may have done the first fact
				// Try 10 times before giving up
				boolean success = false;
				Actor primaryActor = null;
				List<Actor> distrActors = new ArrayList<Actor>();
				Fact otherFact = null;
				Random rng = new Random();

				locateSmartChoices: for (int j = 0; j < 10; j++) {
					if (didTheAction.size() == 0) {
						continue;
					}
					primaryActor = didTheAction.get(rng.nextInt(didTheAction.size()));

					// System.out.println("Checking " + primaryActor.getName() + " as possible candidate");

					// We want our primary actor to have done at least two things
					// (But an actor who has not done at least two things can be a good distractor)
					// All of the actors we are checking have done the frequent fact
					if (primaryActor.getFactMatrix().size() < 2) {
						// System.out.println(primaryActor.getName() + " only did one thing--adding to distractors");
						distrActors.add(primaryActor);
						didTheAction.remove(primaryActor);
						continue locateSmartChoices;
					}

					// 

					// Look for a Fact done by the primary Actor but not by any of the other actors who did the first Fact
					findOtherFact: for (Fact f : primaryActor.getFactMatrix()) {
						if (!f.predicateEquals(maxFact)) {
							List<Actor> tempDistractors = new ArrayList<Actor>();
							// System.out.println("trying " + f.stringWithoutSubject() + " from " + primaryActor.getName() + "'s fact matrix");

							for (Actor a : didTheAction) {
								// System.out.println("Checking " + a.getName() + ": " + a.did(f));
								if (!a.did(f) && !tempDistractors.contains(a) && !distrActors.contains(a)) {
									// System.out.println(a.getName() + " did the first but not the second");
									tempDistractors.add(a);
								}
							}

							// We can change these bounds to fit however many distractors we want--at this point, it means two. If we found some
							// smart distractors, but not enough for a whole question of smart choices
							if (tempDistractors.size() + distrActors.size() > 1 && tempDistractors.size() + distrActors.size() < 3) {
								for (Actor a : actorMatrix) {
									if (tempDistractors.contains(a) || distrActors.contains(a)) {
										continue;
									}

									// Look for actors who did the second action but not the first
									if (a.did(f) && !didTheAction.contains(a)) {
										tempDistractors.add(a);
									}
								}	

								for (Actor a : actorMatrix) {
									if (tempDistractors.contains(a) || distrActors.contains(a)) {
										continue;
									}
									
									// Look for actors who did neither action
									if (!a.did(f) && !didTheAction.contains(a)) {
										tempDistractors.add(a);
									}
								}
							}

							// If we succeed in finding three actors who have not done this action, we're done!
							if (tempDistractors.size() + distrActors.size() >= 3) {
								// System.out.println("found enough distractors");
								otherFact = f;
								success = true;
								distrActors.addAll(tempDistractors);
								break findOtherFact;
							} // else {
							// 	System.out.println("not enough distractors");
							// }
						}
					}
				}

				// System.out.println("Primary actor is " + primaryActor.getName());
				distrActors.remove(primaryActor);

				if (success) {
					// System.out.println("---Making a smart question!");
					// Generate a smarter 'AND' question and add it, then return
					Collections.shuffle(distrActors);

					String question = "";
					String questionReverse = "";
					if (primaryActor.getProperNounType().equals("PERSON")) {
						question = "Who ";
						questionReverse = "Who ";
					} else if (primaryActor.getProperNounType().equals("LOCATION")) {
						question = "Where ";
						questionReverse = "Where ";
					} else {
						question = "What ";
						questionReverse = "What ";
					}

					question += (maxFact.stringWithoutSubject().trim() + " and " + otherFact.stringWithoutSubject().trim() + "?");
					questionReverse += (otherFact.stringWithoutSubject().trim() + " and " + maxFact.stringWithoutSubject().trim() + "?");

					// check if any other and question already used these two facts
//					for (QuestionPart ques : questions) {
//						if (ques.prompt.content.equals(question) || ques.prompt.content.equals
//								(questionReverse)) {
//							continue questionLoop;
//						}
//					}

				    // Add the correct choice (the Actor who DID both things)
					
					String corrChoice = primaryActor.getName();
					
					ArrayList<String> distractors = new ArrayList<String>();

			        //Add distractors (things that the actor DID do)
			        LinkedList<Integer> chosen = new LinkedList<Integer>();
				    for (int j = 0; j < 3; j++) {
				        int idx = new Random().nextInt(distrActors.size());
				        
				        if(chosen.contains((Integer)idx))
				        	j--;
				        
				        else {
				        	
				        	chosen.add((Integer)idx);
				        	distractors.add(distrActors.get(idx).getName());
				        }
				        
					}

				    QuestionStructure q;
				    
				    /*====  Question Structure Added (( 5 [Simple 1] ))  ====*/
				    
				    q = new QuestionStructure(question, corrChoice, distractors);
		    		dq.addQuestions(q);
		    		
		    		/*============================================*/
				    
					//System.out.println("---Made a smart question: " + question);
					// System.out.println("Primary Actor: " + primaryActor.getName());
//			        System.out.println(questions.size());

					continue questionLoop;
				}
			}

			// System.out.println("---Trying to make a random question");

			// TODO: If we reach this point, we have failed to choose a question smartly. Choose randomly.
			Random rng = new Random();
			Actor primaryActor = null;
			List<Actor> distrActors = new ArrayList<Actor>();
			identifyRandomly: for (int k = 0; k < 10; k++) {
				primaryActor = actorMatrix.get(rng.nextInt(actorMatrix.size()));
				distrActors = new ArrayList<Actor>();
				List<Fact> fm = new ArrayList<Fact>();
				fm.addAll(primaryActor.getFactMatrix());

				// We must have an Actor who has done two or more things
				if (fm.size() < 2) {
					continue identifyRandomly;
				}

				// Pick two random facts from this actor's matrix
				Fact f1 = fm.get(rng.nextInt(primaryActor.getFactMatrix().size()));
				Fact f2;
				
				do {
					f2 = fm.get(rng.nextInt(primaryActor.getFactMatrix().size()));
				} while (f1 == f2);

				// Find three Actors who have not done both of these things
				for (Actor a : actorMatrix) {
					if (!a.did(f1) && !a.did(f2)) {
						distrActors.add(a);
					}
				}

				if (distrActors.size() < 3) {
					continue identifyRandomly;
				}

				// Generate a smarter 'AND' question and add it, then return
				Collections.shuffle(distrActors);

				String question = "";
				String questionReverse = "";
				if (primaryActor.getProperNounType().equals("PERSON")) {
					question = "Who ";
					questionReverse = "Who ";
				} else if (primaryActor.getProperNounType().equals("LOCATION")) {
					question = "Where ";
					questionReverse = "Where ";
				} else {
					question = "What ";
					questionReverse = "What ";
				}

				question += (f1.stringWithoutSubject().trim() + " and " + f2.stringWithoutSubject().trim() + "?");
				questionReverse += (f2.stringWithoutSubject().trim() + " and " + f1.stringWithoutSubject().trim() + "?");

				// check if any other and question already used these two facts
//				for (QuestionPart ques : questions) {
//					if (ques.prompt.content.equals(question) || ques.prompt.content.equals(questionReverse)) {
//						continue questionLoop;
//					}
//				}


				// Add the correct choice (the Actor who DID both things)
				
				String corrChoice = primaryActor.getName();
				
				ArrayList<String> distractors = new ArrayList<String>();

			    //Add distractors (things that the actor DID do)
			    LinkedList<Integer> chosen = new LinkedList<Integer>();
				for (int j = 0; j < 3; j++) {
				    int idx = new Random().nextInt(distrActors.size());
				    if(chosen.contains((Integer)idx))
				       	j--;
				    else{
				       	chosen.add((Integer)idx);
				       	distractors.add(distrActors.get(idx).getName());
				    }
				       
				}
				
				QuestionStructure q;
				
				/*====  Question Structure Added (( 6 [Simple 2] ))  ====*/
			    
			    q = new QuestionStructure(question, corrChoice, distractors);
	    		dq.addQuestions(q);
	    		
	    		/*============================================*/

				// System.out.println("---Made a random question: " + question);
				// System.out.println("Primary Actor: " + primaryActor.getName());
//		        System.out.println(questions.size());

			}
		}
		
		return dq;
	}


	/**
	 * generateQuestions()
	 * - Main method: parse the entire passage and generate questions.
	 * 
	 * @param exerciseId - the ID of the exercise we want these questoons to go into
	 * @param passage - the plaintext of the passage
	 * @param numQuestions - the number of questions we ultimately want added to the exercise
	 * @return dq
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")	
	public static DocumentQuestions generateQuestions(StanfordCoreNLP pipeline, Integer gradeLevel, String passage, UserSimple user,  DocumentQuestions dq) throws Exception {
		// TODO: this is mostly still Stanford's code

		/***** Variables for adding questions to database *****/
//		globalModId = exerciseId;
//		globalMod = Exercise.byId(exerciseId);
		globalPrompt = passage;
//		questions = new ArrayList<QuestionPart>();
//		Course course = Course.getCourseForExercise(globalModId);

		
		globalUser = user;

		//System.out.println("passage = " + passage);
		//System.out.println("user = " + globalUser);
		//System.out.println("user = " + globalUser.id);

		int index = 0;

		// a comprehension question QuestionPart Group has a passage as Attachment and counts as a nontrivial group (not a wrapper for one QuestionPart)
//		InputRichText contentText = InputRichText.create(new InputRichText(passage));
		// TODO: is global?
//		qg = QuestionGroup.create(new QuestionGroup("Answer the following questions.", contentText, questionFormat, globalUser, project, true, true));
		
		/***** Fact matrix variables *****/
		actorMatrix = new ArrayList<Actor>();
		factFrequencyMatrix = new HashMap<Fact, Short>();
			

		// create an annotation
		Annotation annotation = new Annotation(passage);
		//System.out.println("THIS PASSAGE IS " + passage);

		pipeline.annotate(annotation);
		 List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		 

		 try {
			 SieveCoreferenceSystem corefSystem;
			 corefSystem = new SieveCoreferenceSystem(new Properties());
				MentionExtractor  mentionExtractor = new MentionExtractor(corefSystem.dictionaries(),
				          corefSystem.semantics());
		      List<Tree> trees = new ArrayList<Tree>();
		      List<List<CoreLabel>> sentences2 = new ArrayList<List<CoreLabel>>();

		      // extract trees and sentence words
		      // we are only supporting the new annotation standard for this Annotator!
		      if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
		        // int sentNum = 0;
		        for (CoreMap sentence : annotation
		            .get(CoreAnnotations.SentencesAnnotation.class)) {
		          List<CoreLabel> tokens = sentence
		              .get(CoreAnnotations.TokensAnnotation.class);
		          sentences2.add(tokens);
		          Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		          trees.add(tree);
		          MentionExtractor.mergeLabels(tree, tokens);
		          MentionExtractor.initializeUtterance(tokens);
		        }
		      } else {
		        //System.err.println("ERROR: this coreference resolution system requires SentencesAnnotation!");
		      }

		      // extract all possible mentions
		      // this is created for each new annotation because it is not threadsafe
		      RuleBasedCorefMentionFinder finder = new RuleBasedCorefMentionFinder();
		      List<List<Mention>> allUnprocessedMentions = finder
		          .extractPredictedMentions(annotation, 0, corefSystem.dictionaries());

		      // add the relevant info to mentions and order them for coref
		      edu.stanford.nlp.dcoref.Document document = mentionExtractor.arrange(
		          annotation,
		          sentences2,
		          trees,
		          allUnprocessedMentions);
		      List<List<Mention>> orderedMentions = document.getOrderedMentions();
		      //if (VERBOSE) {
//		        for (int i = 0; i < orderedMentions.size(); i++) {
//		          //System.err.printf("Mentions in sentence #%d:\n", i);
//		          for (int j = 0; j < orderedMentions.get(i).size(); j++) {
//		            System.err.println("\tMention #"
//		                + j
//		                + ": "
//		                + orderedMentions.get(i).get(j).spanToString());
//		          }
//		        }
//		      }

		      Map<Integer, CorefChain> result = corefSystem.coref(document);
		      annotation.set(CorefCoreAnnotations.CorefChainAnnotation.class, result);
		      List<Pair<IntTuple, IntTuple>> links = SieveCoreferenceSystem
		              .getLinks(result);
		      // for backward compatibility
		        //
		        // save the coref output as CorefGraphAnnotation
		        //

		        // cdm 2013: this block didn't seem to be doing anything needed....
		        // List<List<CoreLabel>> sents = new ArrayList<List<CoreLabel>>();
		        // for (CoreMap sentence:
		        // annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
		        // List<CoreLabel> tokens =
		        // sentence.get(CoreAnnotations.TokensAnnotation.class);
		        // sents.add(tokens);
		        // }

		        // this graph is stored in CorefGraphAnnotation -- the raw links found
		        // by the coref system
		      List<Pair<IntTuple, IntTuple>> graph = new ArrayList<Pair<IntTuple, IntTuple>>();

		        for (Pair<IntTuple, IntTuple> link : links) {
		          //
		          // Note: all offsets in the graph start at 1 (not at 0!)
		          // we do this for consistency reasons, as indices for syntactic
		          // dependencies start at 1
		          //
		          int srcSent = link.first.get(0);
		          int srcTok = orderedMentions.get(srcSent - 1).get(
		              link.first.get(1) - 1).headIndex + 1;
		          int dstSent = link.second.get(0);
		          int dstTok = orderedMentions.get(dstSent - 1).get(
		              link.second.get(1) - 1).headIndex + 1;
		          IntTuple dst = new IntTuple(2);
		          dst.set(0, dstSent);
		          dst.set(1, dstTok);
		          IntTuple src = new IntTuple(2);
		          src.set(0, srcSent);
		          src.set(1, srcTok);
		          graph.add(new Pair<IntTuple, IntTuple>(src, dst));
		        }
		        annotation.set(CorefCoreAnnotations.CorefGraphAnnotation.class, graph);

		        for (CorefChain corefChain : result.values()) {
		          if (corefChain.getMentionsInTextualOrder().size() < 2)
		            continue;
		          CorefMention chain = corefChain.getRepresentativeMention();
		          Set<CoreLabel> coreferentTokens = Generics.newHashSet();
		          for (CorefMention mention : corefChain.getMentionsInTextualOrder()) {
		            CoreMap sentence = annotation.get(
		                CoreAnnotations.SentencesAnnotation.class).get(
		                mention.sentNum - 1);
		            CoreLabel token = sentence.get(
		                CoreAnnotations.TokensAnnotation.class).get(
		                mention.headIndex - 1);
		            //System.out.println(token.lemma() + " IS THE MENTION FOR " + chain.mentionSpan );
		            
		            CoreMap test2 = sentences.get(mention.sentNum - 1);
		            Tree test = test2.get(TreeCoreAnnotations.TreeAnnotation.class);
		            for(Label label : test.getLeaves()){
		            	if(label.value().trim().equalsIgnoreCase(token.lemma().trim()) && !label.value().contains(token.lemma().trim())){
		            		//System.out.println("MENTION MATCHES");
		            		//label.setFromString(token.lemma())
		            		label.setValue(chain.mentionSpan);
		            		//System.out.println("NEW VALUE SHOULD BE " + chain.mentionSpan + "BUT IS " + label.value());
		            		//System.out.println(token.lemma() + " IS THE MENTION FOR " + chain.mentionSpan );
		            	}
		            	else{
		            		//System.out.println("MENTION DOESNT MATCH " + token.lemma() + "BECAUSE ITS " + label.value());
		            	}
		            }
		            //System.out.println("NEW TREE IS");
		            //test.pennPrint();
		          }
		          for (CoreLabel token : coreferentTokens) {
			        	token.set(CorefCoreAnnotations.CorefClusterAnnotation.class, coreferentTokens);
			      }
		          
		        }
		        
		       

		    } catch (RuntimeException e) {
		      throw e;
		    } catch (Exception e) {
		      throw new RuntimeException(e);
		    }


		//System.out.println(annotation);
		//System.out.println(annotation.get(CoreAnnotations.SentencesAnnotation.class));
				
		//List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		// An Annotation is a Map with Class keys for the linguistic analysis types.
		// You can get and use the various analyses individually.
		// For instance, this gets the parse tree of the first sentence in the content.
		//List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
		
		// Go through the entire passage and extract facts from every sentence
		if (sentences != null && !sentences.isEmpty()) {
			// Open the proper noun classifier
			try {
				classifier = CRFClassifier.getDefaultClassifier();
			} catch (ClassCastException e) {
				//e.printStackTrace();
			}

			whatDistractors = new ArrayList<String>();

			for (int i = 0; i < sentences.size(); i++) {
				// System.out.println("TESTING SENTENCE " + i);
				CoreMap sentence = sentences.get(i);

				// Update the fact matrix with a new fact we extract from the passage
				insertFact(extractOneFact(sentence));
			}
		}

		// Print out final fact matrix
		//System.out.println("\n\n\nFINAL FACT MATRIX: " + actorMatrix.size());
//		int i = 0;
//
//		for (Actor a : actorMatrix) {
//			System.out.println("Actor " + i);
//			i++;
//
//			//a.print();
//		}

//		for (Entry<Fact, Short> e : factFrequencyMatrix.entrySet()) {
//			System.out.println(e.getKey().stringWithoutSubject() + " was done " + e.getValue() + " times");
//		}

		dq = generateComplexChooseQuestions(1, dq);
		dq = generateComplexAndQuestions(1, dq);
		for (int j = 0; j < sentences.size(); j++) {
			dq = generateSimpleQuestions(sentences.get(j), dq);

		}

		
		return dq;
	}
	
	
}