package generation_models;

import edu.stanford.nlp.ling.Label;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * The Actor class contains all information on one single person or thing which performs actions in a passage.
 * It keeps track of the subject as well as their proper noun type (person, place, organization) so as to
 * make question generation easier (is this actor a "who" or a "what"?). It also keeps track of all the
 * actions or facts associated with that subject in a "fact matrix".
 * nb: Really the costruct more appropriately called the "fact matrix" as we have conceived it is the
 * actor matrix in PassageParser.java. This is just a hashmap that holds facts, whereas the actor matrix
 * holds all the facts in the entire passage through their Actors.
 * @author Elsbeth
 *
 */
public class Actor {
	public String name;
	public List<String> aliases;
	public HashSet<Fact> factMatrix;
	// should not be null, since the subject should be a noun, but null would indicate not a noun
	public String properNounType;

	// Constructor: default
	public Actor() {
		this.name = "";
		this.aliases = new ArrayList<String>();
		this.factMatrix = new HashSet<Fact>();
		this.properNounType = null;
	}

	// Constructor: make a new actor with an empty fact matrix that can be filled later
	public Actor(String n, String p) {
		this.name = n;
		this.aliases = new ArrayList<String>();
		this.factMatrix = new HashSet<Fact>();
		this.properNounType = p;
	}

	// Constructor: make a new actor, having already created a full fact matrix and aliases list for them
	public Actor(String n, List<String> a, HashSet<Fact> m, String p) {
		this.name = n;
		this.aliases = a;
		this.factMatrix = m;
		this.properNounType = p;
	}

	// Getter for name
	public String getName() {
		return this.name;
	}

	// Setter for name
	public void setName(String name) {
		this.name = name;
	}

	// Getter for fact matrix
	public HashSet<Fact> getFactMatrix() {
		return this.factMatrix;
	}

	// Setter for fact matrix
	public void setFactMatrix(HashSet<Fact> factMatrix) {
		this.factMatrix = factMatrix;
	}

	// Getter for aliases
	public List<String> getAliases() {
		return this.aliases;
	}

	// Setter for aliases
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	// Getter for name
	public String getProperNounType() {
		return this.properNounType;
	}

	// Setter for name
	public void setProperNounType(String properNounType) {
		this.properNounType = properNounType;
	}

	// Return whether or not this Actor performed a similar fact to the input
	public boolean did(Fact fact) {
		for (Fact f : this.factMatrix) {
			if (fact.predicateEquals(f)) {
				return true;
			}
		}

		return false;
	}

	// printer method - prints the entire Actor
	public void print() {
		System.out.println(this.name + ", a " + this.properNounType);

		Iterator<Fact> factIterator = this.factMatrix.iterator();

		while (factIterator.hasNext()) {
			System.out.print("--");
			Fact f = factIterator.next();

			for (Label l : f.getVerb().getCore().yield()) {
				System.out.print(l.value() + " ");
			}

			if (f.getIndirectObject() != null) {
				for (Label l : f.getIndirectObject().getCore().yield()) {
					System.out.print(l.value() + " ");
				}
			}

			if (f.getObject() != null) {
				for (Label l : f.getObject().getCore().yield()) {
					System.out.print(l.value() + " ");
				}
			}

			System.out.println();
		}
	}
}