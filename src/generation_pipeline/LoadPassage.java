package generation_pipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

/** 
 * ImportDocument loads in the passage according to the parameters.
 *   
 * @author TerranceTaubes
 */
public class LoadPassage {
	
	public static Scanner scan = new Scanner(System.in);
	
	public LoadPassage() {}
	
	/** 
	 * Read()
	 * - Reads parameters to discern whether user input is either the passage itself or the name of the passage,
     *   and loads in the passage path and text into the String[] path_text.
     *   
	 * @param path
	 * @param uploadMethod
	 * @param passageInput
	 * @return path_text
	 * @throws Exception
	 */
	public static String[] Read(String path, String uploadMethod, String passageInput) throws Exception {
		
		String[] path_text = new String[2];
		
		if (uploadMethod == null) {
			
			//System.out.println("Path: "+path);
			System.out.println("Enter file name:");
		    String file = scan.next();
			path += file+".txt";
			
			
			BufferedReader reader = new BufferedReader(new FileReader(path));
			
			String text = "";
			String readString = null;
			
			while ((readString = reader.readLine()) != null) {
				text += readString+" ";
			}		
			
			reader.close();
						
			path_text[0] = path;
			path_text[1] = text;
			
		}
				
		else if (uploadMethod.equals("file")) {
			
			path += passageInput+".txt";
			
			
			BufferedReader reader = new BufferedReader(new FileReader(path));
			
			String text = "";
			String readString = null;
			
			while ((readString = reader.readLine()) != null) {
				text += readString+" ";
			}
			
			reader.close();
			
			path_text[0] = path;
			path_text[1] = text;
			
		}
		
		else if (uploadMethod.equals("manual")) {
			
			path_text[0] = path;
			path_text[1] = passageInput;
			
		} 
		
		
		System.out.println(path_text[1]);
		
		return path_text;
		
	}

}
