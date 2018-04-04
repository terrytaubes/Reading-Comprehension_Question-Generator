package generation_models;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;

/**
 * The Fact class embodies one elementary fact about a certain actor, consisting of a subject, verb, and maybe direct object/indirect object.
 * We also retain the original sentence and its position in the passage for question generation.
 * @author Elsbeth
 *
 */
public class Fact {
	public String sentence;
	public int orderIndex;
	public boolean negative;
	public Component subject;
	public Component verb;
	public Component object;
	public Component indirectObject;

	// Constructor: default
	public Fact() {
		this.sentence = "";
		this.orderIndex = 0;
		// Do not instantiate trees--we will probably send it trees in the constructor
		this.subject = this.verb = this.object = null;
	}

	// Constructor: make a new Fact, given all its information
	public Fact(String s, int i, Component b, Component v, Component o, Component io, boolean n) {
		this.sentence = s;
		this.orderIndex = i;
		this.subject = b;
		this.verb = v;
		this.object = o;
		this.indirectObject = io;
		this.negative = n;
	}

	// Getter for sentence
	public String getSentence() {
		return this.sentence;
	}

	// Setter for sentence
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	// Getter for order index
	public int getOrderIndex() {
		return this.orderIndex;
	}

	// Setter for order index
	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	// Getter for negativity
	public boolean isNegative() {
		return this.negative;
	}

	// Setter for negativity
	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	// Getter for subject
	public Component getSubject() {
		return this.subject;
	}

	// Setter for subject
	public void setSubject(Component subject) {
		this.subject = subject;
	}

	// Getter for verb
	public Component getVerb() {
		return this.verb;
	}

	// Setter for verb
	public void setVerb(Component verb) {
		this.verb = verb;
	}

	// Getter for object
	public Component getObject() {
		return this.object;
	}

	// Setter for object
	public void setObject(Component object) {
		this.object = object;
	}

	// Getter for indirect object
	public Component getIndirectObject() {
		return this.indirectObject;
	}

	// Setter for object
	public void setIndirectObject(Component indirectObject) {
		this.indirectObject = indirectObject;
	}

	// Stringify method
	public void print() {
		System.out.println("--FACT:");
		System.out.println("Sentence: " + this.sentence);
		System.out.println("Order Index: " + this.orderIndex);
		System.out.println("Subject...");
		this.subject.print();
		System.out.println("Verb..");
		this.verb.print();

		if (this.object != null) {
			System.out.println("Object...");
			this.object.print();
		} else {
			System.out.println("No object...");
		}

		if (this.indirectObject != null) {
			System.out.println("Indirect Object...");
			this.indirectObject.print();
		} else {
			System.out.println("No indirect object...");
		}
	}

	// Comparison method between Facts. Checks if the subject, verb, and object(s) are all the same.
	// TODO: Also check attributes? Just use the Trees' comparison methods for all?
	public boolean equals(Fact otherFact) {
		if (otherFact == null) {
			return false;
		}

		String candidateCore = "", secondCore = "";

		for (Label l : this.getSubject().getCore().yield()) {
			candidateCore += l.value() + " ";
		}

		for (Label l : otherFact.getSubject().getCore().yield()) {
			secondCore += l.value() + " ";
		}

		if (!candidateCore.equals(secondCore)) {
			return false;
		}

		candidateCore = "";
		secondCore = "";

		for (Label l : this.getVerb().getCore().yield()) {
			candidateCore += l.value() + " ";
		}

		for (Label l : otherFact.getVerb().getCore().yield()) {
			secondCore += l.value() + " ";
		}

		if (!candidateCore.equals(secondCore)) {
			return false;
		}

		if (object != null && otherFact.getObject() != null) {
			candidateCore = "";
			secondCore = "";

			for (Label l : this.getObject().getCore().yield()) {
				candidateCore += l.value() + " ";
			}

			for (Label l : otherFact.getObject().getCore().yield()) {
				secondCore += l.value() + " ";
			}

			if (!candidateCore.equals(secondCore)) {
				return false;
			}
		}

		if (indirectObject != null && otherFact.getIndirectObject() != null) {
			candidateCore = "";
			secondCore = "";

			for (Label l : this.getIndirectObject().getCore().yield()) {
				candidateCore += l.value() + " ";
			}

			for (Label l : otherFact.getIndirectObject().getCore().yield()) {
				secondCore += l.value() + " ";
			}

			if (!candidateCore.equals(secondCore)) {
				return false;
			}
		}

		return true;
	}

	public boolean predicateEquals(Fact otherFact) {
		if (otherFact == null) {
			return false;
		}

		String candidateCore = "", secondCore = "";

		for (Label l : this.getVerb().getCore().yield()) {
			candidateCore += l.value() + " ";
		}

		for (Label l : otherFact.getVerb().getCore().yield()) {
			secondCore += l.value() + " ";
		}

		if (!candidateCore.equals(secondCore)) {
			return false;
		}

		if (object != null && otherFact.getObject() != null) {
			candidateCore = "";
			secondCore = "";

			for (Label l : this.getObject().getCore().yield()) {
				candidateCore += l.value() + " ";
			}

			for (Label l : otherFact.getObject().getCore().yield()) {
				secondCore += l.value() + " ";
			}

			if (!candidateCore.equals(secondCore)) {
				return false;
			}
		}

		if (indirectObject != null && otherFact.getIndirectObject() != null) {
			candidateCore = "";
			secondCore = "";

			for (Label l : this.getIndirectObject().getCore().yield()) {
				candidateCore += l.value() + " ";
			}

			for (Label l : otherFact.getIndirectObject().getCore().yield()) {
				secondCore += l.value() + " ";
			}

			if (!candidateCore.equals(secondCore)) {
				return false;
			}
		}

		return true;
	}

	// Return the verb, object, and indirect object parts in a logical order, but not the subject
	public String stringWithoutSubject() {
		String s = "";

		for(Tree t : verb.attributes){
			for(Label w : t.yield()){
				s += w.value() + " ";
			}
		}

		for (Label l : verb.getCore().yield()) {
			s += l.value() + " ";
			
		}

		if (indirectObject != null) {
			for(Tree t : indirectObject.attributes){
				if(!t.label().toString().equals("PP")){
					for(Label w : t.yield()){
						s += w.value() + " ";
					}
				}
			}

			for (Label l : indirectObject.getCore().yield()) {
				s += l.value() + " ";
			}

			for(Tree t : indirectObject.attributes){
				if(t.label().toString().equals("PP")){
					for(Label w : t.yield()){
						s += w.value() + " ";
					}
				}
			}
		}

		if (object != null) {
			for(Tree t : object.attributes){
				if (!t.label().toString().equals("PP")) {
					for(Label l : t.yield()){
						s += l.value() + " ";
					}
				}
			}

			for (Label l : object.getCore().yield()) {
				s += l.value() + " ";
			}

			for(Tree t : object.attributes){
				if (t.label().toString().equals("PP")) {
					for(Label l : t.yield()){
						s += l.value() + " ";
					}
				}
			}
		}
		
		return s;
	}
}