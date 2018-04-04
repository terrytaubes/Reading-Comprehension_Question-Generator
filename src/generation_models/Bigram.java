package generation_models;

public class Bigram {
	
	public String[] word = new String[2];
	public String[] pos = new String[2];
	
	public Bigram(String first, String second) {
		word[0] = first;
		word[1] = second;
		pos[0] = null;
		pos[1] = null;
	}
	
	public Bigram(String first, String pos1, String second, String pos2) {
		word[0] = first;
		word[1] = second;
		pos[0]  = pos1;
		pos[1] = pos2;
	}
	
	public void print() {
		System.out.println("("+word[0]+", "+word[1]+")");
	}

	public String asString() {
		String val = "("+word[0]+", "+word[1]+")";
		
		return val;
	}
	
	public String asStringPOS() {
		String val = "("+word[0]+"-"+pos[0]+", "+word[1]+"-"+pos[1]+")";
		
		return val;
	}
	
}
