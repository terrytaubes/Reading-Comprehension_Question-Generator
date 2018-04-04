package generation_models;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A Component is a fundamental part of a sentence: its subject, verb, or object.
 * We retain the identity of the component (its content and label in the core Tree),
 * all the attributes which modify it (adjectives, adverbs, prepositional phrases, etc.),
 * and if it is a noun, what role of noun it is so we can choose the correct question
 * word.
 * @author Elsbeth
 *
 */
public class Component {
	// null for a Component that is not a noun
	public String properNounType;
	// all the modifiers of this Component, saved in parse trees
	// can be adjectives, adverbs, or prepositional phrases. an adjective modifier under a verb is an adjective complement.
	public List<Tree> attributes;
	// a single node (or maybe a few nodes if we have a multi-word noun, like a person's name) containing the "core" component (the subject, object, or verb) and its label
	public Tree core;
	
	// Constructor: default
	public Component() {
		this.properNounType = null;
		this.attributes = new ArrayList<Tree>();
		this.core = null;
	}
	
	// Constructor: no noun type
	public Component(List<Tree> a, Tree c) {
		this.properNounType = null;
		this.attributes = a;
		this.core = c;
	}
	
	// Constructor: all fields
	public Component(String n, List<Tree> a, Tree c) {
		this.properNounType = n;
		this.attributes = a;
		this.core = c;
	}
	
	// Getter for proper noun type
	public String getProperNounType() {
		return this.properNounType;
	}
	
	// Setter for proper noun type
	public void setProperNounType(String properNounType) {
		this.properNounType = properNounType;
	}
	
	// Getter for attributes
	public List<Tree> getAttributes() {
		return this.attributes;
	}
	
	// Setter for attributes
	public void setAttributes(List<Tree> attributes) {
		this.attributes = attributes;
	}
	
	// Getter for core
	public Tree getCore() {
		return this.core;
	}
	
	// Setter for core
	public void setCore(Tree core) {
		this.core = core;
	}

	// Printer method
	public void print() {
		System.out.println("---COMPONENT:");
		System.out.println("Proper Noun Type: " + this.properNounType);
		System.out.println("Attributes:");

		for (Tree t : this.attributes) {
			t.pennPrint();
		}

		System.out.println("Core:");

		this.core.pennPrint();

		System.out.println();
	}
}