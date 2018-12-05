package alexbogatu.github.editFST;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

public class App 
{
public static void main(String[] args) {
		
		String str1 = "This is a sentence. It is made of words";
		String str2 = "This sentence is similar. It has almost the same words";
		
		StringMetric levenshtein = StringMetrics.levenshtein();
		EditDistance edit = new EditDistance();
		
    	
		//We are comparing distances
		float levenshteinResult = 1.0f - levenshtein.compare(str1, str2);
		float editResult = edit.compare(str1, str2);
		
		System.out.println(Float.toString(levenshteinResult) + " | " + Float.toString(editResult));

	}
}
