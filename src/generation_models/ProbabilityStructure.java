package generation_models;

public class ProbabilityStructure {
	
	String question;
	public int count;
	public Double added;
	public Double multi;
	
	public ProbabilityStructure(String question, int count, Double addedProb, Double multiProb) {
		this.question = question;
		this.count = count;
		this.added = addedProb;
		this.multi = multiProb;
	}

}
